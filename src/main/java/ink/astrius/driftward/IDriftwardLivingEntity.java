package ink.astrius.driftward;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;

public interface IDriftwardLivingEntity {
    boolean driftward$hurtEquipmentIframes(int iframeCount, DamageSource damageSource, float damageAmount, EquipmentSlot... slots);
}
