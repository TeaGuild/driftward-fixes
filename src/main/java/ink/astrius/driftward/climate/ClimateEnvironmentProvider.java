package ink.astrius.driftward.climate;

import com.github.thedeathlycow.thermoo.api.ThermooRegistryKeys;
import com.github.thedeathlycow.thermoo.api.environment.component.EnvironmentComponentTypes;
import com.github.thedeathlycow.thermoo.api.environment.provider.EnvironmentProvider;
import com.github.thedeathlycow.thermoo.api.environment.provider.EnvironmentProviderType;
import com.github.thedeathlycow.thermoo.api.util.TemperatureRecord;
import com.github.thedeathlycow.thermoo.api.util.TemperatureUnit;
import com.mojang.serialization.MapCodec;
import ink.astrius.driftward.Driftward;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Thermoo environment provider type `driftward:climate`, used by the pack's environment definitions
public final class ClimateEnvironmentProvider implements EnvironmentProvider {
    public static final ClimateEnvironmentProvider INSTANCE = new ClimateEnvironmentProvider();
    public static final MapCodec<ClimateEnvironmentProvider> CODEC = MapCodec.unit(INSTANCE);

    private static final DeferredRegister<EnvironmentProviderType<?>> TYPES =
        DeferredRegister.create(ThermooRegistryKeys.ENVIRONMENT_PROVIDER_TYPE, Driftward.MOD_ID);
    public static final Supplier<EnvironmentProviderType<ClimateEnvironmentProvider>> TYPE =
        TYPES.register("climate", () -> new EnvironmentProviderType<>(CODEC));

    private ClimateEnvironmentProvider() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    @Override
    public void buildCurrentComponents(Level level, BlockPos pos, Holder<Biome> biome, DataComponentMap.Builder builder) {
        if (!ClimateConstants.get().enabled()) {
            return;
        }
        var climate = ClimateModel.breakdown(level, pos, biome);
        builder.set(EnvironmentComponentTypes.TEMPERATURE, new TemperatureRecord(climate.celsius(), TemperatureUnit.CELSIUS));
        if (ClimateConstants.get().humidityEnabled()) {
            builder.set(EnvironmentComponentTypes.RELATIVE_HUMIDITY, climate.relativeHumidity());
        }
    }

    @Override
    public EnvironmentProviderType<? extends EnvironmentProvider> getType() {
        return TYPE.get();
    }
}
