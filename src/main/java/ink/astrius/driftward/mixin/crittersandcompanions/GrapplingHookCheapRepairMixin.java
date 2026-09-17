package ink.astrius.driftward.mixin.crittersandcompanions;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bonsaistudi0s.crittersandcompanions.common.registry.CACItems;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Restriction(require = @Condition("crittersandcompanions"))
@Mixin(AnvilMenu.class)
public class GrapplingHookCheapRepairMixin {
    @ModifyExpressionValue(
        method = "createResult",
        at = @At(
            value = "CONSTANT",
            args = "intValue=4"
        )
    )
    public int cheaperHookRepair(int original, @Local(ordinal = 0) ItemStack itemStack) {
        if (itemStack.is(CACItems.GRAPPLING_HOOK)) {
            return 2;
        } else {
            return original;
        }
    }
}
