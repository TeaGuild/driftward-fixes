package ink.astrius.driftwardfixes.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vectorwing.farmersdelight.common.registry.ModItems;
import vectorwing.farmersdelight.common.utility.TextUtils;

import java.util.List;

@EmiEntrypoint
public class EMICompat implements EmiPlugin {
    private ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("farmersdelight", path);
    }
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipe(new PearlDisenchantingRecipe());
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.WHEAT_DOUGH.get())), List.of(TextUtils.JEI("info.dough")), id("/info/dough")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.STRAW.get())), List.of(TextUtils.JEI("info.straw")), id("/info/straw")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.HAM.get())), List.of(TextUtils.JEI("info.ham")), id("/info/ham")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.SMOKED_HAM.get())), List.of(TextUtils.JEI("info.ham")), id("/info/ham")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.FLINT_KNIFE.get())), List.of(TextUtils.JEI("info.knife")), id("/info/knife")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.IRON_KNIFE.get())), List.of(TextUtils.JEI("info.knife")), id("/info/knife")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.DIAMOND_KNIFE.get())), List.of(TextUtils.JEI("info.knife")), id("/info/knife")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.NETHERITE_KNIFE.get())), List.of(TextUtils.JEI("info.knife")), id("/info/knife")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.GOLDEN_KNIFE.get())), List.of(TextUtils.JEI("info.knife")), id("/info/knife")));

        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.WILD_CABBAGES.get()), EmiStack.of(ModItems.CABBAGE.get()), EmiStack.of(ModItems.CABBAGE_LEAF.get())), List.of(TextUtils.JEI("info.wild_cabbages")), id("/info/wild_cabbages")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.WILD_BEETROOTS.get()), EmiStack.of(Items.BEETROOT)), List.of(TextUtils.JEI("info.wild_beetroots")), id("/info/wild_beetroots")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.WILD_CARROTS.get()), EmiStack.of(Items.CARROT)), List.of(TextUtils.JEI("info.wild_carrots")), id("/info/wild_carrots")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.WILD_ONIONS.get()), EmiStack.of(ModItems.ONION.get())), List.of(TextUtils.JEI("info.wild_onions")), id("/info/wild_onions")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.WILD_POTATOES.get()), EmiStack.of(Items.POTATO)), List.of(TextUtils.JEI("info.wild_potatoes")), id("/info/wild_potatoes")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.WILD_TOMATOES.get()), EmiStack.of(ModItems.TOMATO.get())), List.of(TextUtils.JEI("info.wild_tomatoes")), id("/info/wild_tomatoes")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(ModItems.WILD_RICE.get()), EmiStack.of(ModItems.RICE.get()), EmiStack.of(ModItems.RICE_PANICLE.get())), List.of(TextUtils.JEI("info.wild_rice")), id("/info/wild_rice")));
    }
}
