package ink.astrius.driftward.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

// Ambient temperature from the world generator's own climate noise at a position:
// the annual mean from noise temperature, the seasonal swing from continentality and dryness,
// cooling with altitude, a cave climate below the surface, plus season and time of day.
public final class ClimateModel {
    private static final ResourceLocation DEEPER_DOWN = ResourceLocation.fromNamespaceAndPath("spectrum", "deeper_down");
    private static final TagKey<Biome> IS_OCEAN = biomeTag("is_ocean");
    private static final TagKey<Biome> IS_RIVER = biomeTag("is_river");
    private static final TagKey<Biome> IS_BEACH = biomeTag("is_beach");

    private ClimateModel() {}

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    // Noise climate at a position; `null` where the generator has no climate noise (or on the client)
    private record Noise(double temperature, double humidity, double continentalness) {}

    private static Noise sample(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        var source = serverLevel.getChunkSource();
        if (!(source.getGenerator() instanceof NoiseBasedChunkGenerator)) {
            return null;
        }
        Climate.TargetPoint point = source.randomState().sampler().sample(
            QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())
        );
        return new Noise(
            Climate.unquantizeCoord(point.temperature()),
            Climate.unquantizeCoord(point.humidity()),
            Climate.unquantizeCoord(point.continentalness())
        );
    }

    public static double temperature(Level level, BlockPos pos, Holder<Biome> biome) {
        var c = ClimateConstants.get();
        var correction = biome.getData(BiomeClimate.DATA_MAP);
        var settings = biome.value().getModifiedClimateSettings();
        boolean deeperDown = level.dimension().location().equals(DEEPER_DOWN);
        boolean water = biome.is(IS_OCEAN) || biome.is(IS_RIVER) || biome.is(IS_BEACH);

        // Without noise (client side, flat worlds), approximate the noise axes from the biome itself
        var noise = sample(level, pos);
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

        // Annual mean and seasonal amplitude at sea level
        double mean;
        double amplitude;
        if (deeperDown) {
            mean = c.deeperDownBase() + c.deeperDownScale() * t;
            amplitude = 0;
        } else {
            mean = correction != null && correction.mean().isPresent()
                ? correction.mean().get()
                : ClimateConstants.interpolate(c.bandNoise(), c.bandCelsius(), t);
            amplitude = ClimateConstants.interpolate(c.amplitudeContinentalness(), c.amplitudeCelsius(), continentalness);
            if (mean >= c.tropicMean() && h >= c.tropicHumidity()) {
                amplitude *= c.tropicDamping();
            }
            amplitude = Math.min(c.maxAmplitude(), amplitude * (1 + c.aridAmplitude() * dryness));
            mean += c.aridBonus() * dryness;
        }
        if (correction != null) {
            mean += correction.offset();
        }

        // Altitude
        if (!deeperDown && (correction == null || correction.lapse())) {
            mean -= c.lapsePerBlock() * Math.max(0, pos.getY() - level.getSeaLevel());
        }

        // Snowy biomes keep winter below freezing
        boolean snowy = settings.temperature() < c.snowTemperature()
            || settings.temperatureModifier() == Biome.TemperatureModifier.FROZEN;
        if (!deeperDown && snowy) {
            mean = Math.min(mean, amplitude + c.snowMargin());
        }

        // Underground: blend into the cave climate, which has no seasons and warms with depth
        double exposure = 1;
        if (!deeperDown) {
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
            double depth = surface - pos.getY();
            double cave = Mth.clamp((depth - c.caveDepthStart()) / (c.caveDepthFull() - c.caveDepthStart()), 0, 1);
            if (cave > 0) {
                double caveMean = c.caveBase() + c.geothermalPerBlock() * Math.max(0, depth - c.caveDepthFull())
                    + (correction != null ? correction.offset() : 0);
                mean = Mth.lerp(cave, mean, caveMean);
                amplitude *= 1 - cave;
                exposure = 1 - cave;
            }
        }

        // Season and time of day
        double season = ClimateSeasons.factor(level, c.seasonPeak());
        double day = Math.cos(2 * Math.PI * ((level.getDayTime() % 24000) - c.diurnalPeakTick()) / 24000.0);
        double diurnal = deeperDown ? 0 : exposure * c.diurnalAmplitude() * (1 + c.diurnalArid() * dryness);

        return mean + amplitude * season + diurnal * day;
    }
}
