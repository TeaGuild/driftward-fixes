package ink.astrius.driftward;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import ink.astrius.driftward.emi.ChalkEmi;
import ink.astrius.driftward.emi.CreeperOverhaulEmi;
import ink.astrius.driftward.grindstone.DisenchantingEmiRecipe;
import ink.astrius.driftward.kube.DriftwardKubeJSPlugin;
import ink.astrius.driftward.potions.PotionsEmi;
import ink.astrius.driftward.reg.ModLoaded;
import ink.astrius.driftward.trickster.TricksterGatedEmiRecipes;

import java.util.ArrayList;
import java.util.List;

@EmiEntrypoint
public class DriftwardEmi implements EmiPlugin {
    public static List<EmiRecipe> prependRecipes = new ArrayList<>();
    public static final int SLOT_SIZE = 18;
    public static final int LARGE_SLOT_SIZE = 26;

    @Override
    public void initialize(EmiInitRegistry registry) {
        prependRecipes = new ArrayList<>();
        if (ModLoaded.KUBEJS) {
            DriftwardKubeJSPlugin.initEmi(registry);
        }
    }

    @Override
    public void register(EmiRegistry registry) {
        DisenchantingEmiRecipe.registerEmi(registry);
        Cobblegen.registerEmi(registry);
        if (ModLoaded.TRICKSTER) {
            TricksterGatedEmiRecipes.registerEmi(registry);
        }
        if (ModLoaded.BETTER_END) {
            CrystaliteElytraRepairRecipeFix.registerEmi(registry);
        }
        if (ModLoaded.SPECTRUM) {
            PotionsEmi.registerEmi(registry);
        }
        if (ModLoaded.CHALK) {
            ChalkEmi.registerEmi(registry);
        }
        if (ModLoaded.CREEPER_OVERHAUL && ModLoaded.EMI_LOOT) {
            CreeperOverhaulEmi.registerEmi(registry);
        }
    }
}
