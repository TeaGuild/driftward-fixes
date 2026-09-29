package ink.astrius.driftward.mixin.crittersandcompanions;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.llamalad7.mixinextras.sugar.Share;
import io.github.bonsaistudi0s.crittersandcompanions.common.entity.GrapplingHookEntity;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Restriction(require = @Condition("crittersandcompanions"))
@Mixin(Player.class)
public abstract class ScalePlayerFallDamageMixin extends LivingEntity {
    @Shadow
    @Nullable
    public Vec3 currentImpulseImpactPos;

    @Shadow
    @Nullable
    public Entity currentExplosionCause;

    public ScalePlayerFallDamageMixin() {
        super(null, null);
        throw new AssertionError();
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"))
    private void saveExplosionCauseBeforeReset(
        CallbackInfoReturnable<Boolean> cir,
        @Share("savedExplosionCause") LocalRef<Entity> savedExplosionCause
    ) {
        savedExplosionCause.set(this.currentExplosionCause);
    }

    // Apply reduction to the multiplier directly so that it stacks with Feather Falling on boots
    @ModifyArg(
        method = "causeFallDamage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z",
            ordinal = 1
        ),
        index = 0
    )
    private float adjustFallDamage(
        float height,
        @Share("savedExplosionCause") LocalRef<Entity> savedExplosionCause
    ) {
        if (
            savedExplosionCause.get() instanceof GrapplingHookEntity hookEntity
            && this.currentImpulseImpactPos.y < this.getY() + height
        ) {
            // Apply reduction to falling down to the hook level, but not further down
            var reducibleHeight = Math.min((float)(this.getY() + height - this.currentImpulseImpactPos.y), height);
            var fixedHeight = height - reducibleHeight;
            var enchantment = hookEntity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FEATHER_FALLING);
            var level = hookEntity.getItem().getEnchantmentLevel(enchantment);
            var reducedHeight = CombatRules.getDamageAfterMagicAbsorb(reducibleHeight, 3.0F * (float)level);
            return reducedHeight + fixedHeight;
        }
        return height;
    }
}
