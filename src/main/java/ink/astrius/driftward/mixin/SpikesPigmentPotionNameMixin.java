package ink.astrius.driftward.mixin;

import ink.astrius.driftward.potions.PigmentPotionNames;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.mehvahdjukaar.supplementaries.common.items.BambooSpikesTippedItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(
    require = {
        @Condition("spectrum"),
        @Condition("supplementaries")
    }
)
@Mixin(BambooSpikesTippedItem.class)
public class SpikesPigmentPotionNameMixin {
    @Inject(method = "getDescriptionId(Lnet/minecraft/world/item/ItemStack;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void customizePigmentPotionName(ItemStack stack, CallbackInfoReturnable<String> cir) {
        // Matches the prefix Supplementaries itself uses for tipped spikes
        PigmentPotionNames.descriptionId(stack, "item.supplementaries.bamboo_spikes_tipped.effect.")
            .ifPresent(cir::setReturnValue);
    }
}
