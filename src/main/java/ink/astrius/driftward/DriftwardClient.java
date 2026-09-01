package ink.astrius.driftward;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

@Mod(value = Driftward.MOD_ID, dist = Dist.CLIENT)
public class DriftwardClient {
    public DriftwardClient(FMLModContainer container, IEventBus modBus, Dist dist) {
        if (Driftward.crystallarieumAmberCompat != null) {
            modBus.addListener(Driftward.crystallarieumAmberCompat::registerClient);
        }
    }
}
