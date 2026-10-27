package ink.astrius.driftward.potions;

import de.dafuqs.spectrum.registries.SpectrumPotions;
import de.dafuqs.spectrum.registries.SpectrumRecipeTypes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import ink.astrius.driftward.Driftward;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class PotionsEmi {
    public static void registerEmi(EmiRegistry registry) {
        final var waterBottle = EmiStack.of(
            PotionContents.createItemStack(Items.POTION, Potions.WATER)
        );
        final var stacksIterator = EmiStackList.stacks.listIterator();
        while (stacksIterator.hasNext() && !stacksIterator.next().equals(waterBottle)) {}

        final var brewingRecipes = registry.getRecipeManager()
            .getAllRecipesFor(SpectrumRecipeTypes.POTION_WORKSHOP_BREWING);
        for (var recipeHolder : brewingRecipes) {
            final var recipe = recipeHolder.value();
            final var result = EmiStack.of(recipe.getResultItem(null));
            stacksIterator.add(result);
        }

        registry.removeEmiStacks(stack -> {
            final var item = stack.getItemStack().getItem();
            if (!(
                item.equals(Items.POTION)
                    || item.equals(Items.SPLASH_POTION)
                    || item.equals(Items.LINGERING_POTION)
                    || item.equals(Items.TIPPED_ARROW)
                    || item.equals(Driftward.BAMBOO_SPIKES_TIPPED)
            )) {
                return false;
            }

            final var contents = stack.getItemStack().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            final var isPigment = contents.potion().map(x -> x.is(SpectrumPotions.PIGMENT_POTION)).orElse(false);
            final var isWater = contents.potion().map(x -> x.is(Potions.WATER)).orElse(false);
            return !(isPigment && (contents.hasEffects() || !item.equals(Items.POTION)) || isWater && item.equals(Items.POTION));
        });
    }
}
