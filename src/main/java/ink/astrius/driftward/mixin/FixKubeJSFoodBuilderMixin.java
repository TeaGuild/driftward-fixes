package ink.astrius.driftward.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.latvian.mods.kubejs.item.FoodBuilder;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.food.FoodProperties;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("kubejs"))
@Mixin(FoodBuilder.class)
public class FixKubeJSFoodBuilderMixin {
    @ModifyExpressionValue(
        method = "Ldev/latvian/mods/kubejs/item/FoodBuilder;<init>(Lnet/minecraft/world/food/FoodProperties;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodProperties;saturation()F"
        )
    )
    private float fixSaturationModifierConfusion(
        float original,
        @Local(argsOnly = true) FoodProperties properties
    ) {
        return properties.nutrition() > 0 ? original / (properties.nutrition() * 2.0F) : 0;
    }
}
