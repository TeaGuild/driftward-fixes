package ink.astrius.driftward.climate;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

// Meteorological constants of the climate model. Defaults match the Constants sheet of the climate
// workbook; the model reads them through `ClimateConstants`, which is refreshed on config load.
public final class ClimateConfig {
    public final ModConfigSpec.BooleanValue enabled;

    public final ModConfigSpec.ConfigValue<List<? extends Double>> bandNoise;
    public final ModConfigSpec.ConfigValue<List<? extends Double>> bandCelsius;
    public final ModConfigSpec.DoubleValue lapsePerBlock;

    public final ModConfigSpec.ConfigValue<List<? extends Double>> amplitudeContinentalness;
    public final ModConfigSpec.ConfigValue<List<? extends Double>> amplitudeCelsius;
    public final ModConfigSpec.DoubleValue tropicMean;
    public final ModConfigSpec.DoubleValue tropicHumidity;
    public final ModConfigSpec.DoubleValue tropicDamping;
    public final ModConfigSpec.DoubleValue maxAmplitude;
    public final ModConfigSpec.DoubleValue seasonPeak;

    public final ModConfigSpec.DoubleValue humidityNoiseWeight;
    public final ModConfigSpec.DoubleValue dryFactor;
    public final ModConfigSpec.DoubleValue waterHumidity;
    public final ModConfigSpec.DoubleValue aridLimit;
    public final ModConfigSpec.DoubleValue aridAmplitude;
    public final ModConfigSpec.DoubleValue aridBonus;

    public final ModConfigSpec.DoubleValue snowTemperature;
    public final ModConfigSpec.DoubleValue snowMargin;

    public final ModConfigSpec.DoubleValue caveBase;
    public final ModConfigSpec.DoubleValue caveDepthStart;
    public final ModConfigSpec.DoubleValue caveDepthFull;
    public final ModConfigSpec.DoubleValue geothermalPerBlock;

    public final ModConfigSpec.DoubleValue diurnalAmplitude;
    public final ModConfigSpec.DoubleValue diurnalArid;
    public final ModConfigSpec.DoubleValue diurnalPeakTick;

    public final ModConfigSpec.DoubleValue deeperDownBase;
    public final ModConfigSpec.DoubleValue deeperDownScale;

    public ClimateConfig(ModConfigSpec.Builder b) {
        b.comment(
            "Position-based climate model feeding Thermoo's ambient temperature in the Overworld and",
            "the Deeper Down. Temperatures are in Celsius; noise values are the world generator's",
            "multi-noise climate parameters at the queried position."
        ).push("climate");

        enabled = b.comment("Master toggle. When off, the provider reports nothing and other definitions apply.")
            .define("enabled", true);

        b.push("temperature");
        bandNoise = b.comment("Noise temperature points (ascending) of the annual-mean curve")
            .defineList("bandNoise", List.of(-1.2, -0.725, -0.3, 0.025, 0.375, 0.775, 1.2), ClimateConfig::isNumber);
        bandCelsius = b.comment("Annual mean Celsius at each noise point; linear in between")
            .defineList("bandCelsius", List.of(-16.0, -9.0, 4.0, 12.0, 21.0, 27.0, 30.0), ClimateConfig::isNumber);
        lapsePerBlock = b.comment("Cooling per block above sea level (stylised lapse rate)")
            .defineInRange("lapsePerBlock", 0.05, 0.0, 1.0);
        b.pop();

        b.push("seasons");
        amplitudeContinentalness = b.comment("Continentalness points (ascending) of the seasonal amplitude curve")
            .defineList("amplitudeContinentalness", List.of(-1.2, -0.455, -0.19, -0.11, 0.03, 0.3), ClimateConfig::isNumber);
        amplitudeCelsius = b.comment("Seasonal amplitude (Celsius either side of the mean) at each point")
            .defineList("amplitudeCelsius", List.of(3.0, 4.0, 6.0, 9.0, 12.0, 15.0), ClimateConfig::isNumber);
        tropicMean = b.comment("Tropical damping applies from this annual mean")
            .defineInRange("tropicMean", 18.0, -100.0, 100.0);
        tropicHumidity = b.comment("... and from this noise humidity")
            .defineInRange("tropicHumidity", 0.1, -2.0, 2.0);
        tropicDamping = b.comment("Amplitude multiplier in the wet tropics")
            .defineInRange("tropicDamping", 0.4, 0.0, 1.0);
        maxAmplitude = b.comment("Amplitude cap")
            .defineInRange("maxAmplitude", 16.0, 0.0, 50.0);
        seasonPeak = b.comment("Point of the Serene Seasons year (0 = start of spring) with the warmest temperature")
            .defineInRange("seasonPeak", 0.375, 0.0, 1.0);
        b.pop();

        b.push("humidity");
        humidityNoiseWeight = b.comment("Weight of noise humidity; the rest is the biome's downfall")
            .defineInRange("noiseWeight", 0.5, 0.0, 1.0);
        dryFactor = b.comment("Humidity multiplier for biomes without precipitation")
            .defineInRange("dryFactor", 0.6, 0.0, 1.0);
        waterHumidity = b.comment("Humidity floor for ocean, river and beach biomes")
            .defineInRange("waterHumidity", 0.6, 0.0, 1.0);
        aridLimit = b.comment("Dryness starts below this humidity (dryness 0 here, 1 at humidity 0)")
            .defineInRange("aridLimit", 0.35, 0.01, 1.0);
        aridAmplitude = b.comment("Extra seasonal amplitude at full dryness (multiplier)")
            .defineInRange("aridAmplitude", 0.3, 0.0, 5.0);
        aridBonus = b.comment("Extra annual mean at full dryness")
            .defineInRange("aridBonus", 3.0, -20.0, 20.0);
        b.pop();

        b.push("snow");
        snowTemperature = b.comment("Vanilla biome temperature below which it snows")
            .defineInRange("snowTemperature", 0.15, -2.0, 2.0);
        snowMargin = b.comment("In snowy biomes, winter stays at or below this")
            .defineInRange("snowMargin", -3.0, -50.0, 10.0);
        b.pop();

        b.push("underground");
        caveBase = b.comment("Cave temperature near the surface (about the regional annual mean)")
            .defineInRange("caveBase", 12.0, -50.0, 100.0);
        caveDepthStart = b.comment("Blocks below the surface where the cave climate starts to take over")
            .defineInRange("caveDepthStart", 6.0, 0.0, 256.0);
        caveDepthFull = b.comment("Blocks below the surface where the cave climate fully applies")
            .defineInRange("caveDepthFull", 20.0, 1.0, 512.0);
        geothermalPerBlock = b.comment("Warming per block below caveDepthFull (geothermal gradient)")
            .defineInRange("geothermalPerBlock", 0.03, 0.0, 1.0);
        b.pop();

        b.push("day");
        diurnalAmplitude = b.comment("Day/night swing (Celsius either side) at the surface")
            .defineInRange("amplitude", 3.0, 0.0, 30.0);
        diurnalArid = b.comment("Extra day/night swing at full dryness (multiplier)")
            .defineInRange("arid", 1.5, 0.0, 10.0);
        diurnalPeakTick = b.comment("Time of day (ticks) with the warmest temperature; 8000 is 14:00")
            .defineInRange("peakTick", 8000.0, 0.0, 24000.0);
        b.pop();

        b.push("deeper_down");
        deeperDownBase = b.comment("Deeper Down mean at noise temperature 0 (no seasons there)")
            .defineInRange("base", 18.0, -50.0, 100.0);
        deeperDownScale = b.comment("Deeper Down Celsius per unit of noise temperature")
            .defineInRange("scale", 12.0, 0.0, 100.0);
        b.pop();

        b.pop();
    }

    private static boolean isNumber(Object o) {
        return o instanceof Number;
    }
}
