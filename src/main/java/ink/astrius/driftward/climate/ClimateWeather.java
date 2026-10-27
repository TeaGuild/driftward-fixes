package ink.astrius.driftward.climate;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

// Weather as a thin layer over the climate: no simulation and no saved state, just fields of the
// world seed, position and game time that behave plausibly.
//
// - Pressure: highs and lows a few kilometres across, drifting west to east with the prevailing wind
//   and slowly changing shape, so a system passes over in a day or few.
// - Wind: the prevailing drift, plus flow around the highs and lows (and a little into the lows),
//   faster on exposed heights. Stronger in winter, weaker in warm climates.
// - Cloud: from the climate's humidity, more in lows, on windward slopes and on warm humid
//   afternoons (convection), less in highs and in dry biomes.
// - Precipitation where cloud is thick; thunder from strong convection or deep lows.
// - Fog: humid, calm air, mostly around dawn, thickest where cold air pools.
//
// Units: pressure in hPa (anomaly), wind in m/s with x east and z south, the rest 0..1.
public final class ClimateWeather {
    public record State(
        double pressure, double windX, double windZ, double cloud, double precipitation, double thunder, double fog,
        double cloudBase, double cloudTop, Raw raw
    ) {
        public double windSpeed() {
            return Math.sqrt(windX * windX + windZ * windZ);
        }

        // Compass direction the wind blows from, in degrees (0 = north, 90 = east)
        public double windFrom() {
            double to = Math.toDegrees(Math.atan2(windX, -windZ));
            return Mth.positiveModulo(to + 180, 360);
        }
    }

    public static final State CALM = new State(0, 0, 0, 0, 0, 0, 0, 1e9, 1e9, new Raw(0, 0, 0, 0, 0, 0, 1e9));

    private static final double[] SCALES = {3200, 1300, 550};
    private static final double[] WEIGHTS = {1, 0.45, 0.2};
    private static final double GRADIENT_STEP = 160;
    private static final double TERRAIN_STEP = 32;
    // The Overworld's; weather only runs there
    private static final int SEA_LEVEL = 63;

    private static final Map<Long, ImprovedNoise[]> NOISE = new ConcurrentHashMap<>();

    private ClimateWeather() {}

    private static ImprovedNoise[] noise(long seed) {
        return NOISE.computeIfAbsent(seed, s -> {
            var random = RandomSource.create(s ^ 0x5745415448455221L);
            var octaves = new ImprovedNoise[SCALES.length];
            for (int i = 0; i < octaves.length; i++) {
                octaves[i] = new ImprovedNoise(random);
            }
            return octaves;
        });
    }

    // Pressure pattern -1..1 (low .. high), drifted by `drift` blocks east
    private static double pattern(ImprovedNoise[] octaves, double x, double z, double evolve, double drift) {
        double sum = 0, norm = 0;
        for (int i = 0; i < octaves.length; i++) {
            double s = SCALES[i];
            sum += WEIGHTS[i] * octaves[i].noise((x - drift) / s, evolve * (1 + i * 0.7), z / s);
            norm += WEIGHTS[i];
        }
        return Mth.clamp(sum / norm * 2.2, -1, 1);
    }

    // `climate` is the weather-free climate at the position (its humidity, temperature, season and
    // time of day drive the weather); `terrain` gives ground heights for orographic lift (may be null)
    public static State at(long seed, double x, double z, int y, long gameTime, ClimateModel.Breakdown climate,
                           boolean precipitates, IntBinaryOperator terrain) {
        var c = ClimateConstants.get();
        if (!c.weatherEnabled() || climate.deeperDown()) {
            return CALM;
        }
        var octaves = noise(seed);

        // Prevailing drift: stronger in winter, weaker in warm climates
        double steering = c.weatherSteering() * (1 - 0.35 * climate.season()) * (1 - 0.5 * climate.warmth());
        double drift = gameTime * c.weatherDrift();
        double evolve = gameTime / c.weatherEvolveTicks();

        double p = pattern(octaves, x, z, evolve, drift);
        double dpdx = (pattern(octaves, x + GRADIENT_STEP, z, evolve, drift) - pattern(octaves, x - GRADIENT_STEP, z, evolve, drift)) / 2;
        double dpdz = (pattern(octaves, x, z + GRADIENT_STEP, evolve, drift) - pattern(octaves, x, z - GRADIENT_STEP, evolve, drift)) / 2;

        // Flow around the systems (counter-clockwise around lows as seen from above, x east and
        // z south), turned partly into the lows by friction
        double k = c.weatherGradientWind();
        double windX = steering + k * (dpdz - 0.35 * dpdx);
        double windZ = k * (-dpdx - 0.35 * dpdz);

        // Exposed heights are windier
        double height = Mth.clamp((y - c.windStart()) / Math.max(1e-6, c.windFull() - c.windStart()), 0, 1);
        double exposure = 1 + 0.5 * height;
        windX *= exposure;
        windZ *= exposure;
        double speed = Math.sqrt(windX * windX + windZ * windZ);

        // Cloud: from the large-scale humidity (the generator's noise, smooth across biome borders),
        // mostly shaped by the highs and lows
        double humidity = climate.noise() != null
            ? (climate.noise().humidity() + 1) / 2
            : climate.relativeHumidity();
        double cloud = smoothstep(humidity, 0.15, 0.85) * 0.55;
        // Cold-season storminess outside warm climates
        cloud += 0.12 * Math.max(0, -climate.season()) * (1 - climate.warmth());
        cloud += 0.9 * Math.max(0, -p) - 0.45 * Math.max(0, p);
        if (terrain != null && speed > 0.5) {
            int ground = terrain.applyAsInt((int) x, (int) z);
            if (ground != Integer.MIN_VALUE) {
                int ex = terrain.applyAsInt((int) (x + TERRAIN_STEP), (int) z);
                int wx = terrain.applyAsInt((int) (x - TERRAIN_STEP), (int) z);
                int sz = terrain.applyAsInt((int) x, (int) (z + TERRAIN_STEP));
                int nz = terrain.applyAsInt((int) x, (int) (z - TERRAIN_STEP));
                if (ex != Integer.MIN_VALUE && wx != Integer.MIN_VALUE && sz != Integer.MIN_VALUE && nz != Integer.MIN_VALUE) {
                    double gx = (ex - wx) / (2 * TERRAIN_STEP);
                    double gz = (sz - nz) / (2 * TERRAIN_STEP);
                    // Air pushed uphill rises and condenses; downhill it dries (rain shadow)
                    double lift = (windX * gx + windZ * gz) * c.weatherOrographic();
                    cloud += Mth.clamp(lift, -0.35, 0.45) * humidity;
                }
            }
        }
        // Convection: warm, humid afternoons in the warm half of the year
        double convection = smoothstep(climate.airCelsius(), 18, 30) * smoothstep(humidity, 0.45, 0.9)
            * Math.max(0, climate.day()) * Math.max(0.2, 0.5 + 0.5 * climate.season());
        cloud += 0.5 * convection;
        double storm = Mth.clamp(Math.max(convection * 1.6 - 0.5, (-p - 0.75) * 2.5), 0, 1);

        // Fog: humid and calm, around dawn (cool end of the day), thicker where cold air pools
        double dawn = Math.max(0, -climate.day()) * 0.7 + 0.3;
        double fog = smoothstep(humidity, 0.75, 0.98) * (1 - smoothstep(speed, 2, 6)) * dawn
            + 0.6 * climate.valley() * smoothstep(humidity, 0.6, 0.9);

        // Cloud base at the lifting condensation level: about 125 m per °C between air and dew point,
        // with dew point ≈ T - (100 - RH%)/5, so ≈ 25 m × (100 - RH%); one block is about 7.7 m on the
        // model's vertical scale. Lows and convection lift moist air, so the base comes down there.
        double relative = Mth.clamp(0.3 + 0.65 * humidity + 0.25 * Math.max(0, -p) + 0.2 * convection, 0.05, 1);
        // The base is level across an air mass (that's why peaks reach into cloud): measured from sea
        // level plus part of the regional terrain height, not from each column's own ground
        double regional = y;
        if (terrain != null) {
            double sum = 0;
            int n = 0;
            for (int[] o : new int[][] {{128, 0}, {-128, 0}, {0, 128}, {0, -128}}) {
                int h = terrain.applyAsInt((int) x + o[0], (int) z + o[1]);
                if (h != Integer.MIN_VALUE) {
                    sum += h;
                    n++;
                }
            }
            if (n > 0) {
                regional = (sum + y) / (n + 1);
            }
        }
        double reference = SEA_LEVEL + 0.6 * Math.max(0, regional - SEA_LEVEL);
        double cloudBase = reference + Mth.clamp(3.25 * (100 - relative * 100), 12, 260);

        return finish(new Raw(p * 18, windX, windZ, cloud, storm, fog, cloudBase), precipitates);
    }

    // The smooth part of the weather, before the biome decides whether anything falls. It varies
    // over kilometres, so maps compute it on a coarse grid and interpolate.
    public record Raw(double pressure, double windX, double windZ, double cloud, double storm, double fog, double cloudBase) {
        public static Raw lerp(double t, Raw a, Raw b) {
            return new Raw(
                Mth.lerp(t, a.pressure, b.pressure), Mth.lerp(t, a.windX, b.windX), Mth.lerp(t, a.windZ, b.windZ),
                Mth.lerp(t, a.cloud, b.cloud), Mth.lerp(t, a.storm, b.storm), Mth.lerp(t, a.fog, b.fog),
                Mth.lerp(t, a.cloudBase, b.cloudBase)
            );
        }
    }

    // Dry biomes get thinner cloud and nothing falls
    public static State finish(Raw raw, boolean precipitates) {
        double cloud = Mth.clamp(precipitates ? raw.cloud() : raw.cloud() * 0.55, 0, 1);
        double precipitation = precipitates ? smoothstep(cloud, ClimateConstants.get().weatherRainCloud(), 1) : 0;
        double thunder = precipitation > 0 ? raw.storm() * Math.min(1, precipitation * 2) : 0;
        double fog = Mth.clamp(raw.fog() * (1 - 0.6 * precipitation), 0, 1);
        // Rain lowers the base; thicker cloud and storms reach higher
        double base = raw.cloudBase() - 25 * precipitation;
        double top = base + 4 + 60 * cloud * cloud + 120 * thunder;
        return new State(raw.pressure(), raw.windX(), raw.windZ(), cloud, precipitation, thunder, fog, base, top, raw);
    }

    private static double smoothstep(double x, double lo, double hi) {
        double t = Mth.clamp((x - lo) / (hi - lo), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
