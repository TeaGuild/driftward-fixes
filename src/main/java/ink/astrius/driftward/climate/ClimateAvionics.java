package ink.astrius.driftward.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
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
public final class ClimateAvionics {
    private static final String AVIONICS = "create_avionics";

    private ClimateAvionics() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ClimateAvionics::onEnqueue);
    }

    private static void onEnqueue(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded(AVIONICS)) {
            BiFunction<Level, BlockPos, double[]> source = ClimateAvionics::sample;
            InterModComms.sendTo(AVIONICS, "weather_source", () -> source);
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
        double p = w.precipitation();
        double rate = (0.5 + 9.5 * p * p) * (1 + w.thunder());
        return new double[] {snow ? 2 : 1, snow ? rate * 0.5 : rate, w.pressure()};
    }
}
