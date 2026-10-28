package ink.astrius.driftward.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

import java.util.function.BiFunction;

// Create: Avionics' rain gauge and barometer read the weather layer, through InterModComms so
// neither mod needs the other: Avionics takes a `BiFunction<Level, BlockPos, double[]>` under
// "weather_source" and calls it on the server thread about once a second per instrument.
//
// It answers null to leave a position to vanilla (other dimensions, local weather off), else
// {kind, mm/h, hPa}: kind 0 none, 1 rain, 2 snow; the rate as liquid water; the pressure as the
// weather's anomaly, which Avionics adds to Sable's altitude curve.
//
// Rate from the precipitation fraction p (0.05 where rain starts .. 1): 0.5 + 9.5 p² mm/h, so a
// drizzle is about 0.5, moderate rain (p 0.5) about 3 and the heaviest 10, doubled at full thunder
// (cloudbursts); snow carries half the water of rain from the same sky.
//
// The weather radar reads columns instead, under "weather_field": a `BiFunction<Level, double[],
// double[]>` taking {x0, z0, x1, z1, ...} and answering, per column, {kind, mm/h, cloud base y,
// cloud top y, wind x, wind z, freezing level y, cloud cover}: what falls out of the cloud base
// whatever the roofs and terrain under it. Like the cloud layer, it samples the world generator
// at sea level, so it works over chunks that aren't loaded and never loads one. Wind is in blocks
// per second (one block is a metre across); the freezing level is where the sea-level air
// temperature cools to the snow threshold at the model's lapse rate.
public final class ClimateAvionics {
    private static final String AVIONICS = "create_avionics";
    private static final int SURFACE = 63;
    // Same threshold as the rain itself (ClimateLocalWeather)
    private static final double WET = 0.05;
    private static final int STRIDE = 8;

    private ClimateAvionics() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ClimateAvionics::onEnqueue);
    }

    private static void onEnqueue(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded(AVIONICS)) {
            BiFunction<Level, BlockPos, double[]> source = ClimateAvionics::sample;
            InterModComms.sendTo(AVIONICS, "weather_source", () -> source);
            BiFunction<Level, double[], double[]> field = ClimateAvionics::columns;
            InterModComms.sendTo(AVIONICS, "weather_field", () -> field);
        }
    }

    // Cheap: the weather comes from the per-column cache and rain-or-snow from the per-cell one
    private static double[] sample(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !ClimateLocalWeather.active(level) || !level.isLoaded(pos)) {
            return null;
        }
        var w = ClimateLocalWeather.at(serverLevel, pos);
        // Same test the rain itself uses (wet column, below the cloud base), plus open sky
        if (ClimateLocalWeather.dry(level, pos) || !level.canSeeSky(pos.above())) {
            return new double[] {0, 0, w.pressure()};
        }
        var biome = level.getBiome(pos);
        double vanilla = ClimatePrecipitation.vanillaTemperature(level, biome, pos);
        boolean snow = Double.isNaN(vanilla)
            ? biome.value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW
            : vanilla < 0.15;
        return new double[] {snow ? 2 : 1, rate(w, snow), w.pressure()};
    }

    private static double rate(ClimateWeather.State w, boolean snow) {
        double p = w.precipitation();
        double rate = (0.5 + 9.5 * p * p) * (1 + w.thunder());
        return snow ? rate * 0.5 : rate;
    }

    private static double[] columns(Level level, double[] query) {
        if (!(level instanceof ServerLevel serverLevel) || !ClimateLocalWeather.active(level)) {
            return null;
        }
        int n = query.length / 2;
        var out = new double[n * STRIDE];
        for (int i = 0; i < n; i++) {
            column(serverLevel, (int) Math.floor(query[2 * i]), (int) Math.floor(query[2 * i + 1]), out, i * STRIDE);
        }
        return out;
    }

    // The weather over a column from the generator's climate at sea level, as the cloud layer does it
    private static void column(ServerLevel level, int x, int z, double[] out, int at) {
        var c = ClimateConstants.get();
        var source = level.getChunkSource();
        var sampler = source.randomState().sampler();
        int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(SURFACE), qz = QuartPos.fromBlock(z);
        var biome = source.getGenerator().getBiomeSource().getNoiseBiome(qx, qy, qz, sampler);
        Climate.TargetPoint point = sampler.sample(qx, qy, qz);
        var noise = new ClimateModel.Noise(
            Climate.unquantizeCoord(point.temperature()),
            Climate.unquantizeCoord(point.humidity()),
            Climate.unquantizeCoord(point.continentalness())
        );
        var pos = new BlockPos(x, SURFACE, z);
        var conditions = new ClimateModel.Conditions(SURFACE,
            ClimateSeasons.factor(level, c.seasonPeak()), ClimateSeasons.factor(level, c.seasonPeak(), c.waterLag()),
            level.getDayTime(), 0, 0, 0, 0, false, null);
        var climate = ClimateModel.breakdown(level, pos, biome, noise, conditions);
        var w = ClimateWeather.at(level.getSeed(), x, z, SURFACE, level.getGameTime(), climate,
            biome.value().hasPrecipitation(), null);
        double celsius = ClimatePrecipitation.celsius(biome, climate);
        boolean snow = celsius < c.snowCelsius();
        boolean wet = w.precipitation() >= WET;
        out[at] = wet ? (snow ? 2 : 1) : 0;
        out[at + 1] = wet ? rate(w, snow) : 0;
        out[at + 2] = w.cloudBase();
        out[at + 3] = w.cloudTop();
        out[at + 4] = w.windX();
        out[at + 5] = w.windZ();
        out[at + 6] = c.lapsePerBlock() > 0 ? SURFACE + (celsius - c.snowCelsius()) / c.lapsePerBlock() : Double.NaN;
        out[at + 7] = w.cloud();
    }
}
