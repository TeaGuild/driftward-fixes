package ink.astrius.driftward;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import ink.astrius.driftward.grindstone.DisenchantingEmiRecipe;

@EmiEntrypoint
public class EMICompat implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        DisenchantingEmiRecipe.register(registry);
        Cobblegen.registerEmi(registry);
    }
}
