package ink.astrius.driftward;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import ink.astrius.driftward.grindstone.DisenchantingEmiRecipe;
import ink.astrius.driftward.kube.DriftwardKubeJSPlugin;
import ink.astrius.driftward.trickster.TricksterGatedEmiRecipes;

@EmiEntrypoint
public class DriftwardEmiPlugin implements EmiPlugin {
    @Override
    public void initialize(EmiInitRegistry registry) {
        DriftwardKubeJSPlugin.initEmi(registry);
    }

    @Override
    public void register(EmiRegistry registry) {
        DisenchantingEmiRecipe.register(registry);
        TricksterGatedEmiRecipes.register(registry);
        Cobblegen.registerEmi(registry);
    }
}
