package ink.astrius.driftward.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

// The one place other systems ask about the air at a point: temperature, humidity, wind, cloud,
// precipitation and pressure, for any position in a loaded area. Everything comes from the climate
// model and its weather layer, so it stays consistent with what Thermoo, snow and crops see.
//
// Pressure is on Sable's scale (1 at sea level, exp(-0.004 per block) above), times the weather's
// highs and lows and the temperature, so Sable's buoyancy can be switched over to it as is.
public final class ClimateAtmosphere {
    // Sable's default dimension physics: relative pressure falls by this per block above sea level
    public static final double SABLE_PRESSURE_SLOPE = 0.004;
    private static final double SEA_LEVEL_HPA = 1013.25;
    private static final double REFERENCE_KELVIN = 288.15;

    private ClimateAtmosphere() {}

    public static ClimateModel.Breakdown climate(Level level, BlockPos pos) {
        return ClimateModel.breakdown(level, pos, level.getBiome(pos));
    }

    public static double temperature(Level level, BlockPos pos) {
        return climate(level, pos).celsius();
    }

    public static double relativeHumidity(Level level, BlockPos pos) {
        return climate(level, pos).relativeHumidity();
    }

    public static ClimateWeather.State weather(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return ClimateModel.weather(level, pos, biome, ClimateModel.sample(level, pos),
            ClimateModel.live(level, pos, biome, true));
    }

    // Wind in m/s (x east, y up, z south). Slower near the ground and in shelter, following a rough
    // logarithmic profile over the first few dozen blocks above the surface.
    public static Vec3 wind(Level level, BlockPos pos) {
        var w = weather(level, pos);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        double above = Math.max(0, pos.getY() - ground);
        double profile = Math.min(1.2, 0.35 + 0.25 * Math.log1p(above));
        double shelter = 1 - level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) / 15.0;
        double factor = profile * (1 - shelter);
        return new Vec3(w.windX() * factor, 0, w.windZ() * factor);
    }

    // Relative pressure on Sable's scale at a height, with the weather's anomaly
    public static double pressure(Level level, BlockPos pos) {
        double anomaly = weather(level, pos).pressure();
        return heightPressure(level, pos.getY()) * (1 + anomaly / SEA_LEVEL_HPA);
    }

    // Relative air density (1 at sea level and 15 °C): pressure over absolute temperature
    public static double density(Level level, BlockPos pos) {
        double kelvin = temperature(level, pos) + 273.15;
        return pressure(level, pos) * REFERENCE_KELVIN / Math.max(1, kelvin);
    }

    public static double heightPressure(Level level, double y) {
        return Math.exp(-SABLE_PRESSURE_SLOPE * (y - level.getSeaLevel()));
    }
}
