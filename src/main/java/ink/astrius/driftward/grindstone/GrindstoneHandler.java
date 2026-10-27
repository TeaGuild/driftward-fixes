package ink.astrius.driftward.grindstone;

import ink.astrius.driftward.reg.DisenchantingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.GrindstoneEvent;

public class GrindstoneHandler {
    public static void grindstoneEventHandler(final GrindstoneEvent.OnPlaceItem event) {
        final var input = new DisenchantingRecipe.Input(
            event.getTopItem(), event.getBottomItem()
        );
        final var optionalRecipe = DisenchantingRecipes.find(input);
        optionalRecipe
            .map(RecipeHolder::value)
            .ifPresent(recipe -> {
                final var result = recipe.assemble(input, null);
                event.setOutput(result);
                recipe.xp.ifPresent(xp -> event.setXp(xp * result.getCount()));
            });
    }
}
