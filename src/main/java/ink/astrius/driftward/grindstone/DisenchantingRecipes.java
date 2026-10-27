package ink.astrius.driftward.grindstone;

import ink.astrius.driftward.reg.DisenchantingRecipe;
import ink.astrius.driftward.reg.DriftwardReg;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Optional;

public class DisenchantingRecipes {
    // Grindstone slots and events also run on the client, where a multiplayer client has no server.
    // Disenchanting recipes are synced to clients, so fall back to the client's copy there.
    public static Optional<RecipeHolder<DisenchantingRecipe>> find(DisenchantingRecipe.Input input) {
        return recipeManager().flatMap(recipes -> recipes.getRecipeFor(
            DriftwardReg.DISENCHANTING_TYPE.get(),
            input,
            null
        ));
    }

    private static Optional<RecipeManager> recipeManager() {
        final var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return Optional.of(server.getRecipeManager());
        }
        if (FMLEnvironment.dist.isClient()) {
            return ClientRecipes.recipeManager();
        }
        return Optional.empty();
    }
}
