package ink.astrius.driftward.config;

import ink.astrius.driftward.climate.ClimateConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue BALLOON_TEMPERATURE_ENABLED;
    public static final ModConfigSpec.DoubleValue BALLOON_REFERENCE_TEMP_C;
    public static final ModConfigSpec.DoubleValue BALLOON_SENSITIVITY_PER_C;
    public static final ModConfigSpec.DoubleValue BALLOON_MIN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BALLOON_MAX_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue ELYTRA_EXHAUSTION_PER_TICK;

    public static final ClimateConfig CLIMATE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Hot-air and steam balloon lift scales with ambient temperature (delta-T buoyancy):",
                "colder outside air = more lift, hotter air = less. Levitite is a separate system and is unaffected.")
            .push("balloon_temperature");

        BALLOON_TEMPERATURE_ENABLED = b
            .comment("Master toggle for temperature-dependent balloon lift.")
            .define("enabled", true);

        BALLOON_REFERENCE_TEMP_C = b
            .comment("Ambient temperature (Celsius) at which lift is unchanged (multiplier = 1.0).")
            .defineInRange("referenceTemperatureCelsius", 20.0, -273.0, 1000.0);

        BALLOON_SENSITIVITY_PER_C = b
            .comment("Lift multiplier change per Celsius degree below the reference.",
                "0.0125 => +1.25% lift per degree colder; with the default clamp this reaches 1.5x near -20C and 0.7x near +44C.")
            .defineInRange("sensitivityPerCelsius", 0.0125, 0.0, 1.0);

        BALLOON_MIN_MULTIPLIER = b
            .comment("Lowest lift multiplier (very hot environments).")
            .defineInRange("minMultiplier", 0.7, 0.0, 1.0);

        BALLOON_MAX_MULTIPLIER = b
            .comment("Highest lift multiplier (very cold environments / high altitude).")
            .defineInRange("maxMultiplier", 1.5, 1.0, 10.0);

        b.pop();

        b.comment("Gliding with an elytra costs food, so that airships stay the efficient way to travel far.")
            .push("elytra");

        ELYTRA_EXHAUSTION_PER_TICK = b
            .comment("Exhaustion added every tick while gliding. Sprinting costs about 0.028 per tick at full speed; 0 disables.")
            .defineInRange("exhaustionPerTick", 0.02, 0.0, 1.0);

        b.pop();

        CLIMATE = new ClimateConfig(b);

        SPEC = b.build();
    }

    private ServerConfig() {}
}
