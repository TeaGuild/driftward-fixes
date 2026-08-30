package ink.astrius.driftwardfixes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

@Mod(value = DriftwardFixes.MOD_ID, dist = Dist.CLIENT)
public class DriftwardFixesClient {
    public DriftwardFixesClient(FMLModContainer container, IEventBus modBus, Dist dist) {
        modBus.addListener(DriftwardFixes.crystallarieumAmberCompat::registerClient);
    }
}
