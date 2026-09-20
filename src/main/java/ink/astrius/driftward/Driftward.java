package ink.astrius.driftward;

import com.sashafiesta.ccterminals.TerminalBlock;
import ink.astrius.driftward.config.ClientConfig;
import ink.astrius.driftward.config.ServerConfig;
import ink.astrius.driftward.grindstone.GrindstoneHandler;
import ink.astrius.driftward.reg.CrystallarieumAmberCompat;
import ink.astrius.driftward.reg.DriftwardReg;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Mod(Driftward.MOD_ID)
public class Driftward {
    public static final String MOD_ID = "driftward";

    public static final CrystallarieumAmberCompat crystallarieumAmberCompat = ModList.get().isLoaded("spectrum")
        ? new CrystallarieumAmberCompat()
        : null;

    public static @Nullable TerminalBlock TERMINAL_NORMAL;
    public static @Nullable TerminalBlock TERMINAL_ADVANCED;

    public Driftward(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modEventBus.addListener(Driftward::onSetup);
        modEventBus.addListener(Driftward::onConfigLoad);
        modEventBus.addListener(Driftward::onConfigReload);
        DriftwardReg.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(GrindstoneHandler::grindstoneEventHandler);
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
        if (ModList.get().isLoaded("ccterminals")) {
            TERMINAL_NORMAL = (TerminalBlock) BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("ccterminals", "terminal_normal")
            );
            TERMINAL_ADVANCED = (TerminalBlock) BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("ccterminals", "terminal_advanced")
            );
        }
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
