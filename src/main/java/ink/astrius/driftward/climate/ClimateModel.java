package ink.astrius.driftward.climate;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMaps;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

import java.util.function.IntBinaryOperator;

// Ambient temperature from the world generator's own climate noise at a position:
// the annual mean from noise temperature, the seasonal swing from continentality (small in warm
// climates), cooling with altitude, a cave climate below the surface, season and time of day,
// weather, shelter, wind on exposed heights, cold air pooling in valleys, and water.
public final class ClimateModel {
    public static final ResourceLocation DEEPER_DOWN = ResourceLocation.fromNamespaceAndPath("spectrum", "deeper_down");
    private static final TagKey<Biome> IS_OCEAN = biomeTag("is_ocean");
    private static final TagKey<Biome> IS_RIVER = biomeTag("is_river");
    private static final TagKey<Biome> IS_BEACH = biomeTag("is_beach");

    private ClimateModel() {}

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    // Noise climate at a position; `null` where the generator has no climate noise (or on the client)
    public record Noise(double temperature, double humidity, double continentalness) {}

    // What the model needs besides the position and biome.
    // - surface: top of the column (roofs count); anything at or below the position means open sky
    // - season / waterSeason: -1 (coldest) .. 1 (warmest); water lags the air
    // - rain / thunder / cloud: 0..1
    // - shelter: 0 in the open .. 1 with no sky light at all
    // - terrain: ground height (no leaves) at a column, for valley pooling; null skips pooling
    public record Conditions(
        int surface, double season, double waterSeason, long dayTime,
        double rain, double thunder, double cloud, double shelter, boolean inWater, IntBinaryOperator terrain
    ) {
        public Conditions withWeather(double rain, double thunder, double cloud) {
            return new Conditions(surface, season, waterSeason, dayTime, rain, thunder, cloud, shelter, inWater, terrain);
        }
    }

    // Every term of the result, for the climate commands. `celsius` is what Thermoo gets.
    public record Breakdown(
        Noise noise, boolean deeperDown, boolean water, boolean snowy,
        double humidity, double dryness, double relativeHumidity,
        double baseMean, double offset, double lapse, double mean,
        double warmth, double amplitude,
        double depth, double cave,
        double season, double seasonal,
        double day, double diurnalAmplitude, double diurnal,
        double shelter, double weather, double wind, double valley, double pooling,
        boolean inWater, double waterCelsius,
        double airCelsius, double celsius
    ) {}

    public static Noise sample(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        var source = serverLevel.getChunkSource();
        if (!(source.getGenerator() instanceof NoiseBasedChunkGenerator)) {
            return null;
        }
        // The climate noise never changes for a spot and is expensive (Terralith's splines), so it's
        // cached per quart (4×4×4 blocks)
        int qx = QuartPos.fromBlock(pos.getX()), qy = QuartPos.fromBlock(pos.getY()), qz = QuartPos.fromBlock(pos.getZ());
        long key = BlockPos.asLong(qx, qy, qz) ^ ((long) System.identityHashCode(serverLevel) << 1);
        var cached = NOISE.get(key);
        if (cached != null) {
            return cached;
        }
        Climate.TargetPoint point = source.randomState().sampler().sample(qx, qy, qz);
        var noise = new Noise(
            Climate.unquantizeCoord(point.temperature()),
            Climate.unquantizeCoord(point.humidity()),
            Climate.unquantizeCoord(point.continentalness())
        );
        if (NOISE.size() > 1 << 17) {
            NOISE.clear();
        }
        NOISE.put(key, noise);
        return noise;
    }

    private static final java.util.concurrent.ConcurrentHashMap<Long, Noise> NOISE = new java.util.concurrent.ConcurrentHashMap<>();

    // Ground height for pooling and the weather: the loaded heightmap, nothing where the chunk isn't
    // loaded (never loads chunks). Only this terrain's valley depths are cached; maps pass their own.
    public static IntBinaryOperator terrain(Level level) {
        return new LiveTerrain(level);
    }

    private record LiveTerrain(Level level) implements IntBinaryOperator {
        @Override
        public int applyAsInt(int x, int z) {
            // Loaded chunks only: the generator's height is a full noise column (about a millisecond
            // with Terralith), far too slow for anything that runs per tick
            if (level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            }
            return Integer.MIN_VALUE;
        }
    }

    // Live conditions at a loaded position. `outdoor` ignores shelter and water: the air above the
    // spot, which is what snow and ice follow. Cloud comes from the weather layer; rain and thunder
    // too once it drives precipitation, until then from vanilla's global weather.
    public static Conditions live(Level level, BlockPos pos, Holder<Biome> biome, boolean outdoor) {
        var c = ClimateConstants.get();
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        double shelter = 0;
        boolean inWater = false;
        if (!outdoor) {
            shelter = 1 - level.getBrightness(LightLayer.SKY, pos) / 15.0;
            inWater = level.getFluidState(pos).is(FluidTags.WATER);
        }
        double rain = level.getRainLevel(1);
        double thunder = level.getThunderLevel(1);
        var at = new Conditions(
            surface,
            ClimateSeasons.factor(level, c.seasonPeak()),
            ClimateSeasons.factor(level, c.seasonPeak(), c.waterLag()),
            level.getDayTime(),
            rain, thunder, rain, shelter, inWater, terrain(level)
        );
        var weather = weather(level, pos, biome, null, at);
        if (weather == ClimateWeather.CALM) {
            return at;
        }
        return c.weatherDrivesRain()
            ? at.withWeather(weather.precipitation(), weather.thunder(), weather.cloud())
            : at.withWeather(rain, thunder, Math.max(rain, weather.cloud()));
    }

    // The weather at a position under the given conditions (their own weather is ignored)
    // The weather is smooth over kilometres and asked for constantly (Thermoo, Serene Seasons' rain
    // and snow checks, crops), so live queries share one result per 16×16 column for two seconds,
    // computed at the surface
    private static final int WEATHER_TICKS = 40;
    private static final java.util.concurrent.ConcurrentHashMap<Long, ClimateWeather.State> WEATHER =
        new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile long weatherBucket = Long.MIN_VALUE;

    public static ClimateWeather.State weather(Level level, BlockPos pos, Holder<Biome> biome, Noise noise, Conditions at) {
        if (!(level instanceof ServerLevel serverLevel) || !ClimateConstants.get().weatherEnabled()) {
            return ClimateWeather.CALM;
        }
        if (!(at.terrain() instanceof LiveTerrain)) {
            return computeWeather(serverLevel, pos, biome, noise != null ? noise : sample(level, pos), at);
        }
        long bucket = level.getGameTime() / WEATHER_TICKS;
        if (bucket != weatherBucket) {
            WEATHER.clear();
            weatherBucket = bucket;
        }
        long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        var cached = WEATHER.get(key);
        if (cached != null) {
            return cached;
        }
        var surface = new BlockPos(pos.getX(), at.surface(), pos.getZ());
        var weather = computeWeather(serverLevel, surface, biome, noise != null ? noise : sample(level, surface), at);
        if (WEATHER.size() > 1 << 14) {
            WEATHER.clear();
        }
        WEATHER.put(key, weather);
        return weather;
    }

    private static ClimateWeather.State computeWeather(ServerLevel level, BlockPos pos, Holder<Biome> biome, Noise noise, Conditions at) {
        var climate = breakdown(level, pos, biome, noise, at.withWeather(0, 0, 0));
        return ClimateWeather.at(level.getSeed(), pos.getX(), pos.getZ(), pos.getY(), level.getGameTime(),
            climate, biome.value().hasPrecipitation(), at.terrain());
    }

    public static double temperature(Level level, BlockPos pos, Holder<Biome> biome) {
        return breakdown(level, pos, biome).celsius();
    }

    public static Breakdown breakdown(Level level, BlockPos pos, Holder<Biome> biome) {
        return breakdown(level, pos, biome, sample(level, pos), live(level, pos, biome, false));
    }

    public static Breakdown breakdown(Level level, BlockPos pos, Holder<Biome> biome, Noise noise, Conditions at) {
        var c = ClimateConstants.get();
        var correction = biome.getData(BiomeClimate.DATA_MAP);
        var settings = biome.value().getModifiedClimateSettings();
        boolean deeperDown = level.dimension().location().equals(DEEPER_DOWN);
        boolean ocean = biome.is(IS_OCEAN);
        boolean water = ocean || biome.is(IS_RIVER) || biome.is(IS_BEACH);

        // Without noise (client side, flat worlds), approximate the noise axes from the biome itself
        double t = noise != null ? noise.temperature() : Mth.clamp((settings.temperature() - 0.8) * 0.75, -1, 1);
        double h = noise != null ? noise.humidity() : Mth.clamp(settings.downfall() * 2 - 1, -1, 1);
        double continentalness = noise != null ? noise.continentalness() : 0.1;

        // Humidity 0..1 and dryness
        double humidity = c.humidityNoiseWeight() * (h + 1) / 2
            + (1 - c.humidityNoiseWeight()) * Math.min(settings.downfall(), 1);
        humidity = Mth.clamp(humidity * (biome.value().hasPrecipitation() ? 1 : c.dryFactor()), 0, 1);
        if (water) {
            humidity = Math.max(humidity, c.waterHumidity());
        }
        double dryness = deeperDown || water ? 0 : Math.max(0, (c.aridLimit() - humidity) / c.aridLimit());

        // Annual mean and seasonal amplitude at sea level. Warm climates have small seasons whether
        // wet or dry (savanna, desert); the wet tropics have the smallest.
        double baseMean;
        double warmth = 0;
        double amplitude;
        if (deeperDown) {
            baseMean = c.deeperDownBase() + c.deeperDownScale() * t;
            amplitude = 0;
        } else {
            baseMean = correction != null && correction.mean().isPresent()
                ? correction.mean().get()
                : ClimateConstants.interpolate(c.bandNoise(), c.bandCelsius(), t);
            amplitude = ClimateConstants.interpolate(c.amplitudeContinentalness(), c.amplitudeCelsius(), continentalness);
            warmth = Mth.clamp((baseMean - c.warmStart()) / Math.max(1e-6, c.warmFull() - c.warmStart()), 0, 1);
            amplitude *= Mth.lerp(warmth, 1, c.warmDamping());
            if (h >= c.tropicHumidity()) {
                amplitude *= Mth.lerp(warmth, 1, c.tropicDamping());
            }
            amplitude = Math.min(c.maxAmplitude(), amplitude * (1 + c.aridAmplitude() * dryness));
            baseMean += c.aridBonus() * dryness;
        }
        double offset = correction != null ? correction.offset() : 0;
        double mean = baseMean + offset;

        // Altitude
        double lapse = 0;
        if (!deeperDown && (correction == null || correction.lapse())) {
            lapse = c.lapsePerBlock() * Math.max(0, pos.getY() - level.getSeaLevel());
            mean -= lapse;
        }

        // Snowy biomes keep winter below freezing
        boolean snowy = settings.temperature() < c.snowTemperature()
            || settings.temperatureModifier() == Biome.TemperatureModifier.FROZEN;
        if (!deeperDown && snowy) {
            mean = Math.min(mean, amplitude + c.snowMargin());
        }

        // Underground: blend into the cave climate, which has no seasons and warms with depth
        double depth = Math.max(0, at.surface() - pos.getY());
        double cave = 0;
        double exposure = 1;
        if (!deeperDown) {
            cave = Mth.clamp((depth - c.caveDepthStart()) / (c.caveDepthFull() - c.caveDepthStart()), 0, 1);
            if (cave > 0) {
                double caveMean = c.caveBase() + c.geothermalPerBlock() * Math.max(0, depth - c.caveDepthFull()) + offset;
                mean = Mth.lerp(cave, mean, caveMean);
                amplitude *= 1 - cave;
                exposure = 1 - cave;
            }
        }

        double shelter = Mth.clamp(at.shelter(), 0, 1);
        // How much of the outside weather, wind and day/night reaches the position
        double open = deeperDown ? 0 : exposure * (1 - shelter * c.shelterDiurnalDamping());
        double rain = deeperDown ? 0 : Mth.clamp(at.rain(), 0, 1);
        double thunder = deeperDown ? 0 : Mth.clamp(at.thunder(), 0, 1);

        // Season: sheltered air keeps part of it
        double seasonal = amplitude * at.season() * (1 - shelter * c.shelterSeasonDamping());

        // Day and night: dry air makes hot days and cold nights; clouds flatten both
        double day = Math.cos(2 * Math.PI * ((at.dayTime() % 24000) - c.diurnalPeakTick()) / 24000.0);
        double diurnalAmplitude = deeperDown ? 0
            : c.diurnalAmplitude() * (1 + c.diurnalArid() * dryness) * (1 - c.cloudDamping() * Mth.clamp(at.cloud(), 0, 1))
                * (ocean ? c.diurnalOcean() : 1);
        double diurnal = diurnalAmplitude * day * open;

        // Weather
        double precipitation = biome.value().hasPrecipitation() ? 1 : c.dryRainCooling();
        double weather = -(c.rainCooling() * rain * precipitation + c.thunderCooling() * thunder) * open;

        // Wind on exposed heights, stronger in storms
        double height = Mth.clamp((pos.getY() - c.windStart()) / Math.max(1e-6, c.windFull() - c.windStart()), 0, 1);
        double wind = -c.windCooling() * height * (1 + c.windStorm() * (rain + thunder)) * open;

        // Cold air pooling in valleys on clear nights, near the ground
        double valley = 0;
        double pooling = 0;
        double night = Math.max(0, -day);
        if (!deeperDown && night > 0 && rain < 1 && open > 0 && at.terrain() != null) {
            int ground = at.terrain().applyAsInt(pos.getX(), pos.getZ());
            if (ground != Integer.MIN_VALUE && pos.getY() - ground <= c.poolHeight()) {
                valley = valleyDepth(at.terrain(), pos.getX(), pos.getZ(), ground, (int) c.poolRadius()) / c.poolDepth();
                valley = Mth.clamp(valley, 0, 1);
                pooling = -c.poolCooling() * valley * night * (1 - rain) * open;
            }
        }

        double air = mean + seasonal + diurnal + weather + wind + pooling;

        // Water: a smaller, later season and almost no day/night swing; liquid water doesn't go
        // below freezing
        double waterCelsius = mean + amplitude * c.waterSeasonDamping() * at.waterSeason()
            + diurnalAmplitude * day * exposure * c.waterDiurnalDamping();
        waterCelsius = Math.max(waterCelsius, ocean ? c.seawaterMin() : c.freshwaterMin());

        double relativeHumidity = Mth.clamp(0.1 + 0.85 * humidity + 0.3 * rain * precipitation, 0, 1);
        if (cave > 0) {
            relativeHumidity = Mth.lerp(cave, relativeHumidity, Math.max(relativeHumidity, 0.85));
        }
        if (at.inWater()) {
            relativeHumidity = 1;
        }

        return new Breakdown(
            noise, deeperDown, water, snowy,
            humidity, dryness, relativeHumidity,
            baseMean, offset, lapse, mean,
            warmth, amplitude,
            depth, cave,
            at.season(), seasonal,
            day, diurnalAmplitude, diurnal,
            shelter, weather, wind, valley, pooling,
            at.inWater(), waterCelsius,
            air, at.inWater() ? waterCelsius : air
        );
    }

    // Valley depth changes only with terrain, so it's cached per 4×4 column cell. Positions are
    // world-agnostic here; the Overworld is the only dimension with pooling in practice.
    private static final Long2DoubleMap VALLEYS = Long2DoubleMaps.synchronize(new Long2DoubleOpenHashMap());

    public static void clearCache() {
        VALLEYS.clear();
    }

    private static double valleyDepth(IntBinaryOperator terrain, int x, int z, int ground, int r) {
        if (!(terrain instanceof LiveTerrain)) {
            return computeValleyDepth(terrain, x, z, ground, r);
        }
        long key = ChunkPos.asLong(x >> 2, z >> 2) ^ ((long) r << 58);
        double cached = VALLEYS.getOrDefault(key, Double.NaN);
        if (!Double.isNaN(cached)) {
            return cached;
        }
        double depth = computeValleyDepth(terrain, x, z, ground, r);
        if (VALLEYS.size() > 1 << 16) {
            VALLEYS.clear();
        }
        VALLEYS.put(key, depth);
        return depth;
    }

    // How far a column's ground lies below the average of eight columns around it
    private static double computeValleyDepth(IntBinaryOperator terrain, int x, int z, int ground, int r) {
        int d = (int) Math.round(r * Math.sqrt(0.5));
        int[][] offsets = {{r, 0}, {-r, 0}, {0, r}, {0, -r}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
        double sum = 0;
        int n = 0;
        for (int[] o : offsets) {
            int y = terrain.applyAsInt(x + o[0], z + o[1]);
            if (y != Integer.MIN_VALUE) {
                sum += y;
                n++;
            }
        }
        return n == 0 ? 0 : Math.max(0, sum / n - ground);
    }
}
