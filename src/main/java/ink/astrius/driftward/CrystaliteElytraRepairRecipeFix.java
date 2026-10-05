package ink.astrius.driftward;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.EmiAnvilRecipe;
import dev.emi.emi.registry.EmiRecipes;
import net.minecraft.resources.ResourceLocation;
import org.betterx.betterend.registry.EndItems;

public class CrystaliteElytraRepairRecipeFix {
    public static void register(EmiRegistry registry) {
        registry.removeRecipes(ResourceLocation.fromNamespaceAndPath(
            "emi", "/anvil/repairing/material/betterend/elytra_crystalite/betterend/terminite_ingot"
        ));
        DriftwardEmi.prependRecipes.add(new EmiAnvilRecipe(
            EmiStack.of(EndItems.CRYSTALITE_ELYTRA),
            EmiStack.of(EndItems.ENCHANTED_MEMBRANE),
            ResourceLocation.fromNamespaceAndPath(
                Driftward.MOD_ID,
                "/anvil/repairing/crystalite_elytra_with_enchanted_membrane"
            )
        ));
    }
}
