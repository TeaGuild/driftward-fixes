package ink.astrius.driftward.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.registry.EmiRecipes;
import ink.astrius.driftward.DriftwardEmi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.stream.Stream;

@Mixin(EmiRecipes.class)
public class EmiRecipeOrderMixin {
    @Definition(id = "recipes", field = "Ldev/emi/emi/registry/EmiRecipes;recipes:Ljava/util/List;")
    @Definition(id = "stream", method = "Ljava/util/List;stream()Ljava/util/stream/Stream;")
    @Expression("recipes.stream()")
    @WrapOperation(
        method = "bake",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private static Stream<EmiRecipe> prependRecipes(List<EmiRecipe> instance, Operation<Stream<EmiRecipe>> original) {
        return Stream.concat(DriftwardEmi.prependRecipes.stream(), original.call(instance));
    }
}
