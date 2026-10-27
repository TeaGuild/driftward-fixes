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
    public final ModConfigSpec.DoubleValue warmStart;
    public final ModConfigSpec.DoubleValue warmFull;
    public final ModConfigSpec.DoubleValue warmDamping;
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
    public final ModConfigSpec.DoubleValue diurnalOcean;

    public final ModConfigSpec.DoubleValue deeperDownBase;
    public final ModConfigSpec.DoubleValue deeperDownScale;

    public final ModConfigSpec.DoubleValue rainCooling;
    public final ModConfigSpec.DoubleValue dryRainCooling;
    public final ModConfigSpec.DoubleValue thunderCooling;
    public final ModConfigSpec.DoubleValue cloudDamping;

    public final ModConfigSpec.DoubleValue shelterSeasonDamping;
    public final ModConfigSpec.DoubleValue shelterDiurnalDamping;

    public final ModConfigSpec.DoubleValue windStart;
    public final ModConfigSpec.DoubleValue windFull;
    public final ModConfigSpec.DoubleValue windCooling;
    public final ModConfigSpec.DoubleValue windStorm;

    public final ModConfigSpec.DoubleValue waterSeasonDamping;
    public final ModConfigSpec.DoubleValue waterLag;
    public final ModConfigSpec.DoubleValue waterDiurnalDamping;
    public final ModConfigSpec.DoubleValue freshwaterMin;
    public final ModConfigSpec.DoubleValue seawaterMin;

    public final ModConfigSpec.DoubleValue poolRadius;
    public final ModConfigSpec.DoubleValue poolDepth;
    public final ModConfigSpec.DoubleValue poolCooling;
    public final ModConfigSpec.DoubleValue poolHeight;

    public final ModConfigSpec.BooleanValue precipitationEnabled;
    public final ModConfigSpec.DoubleValue snowCelsius;
    public final ModConfigSpec.DoubleValue celsiusPerUnit;
    public final ModConfigSpec.IntValue syncInterval;
    public final ModConfigSpec.IntValue syncRadius;
    public final ModConfigSpec.IntValue syncSpacing;

    public final ModConfigSpec.DoubleValue meltPercent;
    public final ModConfigSpec.IntValue meltRolls;
    public final ModConfigSpec.BooleanValue surfaceEnabled;
    public final ModConfigSpec.DoubleValue surfaceMargin;

    public final ModConfigSpec.BooleanValue humidityEnabled;

    public final ModConfigSpec.BooleanValue cropsEnabled;
    public final ModConfigSpec.DoubleValue cropMinGrowth;
    public final ModConfigSpec.DoubleValue cropFrost;
    public final ModConfigSpec.DoubleValue cropKill;
    public final ModConfigSpec.DoubleValue cropFrostChance;
    public final ModConfigSpec.DoubleValue hardyOffset;

    public final ModConfigSpec.BooleanValue mobsEnabled;
    public final ModConfigSpec.DoubleValue coldMobMax;
    public final ModConfigSpec.DoubleValue warmMobMin;

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
        warmStart = b.comment("Warm climates have small seasons: damping starts at this annual mean")
            .defineInRange("warmStart", 12.0, -100.0, 100.0);
        warmFull = b.comment("... and is complete at this annual mean")
            .defineInRange("warmFull", 22.0, -100.0, 100.0);
        warmDamping = b.comment("Amplitude multiplier in fully warm climates, wet or dry")
            .defineInRange("warmDamping", 0.35, 0.0, 1.0);
        tropicHumidity = b.comment("Warm climates from this noise humidity are wet tropics")
            .defineInRange("tropicHumidity", 0.1, -2.0, 2.0);
        tropicDamping = b.comment("Further amplitude multiplier in the fully warm wet tropics")
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
            .defineInRange("arid", 2.0, 0.0, 10.0);
        diurnalPeakTick = b.comment("Time of day (ticks) with the warmest temperature; 8000 is 14:00")
            .defineInRange("peakTick", 8000.0, 0.0, 24000.0);
        diurnalOcean = b.comment("Day/night swing over the ocean (multiplier); the sea barely changes overnight")
            .defineInRange("ocean", 0.3, 0.0, 1.0);
        b.pop();

        b.push("deeper_down");
        deeperDownBase = b.comment("Deeper Down mean at noise temperature 0 (no seasons there)")
            .defineInRange("base", 18.0, -50.0, 100.0);
        deeperDownScale = b.comment("Deeper Down Celsius per unit of noise temperature")
            .defineInRange("scale", 12.0, 0.0, 100.0);
        b.pop();

        b.push("weather");
        rainCooling = b.comment("Cooling while it rains (Celsius)")
            .defineInRange("rainCooling", 3.0, 0.0, 30.0);
        dryRainCooling = b.comment("Fraction of rain cooling in biomes without precipitation (overcast only)")
            .defineInRange("dryRainCooling", 0.3, 0.0, 1.0);
        thunderCooling = b.comment("Extra cooling during thunderstorms (Celsius)")
            .defineInRange("thunderCooling", 1.5, 0.0, 30.0);
        cloudDamping = b.comment("Day/night swing removed by full overcast (clear nights are colder)")
            .defineInRange("cloudDamping", 0.6, 0.0, 1.0);
        b.pop();

        b.comment("Shelter is how little sky light reaches a position (0 in the open, 1 with none).",
            "Sheltered air keeps the daily mean: no day/night swing, weather or wind, and a smaller season.")
            .push("shelter");
        shelterSeasonDamping = b.comment("Seasonal swing removed at full shelter")
            .defineInRange("seasonDamping", 0.5, 0.0, 1.0);
        shelterDiurnalDamping = b.comment("Day/night swing, weather and wind removed at full shelter")
            .defineInRange("diurnalDamping", 1.0, 0.0, 1.0);
        b.pop();

        b.push("wind");
        windStart = b.comment("Height where exposed wind chill starts")
            .defineInRange("start", 110.0, -64.0, 512.0);
        windFull = b.comment("Height where wind chill is full")
            .defineInRange("full", 220.0, -64.0, 512.0);
        windCooling = b.comment("Wind chill at full height in the open (Celsius)")
            .defineInRange("cooling", 4.0, 0.0, 30.0);
        windStorm = b.comment("Wind chill multiplier added during rain (thunder doubles it)")
            .defineInRange("storm", 0.5, 0.0, 5.0);
        b.pop();

        b.push("water");
        waterSeasonDamping = b.comment("Water keeps this fraction of the air's seasonal swing")
            .defineInRange("seasonFactor", 0.6, 0.0, 1.0);
        waterLag = b.comment("Water's seasons lag the air's by this fraction of a year")
            .defineInRange("lag", 0.0833, 0.0, 0.5);
        waterDiurnalDamping = b.comment("Water keeps this fraction of the day/night swing")
            .defineInRange("diurnalFactor", 0.15, 0.0, 1.0);
        freshwaterMin = b.comment("Liquid fresh water is never colder than this")
            .defineInRange("freshwaterMin", 0.0, -10.0, 10.0);
        seawaterMin = b.comment("Liquid sea water is never colder than this")
            .defineInRange("seawaterMin", -1.8, -10.0, 10.0);
        b.pop();

        b.comment("Cold air sinks into valleys on clear nights").push("pooling");
        poolRadius = b.comment("Distance of the terrain samples around a position")
            .defineInRange("radius", 24.0, 4.0, 128.0);
        poolDepth = b.comment("Valley depth (below the surrounding average) with full pooling")
            .defineInRange("depth", 16.0, 1.0, 128.0);
        poolCooling = b.comment("Cooling at full pooling at midnight (Celsius)")
            .defineInRange("cooling", 4.0, 0.0, 30.0);
        poolHeight = b.comment("Only this close above the ground")
            .defineInRange("height", 8.0, 0.0, 64.0);
        b.pop();

        b.comment("Snow, ice, melting and rain-or-snow follow the model (through Serene Seasons' hooks)")
            .push("precipitation");
        precipitationEnabled = b.define("enabled", true);
        snowCelsius = b.comment("Snow falls and water freezes below this air temperature")
            .defineInRange("snowCelsius", 0.5, -20.0, 20.0);
        celsiusPerUnit = b.comment("Celsius per unit of vanilla biome temperature, for vanilla code reading it")
            .defineInRange("celsiusPerUnit", 20.0, 1.0, 100.0);
        syncInterval = b.comment("Ticks between climate updates sent to each player (for snow/rain rendering)")
            .defineInRange("syncInterval", 40, 5, 1200);
        syncRadius = b.comment("Grid cells either side of the player in each update")
            .defineInRange("syncRadius", 4, 1, 16);
        syncSpacing = b.comment("Blocks between grid cells")
            .defineInRange("syncSpacing", 8, 2, 64);
        meltPercent = b.comment("Serene Seasons melting, all year: chance per roll (percent) that a random surface block is",
                "checked; whether it melts is up to the temperature (replaces the per-season melt settings)")
            .defineInRange("meltPercent", 25.0, 0.0, 100.0);
        meltRolls = b.comment("Rolls per chunk per tick")
            .defineInRange("meltRolls", 1, 0, 16);
        surfaceEnabled = b.comment("When a chunk loads, bring its snow and ice in line with the current climate before",
                "players see it (chunks keep last winter's snow otherwise)")
            .define("surfaceCatchUp", true);
        surfaceMargin = b.comment("Only change snow and ice this far (Celsius, daily mean) from the snow line, so nothing",
                "flickers around freezing")
            .defineInRange("surfaceMargin", 3.0, 0.0, 30.0);
        b.pop();

        b.push("humidity_component");
        humidityEnabled = b.comment("Report the model's relative humidity to Thermoo (replaces Scorchful's tag-based values)")
            .define("enabled", true);
        b.pop();

        b.push("crops");
        cropsEnabled = b.define("enabled", true);
        cropMinGrowth = b.comment("Crops don't grow below this temperature")
            .defineInRange("minGrowth", 5.0, -50.0, 50.0);
        cropFrost = b.comment("Frost: crops can lose a growth stage below this")
            .defineInRange("frost", -1.0, -50.0, 50.0);
        cropKill = b.comment("Hard frost: crops die below this")
            .defineInRange("kill", -6.0, -50.0, 50.0);
        cropFrostChance = b.comment("Chance per growth attempt to lose a stage in frost")
            .defineInRange("frostChance", 0.5, 0.0, 1.0);
        hardyOffset = b.comment("Crops in #sereneseasons:winter_crops tolerate this much more cold")
            .defineInRange("hardyOffset", 6.0, 0.0, 50.0);
        b.pop();

        b.push("mobs");
        mobsEnabled = b.comment("Natural spawns of #driftward:cold_spawning / #driftward:warm_spawning follow temperature")
            .define("enabled", true);
        coldMobMax = b.comment("Cold mobs spawn only at or below this")
            .defineInRange("coldMax", 2.0, -50.0, 50.0);
        warmMobMin = b.comment("Warm mobs spawn only at or above this")
            .defineInRange("warmMin", 24.0, -50.0, 60.0);
        b.pop();

        b.pop();
    }

    private static boolean isNumber(Object o) {
        return o instanceof Number;
    }
}
