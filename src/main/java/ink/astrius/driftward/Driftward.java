package ink.astrius.driftward;

import com.sashafiesta.ccterminals.TerminalBlock;
import ink.astrius.driftward.climate.BiomeClimate;
import ink.astrius.driftward.climate.ClimateConstants;
import ink.astrius.driftward.climate.ClimateEnvironmentProvider;
import ink.astrius.driftward.climate.ClimateCommand;
import ink.astrius.driftward.climate.ClimateLife;
import ink.astrius.driftward.climate.ClimateSync;
import ink.astrius.driftward.config.ClientConfig;
import ink.astrius.driftward.config.ServerConfig;
import ink.astrius.driftward.grindstone.GrindstoneHandler;
import ink.astrius.driftward.potions.PigmentLootModifier;
import ink.astrius.driftward.kube.DriftwardKubeJSPlugin;
import ink.astrius.driftward.reg.CrystallarieumAmberCompat;
import ink.astrius.driftward.reg.DriftwardReg;
import ink.astrius.driftward.reg.ModLoaded;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

@Mod(Driftward.MOD_ID)
public class Driftward {
    public static final String MOD_ID = "driftward";

    public static final CrystallarieumAmberCompat crystallarieumAmberCompat = ModLoaded.SPECTRUM && ModLoaded.BETTER_END
        ? new CrystallarieumAmberCompat()
        : null;

    public static @Nullable TerminalBlock TERMINAL_NORMAL;
    public static @Nullable TerminalBlock TERMINAL_ADVANCED;
    public static @Nullable Item BAMBOO_SPIKES_TIPPED;

    public Driftward(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modBus.addListener(Driftward::onSetup);
        modBus.addListener(Driftward::onConfigLoad);
        modBus.addListener(Driftward::onConfigReload);
        DriftwardReg.register(modBus);
        modBus.addListener((RegisterDataMapTypesEvent event) -> event.register(BiomeClimate.DATA_MAP));
        if (ModLoaded.THERMOO) {
            ClimateEnvironmentProvider.register(modBus);
        }
        ClimateSync.register(modBus);
        ClimateLife.register();
        ClimateCommand.register();
        NeoForge.EVENT_BUS.addListener(GrindstoneHandler::grindstoneEventHandler);
        if (crystallarieumAmberCompat != null) {
            crystallarieumAmberCompat.register();
        }
        if (ModLoaded.SPECTRUM) {
            DriftwardReg.LOOT_MODIFIERS.register("pigment_potions", () -> PigmentLootModifier.CODEC);
        }
        if (ModLoaded.KUBEJS) {
            DriftwardKubeJSPlugin.register(modBus);
        }
    }

    public static void onSetup(final FMLCommonSetupEvent event) {
        Cobblegen.onSetup();
        if (ModLoaded.CC_TERMINALS) {
            TERMINAL_NORMAL = (TerminalBlock) BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("ccterminals", "terminal_normal")
            );
            TERMINAL_ADVANCED = (TerminalBlock) BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("ccterminals", "terminal_advanced")
            );
        }
        if (ModLoaded.SUPPLEMENTARIES) {
            BAMBOO_SPIKES_TIPPED = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("supplementaries", "bamboo_spikes_tipped")
            );
        }
    }

    public static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            ClientConfig.onConfigReload();
        } else if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            ClimateConstants.reload();
        }
    }

    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            ClientConfig.onConfigReload();
        } else if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            ClimateConstants.reload();
        }
    }
}
