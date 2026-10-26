package ink.astrius.driftward.mixin;

import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.function.Function;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.mehvahdjukaar.supplementaries.integration.EMICompat;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(
    require = {
        @Condition("emi"),
        @Condition("supplementaries")
    }
)
@Mixin(EMICompat.class)
public class FixAshEmiRecipeMixin {
    @Redirect(
        method = "register",
        at = @At(
            value = "INVOKE",
            target = "Ldev/emi/emi/api/recipe/EmiWorldInteractionRecipe$Builder;rightInput(Ldev/emi/emi/api/stack/EmiIngredient;ZLjava/util/function/Function;)Ldev/emi/emi/api/recipe/EmiWorldInteractionRecipe$Builder;"
        )
    )
    private EmiWorldInteractionRecipe.Builder useFireAsInput(
        EmiWorldInteractionRecipe.Builder builder,
        EmiIngredient stack,
        boolean catalyst,
        Function mutator
    ) {
        return builder.rightInput(EmiStack.of(Items.FLINT_AND_STEEL), true);
    }
}
