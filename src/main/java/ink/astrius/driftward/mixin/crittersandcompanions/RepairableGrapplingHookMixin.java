package ink.astrius.driftward.mixin.crittersandcompanions;

import io.github.bonsaistudi0s.crittersandcompanions.common.item.GrapplingHookItem;
import io.github.bonsaistudi0s.crittersandcompanions.common.registry.CACItems;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("crittersandcompanions"))
@Mixin(GrapplingHookItem.class)
public class RepairableGrapplingHookMixin {
    // There must be no @Unique here, it's an override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(CACItems.SILK.get());
    }
}
