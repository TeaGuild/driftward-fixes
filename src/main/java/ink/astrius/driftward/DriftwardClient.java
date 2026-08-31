package ink.astrius.driftward;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

@Mod(value = Driftward.MOD_ID, dist = Dist.CLIENT)
public class DriftwardClient {
    public DriftwardClient(FMLModContainer container, IEventBus modBus, Dist dist) {
        modBus.addListener(Driftward.crystallarieumAmberCompat::registerClient);
    }
}
