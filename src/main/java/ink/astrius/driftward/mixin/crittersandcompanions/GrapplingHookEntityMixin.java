package ink.astrius.driftward.mixin.crittersandcompanions;

import io.github.bonsaistudi0s.crittersandcompanions.common.entity.GrapplingHookEntity;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("crittersandcompanions"))
@Mixin(GrapplingHookEntity.class)
public abstract class GrapplingHookEntityMixin extends ThrowableItemProjectile {
    public GrapplingHookEntityMixin() {
        super(null, null);
        throw new AssertionError();
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lio/github/bonsaistudi0s/crittersandcompanions/common/entity/GrapplingHookEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
        )
    )
    private void freezeWater(CallbackInfo ci) {
        if (this.level() instanceof ServerLevel level) {
            var enchantment = this.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FROST_WALKER);
            var frostWalkerLevel = this.getItem().getEnchantmentLevel(enchantment);
            if (frostWalkerLevel > 0 && this.isInWater()) {
                // Let the enchantment itself perform the necessary checks and logic
                var airPos = this.position().add(0, 1, 0);
                for (var condEffect : enchantment.value().getEffects(EnchantmentEffectComponents.LOCATION_CHANGED)) {
                    // `ReplaceDisk` ignores `enchantedItemInUse`, so we pass `null`
                    condEffect.effect().onChangedBlock(level, frostWalkerLevel, null, this, airPos, true);
                }
            }
        }
    }

    @ModifyVariable(method = "pull", at = @At("STORE"), name = "maxSpeed")
    private double scaleMaxSpeedWithPower(double original) {
        var enchantment = this.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.POWER);
        var level = this.getItem().getEnchantmentLevel(enchantment);
        return original * (1.0 + (double)level / 2.0);
    }

    @ModifyVariable(method = "tick", at = @At("STORE"), name = "maxDistance")
    private double scaleMaxDistanceWithPower(double original) {
        var enchantment = this.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.POWER);
        var level = this.getItem().getEnchantmentLevel(enchantment);
        return original * (1.0 + (double)level / 2.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0;
    }

    @Inject(
        method = "pull",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void applyFeatherFalling(CallbackInfo ci) {
        if (this.getOwner() instanceof Player player) {
            // Save info to reduce fall damage caused by climbing with a hook. Doing this even on
            // downwards movement technically enables a "grappling hook clutch", which I'm not
            // positive about, but it's not game-breaking so might as well allow it.
            player.currentImpulseImpactPos = this.position();
            player.currentExplosionCause = this;
            // Deliberately skip setIgnoreFallDamageFromCurrentImpulse
        }
    }
}
