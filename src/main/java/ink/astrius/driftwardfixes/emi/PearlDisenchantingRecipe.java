package ink.astrius.driftwardfixes.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.List;

import static ink.astrius.driftwardfixes.DriftwardFixes.CUSTOM_ENDER_PEARLS;

public class PearlDisenchantingRecipe implements EmiRecipe {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/grindstone.png");
    private final ResourceLocation id;
    private final List<EmiIngredient> input;
    private final List<EmiStack> output;

    public PearlDisenchantingRecipe() {
        id = ResourceLocation.fromNamespaceAndPath("driftward_fixes", "pearl_disenchanting");
        input = List.of(EmiIngredient.of(CUSTOM_ENDER_PEARLS));
        output = List.of(EmiStack.of(Items.ENDER_PEARL));
    }

    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.GRINDING;
    }

    public ResourceLocation getId() {
        return id;
    }

    public List<EmiIngredient> getInputs() {
        return input;
    }

    public List<EmiStack> getOutputs() {
        return output;
    }

    public boolean supportsRecipeTree() {
        return false;
    }

    public int getDisplayWidth() {
        return 116;
    }

    public int getDisplayHeight() {
        return 56;
    }

    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(BACKGROUND, 0, 0, 116, 56, 30, 15);
        widgets.addSlot(input.getFirst(), 18, 3).drawBack(false);
        widgets.addSlot(output.getFirst(), 98, 18).drawBack(false).recipeContext(this);
    }
}
