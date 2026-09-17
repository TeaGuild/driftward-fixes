package ink.astrius.driftward;

import ink.astrius.driftward.config.ClientConfig;
import ink.astrius.driftward.config.ServerConfig;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
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

    public static final CrystallarieumAmberCompat crystallarieumAmberCompat = ModList.get().isLoaded("spectrum")
        ? new CrystallarieumAmberCompat()
        : null;

    public Driftward(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modEventBus.addListener(Driftward::onSetup);
        modEventBus.addListener(Driftward::onConfigLoad);
        modEventBus.addListener(Driftward::onConfigReload);
        DriftwardReg.CRITERIA_TRIGGERS.register(modEventBus);
        DriftwardReg.BLOCKS.register(modEventBus);
        DriftwardReg.ITEMS.register(modEventBus);
        if (crystallarieumAmberCompat != null) {
            crystallarieumAmberCompat.register(modEventBus);
        }
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

    public static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            ClientConfig.onConfigReload();
        }
    }

    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            ClientConfig.onConfigReload();
        }
    }
}
