package ink.astrius.driftward.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ink.astrius.driftward.IDriftwardLivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SweetBerryBushBlock.class)
public class SweetBerryArmorMixin {
    @WrapOperation(
        method = "entityInside",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    public boolean dontDamageWhenArmored(
        Entity instance,
        DamageSource source,
        float amount,
        Operation<Boolean> original,
        @Local(argsOnly = true) Level level
    ) {
        if (!(instance instanceof Player player)) {
            return original.call(instance, source, amount);
        }

        final var feetItem = player.getItemBySlot(EquipmentSlot.FEET).getItem();
        final var legsItem = player.getItemBySlot(EquipmentSlot.LEGS).getItem();
        if (!(feetItem instanceof ArmorItem feetArmor && legsItem instanceof ArmorItem legsArmor)) {
            return original.call(instance, source, amount);
        }

        // At this point we are ignoring player damage completely

        final var hurtFeet = feetArmor.getMaterial().is(ArmorMaterials.LEATHER);
        final var hurtLegs = legsArmor.getMaterial().is(ArmorMaterials.LEATHER);
        if (!hurtFeet && !hurtLegs) {
            return true;
        }

        final var slots = hurtFeet
            ?
            hurtLegs
                ? new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS}
                : new EquipmentSlot[]{EquipmentSlot.FEET}
            : new EquipmentSlot[]{EquipmentSlot.LEGS};
        final var damaged = ((IDriftwardLivingEntity) player).driftward$hurtEquipmentIframes(
            20,
            source,
            1.0F,
            slots
        );
        if (damaged) {
            // Not using playHurtSound because I think it's supposed
            // to be ran on both sides and doesn't broadcast to source player.
            // But in original SweetBerryBushBlock::entityInside hurt() -> playHurtSound()
            // is ran on server side only and the sound works, so ???
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ((LivingEntityAccessor)player).driftward$getHurtSound(source),
                player.getSoundSource(),
                ((LivingEntityAccessor)player).driftward$getSoundVolume(),
                player.getVoicePitch()
            );
        }
        return true;
    }

}
