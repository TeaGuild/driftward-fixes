package ink.astrius.driftward.client;

import ink.astrius.driftward.Driftward;
import ink.astrius.driftward.reg.ModLoaded;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

@Mod(value = Driftward.MOD_ID, dist = Dist.CLIENT)
public class DriftwardClient {
    public DriftwardClient(FMLModContainer container, IEventBus modBus, Dist dist) {
        if (Driftward.crystallarieumAmberCompat != null) {
            Driftward.crystallarieumAmberCompat.registerClient(modBus);
        }
        if (ModLoaded.SPECTRUM) {
            FoodEffectTooltips.registerClient();
            PigmentStubProperty.registerClient(modBus);
        }
        if (ModLoaded.APPLESKIN && ModLoaded.SUPPLEMENTARIES) {
            LunchBasketTooltipFix.registerClient();
        }
    }
}
