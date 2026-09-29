package ink.astrius.driftward.mixin.crittersandcompanions;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.bonsaistudi0s.crittersandcompanions.common.item.GrapplingHookItem;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("crittersandcompanions"))
@Mixin(GrapplingHookItem.class)
public abstract class GrapplingHookItemMixin extends Item {
    public GrapplingHookItemMixin(Item.Properties properties) {
        super(properties);
        throw new AssertionError();
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment)
            || enchantment.is(Enchantments.POWER)
            || enchantment.is(Enchantments.FROST_WALKER)
            || enchantment.is(Enchantments.FEATHER_FALLING);
    }

    @ModifyArg(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lio/github/bonsaistudi0s/crittersandcompanions/common/entity/GrapplingHookEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private Vec3 scaleMovementWithPower(
        Vec3 original,
        @Local(argsOnly = true) Level level,
        @Local ItemStack stack
    ) {
        var enchantment = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.POWER);
        var powerLevel = stack.getEnchantmentLevel(enchantment);
        return original.scale(1.0 + (double)powerLevel / 2.0);
    }
}
