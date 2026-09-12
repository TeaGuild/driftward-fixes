package ink.astrius.driftward.mixin.frostiful;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Restriction(require = @Condition("frostiful"))
@Mixin(value = FallingBlockEntity.class, priority = 1500)
public class FallingIcicleMixinMixin {
    @TargetHandler(
        mixin = "com.github.thedeathlycow.frostiful.mixins.entity.FallingIcicleMixin",
        name = "lambda$freezeVictimsOnFall$0"
    )
    @ModifyExpressionValue(
        method = "@MixinSquared:Handler",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/thedeathlycow/frostiful/config/group/IcicleConfigGroup;getIcicleCollisionFreezeAmount()I"
        )
    )
    private static int negateIcicleFreezeAmount(int original) {
        return -original;
    }
}
