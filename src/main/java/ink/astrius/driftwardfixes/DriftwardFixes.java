package ink.astrius.driftwardfixes;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod("driftwardfixes")
public class DriftwardFixes {
    public static final String MOD_ID = "driftwardfixes";
    public static final DeferredRegister<CriterionTrigger<?>> CRITERIA_TRIGGERS = DeferredRegister.create(
        BuiltInRegistries.TRIGGER_TYPES,
        MOD_ID
    );
    public static final Supplier<ItemUsedOnLocationTrigger>
        ROTATED_WITH_WRENCH = CRITERIA_TRIGGERS.register(
        "rotated_with_wrench", ItemUsedOnLocationTrigger::new
    );

    public DriftwardFixes(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        modEventBus.addListener(DriftwardFixes::onSetup);
        CRITERIA_TRIGGERS.register(modEventBus);
    }

    public static void onSetup(final FMLCommonSetupEvent event) {
        FluidInteractionRegistry.addInteraction(
            NeoForgeMod.LAVA_TYPE.value(),
            new FluidInteractionRegistry.InteractionInformation(
                NeoForgeMod.WATER_TYPE.value(),
                (fluidState) -> fluidState.isSource()
                    ? Blocks.OBSIDIAN.defaultBlockState()
                    : Blocks.COBBLED_DEEPSLATE.defaultBlockState()
            )
        );
    }
}
