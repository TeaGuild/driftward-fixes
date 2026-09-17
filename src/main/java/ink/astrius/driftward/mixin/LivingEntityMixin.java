package ink.astrius.driftward.mixin;

import ink.astrius.driftward.IDriftwardLivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IDriftwardLivingEntity {
    @Shadow
    protected abstract void doHurtEquipment(DamageSource damageSource, float damageAmount, EquipmentSlot... slots);

    @Unique
    public int armorInvulnerableTime = 0;

    @Inject(method = "baseTick", at = @At("HEAD"))
    public void tickArmorIframes(CallbackInfo ci) {
        if (armorInvulnerableTime > 0) {
            armorInvulnerableTime--;
        }
    }

    @Override
    @Unique
    public boolean driftward$hurtEquipmentIframes(int iframeCount, DamageSource damageSource, float damageAmount, EquipmentSlot... slots) {
        if (armorInvulnerableTime > 0) {
            return false;
        }
        this.doHurtEquipment(damageSource, damageAmount, slots);
        armorInvulnerableTime = iframeCount;
        return true;
    }
}
