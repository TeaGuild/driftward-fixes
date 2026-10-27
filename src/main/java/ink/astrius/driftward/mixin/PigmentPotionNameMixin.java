package ink.astrius.driftward.mixin;

import ink.astrius.driftward.potions.PigmentPotionNames;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TippedArrowItem;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("spectrum"))
@Mixin({PotionItem.class, TippedArrowItem.class})
public abstract class PigmentPotionNameMixin extends Item {
    public PigmentPotionNameMixin() {
        super(null);
    }

    @Inject(method = "getDescriptionId", at = @At("HEAD"), cancellable = true)
    private void customizePigmentPotionName(ItemStack stack, CallbackInfoReturnable<String> cir) {
        PigmentPotionNames.descriptionId(stack, this.getDescriptionId() + ".effect.")
            .ifPresent(cir::setReturnValue);
    }
}
