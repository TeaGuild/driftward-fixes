package ink.astrius.driftward;

import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;

import java.util.function.Supplier;

@Mod(Driftward.MOD_ID)
public class Driftward {
    public static final String MOD_ID = "driftward";
    public static final Supplier<ItemUsedOnLocationTrigger>
        ROTATED_WITH_WRENCH = DriftwardReg.CRITERIA_TRIGGERS.register(
        "rotated_with_wrench", ItemUsedOnLocationTrigger::new
    );

    public static final CrystallarieumAmberCompat crystallarieumAmberCompat = new CrystallarieumAmberCompat();

    public Driftward(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        modEventBus.addListener(Driftward::onSetup);
        DriftwardReg.CRITERIA_TRIGGERS.register(modEventBus);
        DriftwardReg.BLOCKS.register(modEventBus);
        DriftwardReg.ITEMS.register(modEventBus);
        crystallarieumAmberCompat.register(modEventBus);
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
