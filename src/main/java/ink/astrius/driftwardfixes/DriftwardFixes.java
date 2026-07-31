package ink.astrius.driftwardfixes;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;

@Mod("driftwardfixes")
public class DriftwardFixes {
    public DriftwardFixes(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        modEventBus.addListener(DriftwardFixes::onSetup);
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
