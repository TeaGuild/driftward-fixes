package ink.astrius.driftward.climate;

import ink.astrius.driftward.config.ServerConfig;

import java.util.List;

// Immutable snapshot of `ClimateConfig`, so that the model doesn't unbox config values on every query.
public record ClimateConstants(
    boolean enabled,
    double[] bandNoise, double[] bandCelsius, double lapsePerBlock,
    double[] amplitudeContinentalness, double[] amplitudeCelsius,
    double tropicMean, double tropicHumidity, double tropicDamping, double maxAmplitude, double seasonPeak,
    double humidityNoiseWeight, double dryFactor, double waterHumidity,
    double aridLimit, double aridAmplitude, double aridBonus,
    double snowTemperature, double snowMargin,
    double caveBase, double caveDepthStart, double caveDepthFull, double geothermalPerBlock,
    double diurnalAmplitude, double diurnalArid, double diurnalPeakTick,
    double deeperDownBase, double deeperDownScale
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
            cfg.tropicMean.get(), cfg.tropicHumidity.get(), cfg.tropicDamping.get(), cfg.maxAmplitude.get(),
            cfg.seasonPeak.get(),
            cfg.humidityNoiseWeight.get(), cfg.dryFactor.get(), cfg.waterHumidity.get(),
            cfg.aridLimit.get(), cfg.aridAmplitude.get(), cfg.aridBonus.get(),
            cfg.snowTemperature.get(), cfg.snowMargin.get(),
            cfg.caveBase.get(), cfg.caveDepthStart.get(), cfg.caveDepthFull.get(), cfg.geothermalPerBlock.get(),
            cfg.diurnalAmplitude.get(), cfg.diurnalArid.get(), cfg.diurnalPeakTick.get(),
            cfg.deeperDownBase.get(), cfg.deeperDownScale.get()
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
}
