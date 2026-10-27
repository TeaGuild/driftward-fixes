package ink.astrius.driftward.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ChalkEmi {
    public static void registerEmi(EmiRegistry registry) {
        // Chalk 2.0 encodes its color in `dyed_color`. Index stacks carry only the color, while
        // crafted chalk is also unbreakable or damaged, so a comparison over all components (which
        // recipe viewer bridges like TMRV install) never matches colored chalk to its recipes.
        // Compare by color alone.
        final var chalk = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("chalk", "chalk"));
        registry.setDefaultComparison(chalk, Comparison.compareData(stack -> stack.get(DataComponents.DYED_COLOR)));
    }
}
