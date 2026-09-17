package ink.astrius.driftward.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.betterx.betterend.effects.EndStatusEffects;
import org.betterx.betterend.registry.EndEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.alexnijjar.endermanoverhaul.common.entities.base.BaseEnderman;

@Mixin(BaseEnderman.class)
public class EndermanOverhaulEndVeilMixin {
    @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
    public void applyEndVeil(Player player, CallbackInfoReturnable<Boolean> cir) {
        final var headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        if (player.hasEffect(EndStatusEffects.END_VEIL) || EnchantmentHelper.has(headItem, EndEnchantments.getEndVeilState())) {
            cir.setReturnValue(false);
        }
    }
}
