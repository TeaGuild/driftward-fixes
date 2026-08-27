package ink.astrius.driftwardfixes.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Restriction(require = @Condition("create"))
@Mixin(ContraptionCollider.class)
public class ContraptionColliderMixin {
    @ModifyExpressionValue(
        method = "handleDamageFromTrain",
        at = @At(
            value = "INVOKE",
            target = "Lnet/createmod/catnip/config/ConfigBase$ConfigBool;get()Ljava/lang/Object;"
        )
    )
    private static Object damageHostileWhenDamageDisabled(
        Object original,
        @Local(argsOnly = true) Entity entity
    ) {
        final var orig = (boolean) original;
        final var isMonster = entity.getClassification(false) == MobCategory.MONSTER;
        return orig || isMonster;
    }
}
