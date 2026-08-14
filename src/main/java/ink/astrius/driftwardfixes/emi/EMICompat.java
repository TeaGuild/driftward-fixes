package ink.astrius.driftwardfixes.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class EMICompat implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipe(new PearlDisenchantingRecipe());
    }
}
