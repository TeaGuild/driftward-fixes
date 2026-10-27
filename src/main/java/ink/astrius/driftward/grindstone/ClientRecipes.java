package ink.astrius.driftward.grindstone;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Optional;

// Kept in a separate class so that client-only classes are never loaded on a dedicated server.
class ClientRecipes {
    static Optional<RecipeManager> recipeManager() {
        final var connection = Minecraft.getInstance().getConnection();
        return Optional.ofNullable(connection).map(c -> c.getRecipeManager());
    }
}
