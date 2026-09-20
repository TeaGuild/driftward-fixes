package ink.astrius.driftward.mixin.grindstone;

import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Restriction(require = @Condition("supplementaries"))
@Mixin(targets = {"net.minecraft.world.inventory.GrindstoneMenu$2", "net.minecraft.world.inventory.GrindstoneMenu$3"})
public class SupplementariesGrindstoneFixMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void driftward$fixSupplementaries(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Supplementaries only injects into $2, not into $3
        // https://github.com/MehVahdJukaar/Supplementaries/blob/95e712ca5f5c85a4f652f16c6676f5aa92e6a4d8/common/src/main/java/net/mehvahdjukaar/supplementaries/mixins/GrindstoneInputSlotMixin.java
        Item i = stack.getItem();
        if ((i == Items.ENCHANTED_GOLDEN_APPLE || i == ModRegistry.BOMB_BLUE_ITEM.get()) && CommonConfigs.Tweaks.APPLE_DISENCHANT.get()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
