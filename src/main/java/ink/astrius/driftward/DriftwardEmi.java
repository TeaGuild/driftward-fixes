package ink.astrius.driftward;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import ink.astrius.driftward.grindstone.DisenchantingEmiRecipe;
import ink.astrius.driftward.kube.DriftwardKubeJSPlugin;
import ink.astrius.driftward.trickster.TricksterGatedEmiRecipes;

import java.util.ArrayList;
import java.util.List;

@EmiEntrypoint
public class DriftwardEmi implements EmiPlugin {
    public static List<EmiRecipe> prependRecipes = new ArrayList<>();

    @Override
    public void initialize(EmiInitRegistry registry) {
        prependRecipes = new ArrayList<>();
        DriftwardKubeJSPlugin.initEmi(registry);
    }

    @Override
    public void register(EmiRegistry registry) {
        DisenchantingEmiRecipe.register(registry);
        TricksterGatedEmiRecipes.register(registry);
        Cobblegen.registerEmi(registry);
        CrystaliteElytraRepairRecipeFix.register(registry);
    }
}
