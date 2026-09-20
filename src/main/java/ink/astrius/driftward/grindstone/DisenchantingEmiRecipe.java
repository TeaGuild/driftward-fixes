package ink.astrius.driftward.grindstone;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import ink.astrius.driftward.reg.DisenchantingRecipe;
import ink.astrius.driftward.reg.DriftwardReg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

public class DisenchantingEmiRecipe extends BasicEmiRecipe {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/grindstone.png");

    public DisenchantingEmiRecipe(RecipeHolder<DisenchantingRecipe> recipe) {
        super(VanillaEmiRecipeCategories.GRINDING, recipe.id(), 116, 56);
        inputs.add(EmiIngredient.of(recipe.value().ingredient.ingredient));
        outputs.add(EmiStack.of(recipe.value().result));
    }

    public static void register(EmiRegistry registry) {
        final var manager = registry.getRecipeManager();
        System.out.println("MEOW");
        for (final var recipe : manager.getAllRecipesFor(DriftwardReg.DISENCHANTING_TYPE.get())) {
            System.out.println("MEOW " + recipe.id().toString());
            registry.addRecipe(new DisenchantingEmiRecipe(recipe));
        }
    }

    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(BACKGROUND, 0, 0, 116, 56, 30, 15);
        widgets.addSlot(inputs.getFirst(), 18, 3).drawBack(false);
        widgets.addSlot(outputs.getFirst(), 98, 18).drawBack(false).recipeContext(this);
    }
}
