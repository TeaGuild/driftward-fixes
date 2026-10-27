package ink.astrius.driftward.climate;

import ink.astrius.driftward.config.ServerConfig;

import java.util.List;

// Immutable snapshot of `ClimateConfig`, so that the model doesn't unbox config values on every query.
public record ClimateConstants(
    boolean enabled,
    double[] bandNoise, double[] bandCelsius, double lapsePerBlock,
    double[] amplitudeContinentalness, double[] amplitudeCelsius,
    double warmStart, double warmFull, double warmDamping, double tropicHumidity, double tropicDamping,
    double maxAmplitude, double seasonPeak,
    double humidityNoiseWeight, double dryFactor, double waterHumidity,
    double aridLimit, double aridAmplitude, double aridBonus,
    double snowTemperature, double snowMargin,
    double caveBase, double caveDepthStart, double caveDepthFull, double geothermalPerBlock,
    double diurnalAmplitude, double diurnalArid, double diurnalPeakTick, double diurnalOcean,
    double deeperDownBase, double deeperDownScale,
    double rainCooling, double dryRainCooling, double thunderCooling, double cloudDamping,
    double shelterSeasonDamping, double shelterDiurnalDamping,
    double windStart, double windFull, double windCooling, double windStorm,
    double waterSeasonDamping, double waterLag, double waterDiurnalDamping, double freshwaterMin, double seawaterMin,
    double poolRadius, double poolDepth, double poolCooling, double poolHeight,
    boolean precipitationEnabled, double snowCelsius, double celsiusPerUnit,
    int syncInterval, int syncRadius, int syncSpacing,
    double meltPercent, int meltRolls, boolean surfaceEnabled, double surfaceMargin,
    boolean humidityEnabled,
    boolean cropsEnabled, double cropMinGrowth, double cropFrost, double cropKill, double cropFrostChance,
    double hardyOffset,
    boolean mobsEnabled, double coldMobMax, double warmMobMin
) {
    private static volatile ClimateConstants current;

    public static ClimateConstants get() {
        var c = current;
        if (c == null) {
            c = reload();
        }
        return c;
    }

    public static ClimateConstants reload() {
        var cfg = ServerConfig.CLIMATE;
        var c = new ClimateConstants(
            cfg.enabled.get(),
            array(cfg.bandNoise.get()), array(cfg.bandCelsius.get()), cfg.lapsePerBlock.get(),
            array(cfg.amplitudeContinentalness.get()), array(cfg.amplitudeCelsius.get()),
            cfg.warmStart.get(), cfg.warmFull.get(), cfg.warmDamping.get(), cfg.tropicHumidity.get(),
            cfg.tropicDamping.get(),
            cfg.maxAmplitude.get(), cfg.seasonPeak.get(),
            cfg.humidityNoiseWeight.get(), cfg.dryFactor.get(), cfg.waterHumidity.get(),
            cfg.aridLimit.get(), cfg.aridAmplitude.get(), cfg.aridBonus.get(),
            cfg.snowTemperature.get(), cfg.snowMargin.get(),
            cfg.caveBase.get(), cfg.caveDepthStart.get(), cfg.caveDepthFull.get(), cfg.geothermalPerBlock.get(),
            cfg.diurnalAmplitude.get(), cfg.diurnalArid.get(), cfg.diurnalPeakTick.get(), cfg.diurnalOcean.get(),
            cfg.deeperDownBase.get(), cfg.deeperDownScale.get(),
            cfg.rainCooling.get(), cfg.dryRainCooling.get(), cfg.thunderCooling.get(), cfg.cloudDamping.get(),
            cfg.shelterSeasonDamping.get(), cfg.shelterDiurnalDamping.get(),
            cfg.windStart.get(), cfg.windFull.get(), cfg.windCooling.get(), cfg.windStorm.get(),
            cfg.waterSeasonDamping.get(), cfg.waterLag.get(), cfg.waterDiurnalDamping.get(),
            cfg.freshwaterMin.get(), cfg.seawaterMin.get(),
            cfg.poolRadius.get(), cfg.poolDepth.get(), cfg.poolCooling.get(), cfg.poolHeight.get(),
            cfg.precipitationEnabled.get(), cfg.snowCelsius.get(), cfg.celsiusPerUnit.get(),
            cfg.syncInterval.get(), cfg.syncRadius.get(), cfg.syncSpacing.get(),
            cfg.meltPercent.get(), cfg.meltRolls.get(), cfg.surfaceEnabled.get(), cfg.surfaceMargin.get(),
            cfg.humidityEnabled.get(),
            cfg.cropsEnabled.get(), cfg.cropMinGrowth.get(), cfg.cropFrost.get(), cfg.cropKill.get(),
            cfg.cropFrostChance.get(), cfg.hardyOffset.get(),
            cfg.mobsEnabled.get(), cfg.coldMobMax.get(), cfg.warmMobMin.get()
        );
        current = c;
        return c;
    }

    private static double[] array(List<? extends Number> list) {
        return list.stream().mapToDouble(Number::doubleValue).toArray();
    }

    // Piecewise linear interpolation, clamped at both ends. `xs` must be ascending.
    public static double interpolate(double[] xs, double[] ys, double x) {
        int n = Math.min(xs.length, ys.length);
        if (n == 0) {
            return 0;
        }
        if (x <= xs[0]) {
            return ys[0];
        }
        for (int i = 1; i < n; i++) {
            if (x <= xs[i]) {
                double f = (x - xs[i - 1]) / (xs[i] - xs[i - 1]);
                return ys[i - 1] + f * (ys[i] - ys[i - 1]);
            }
        }
        return ys[n - 1];
    }

    // Model Celsius as a vanilla biome temperature, with the snow line at vanilla's 0.15
    public double toVanilla(double celsius) {
        return 0.15 + (celsius - snowCelsius) / celsiusPerUnit;
    }
}
