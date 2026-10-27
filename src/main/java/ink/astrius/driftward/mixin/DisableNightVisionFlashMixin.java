package ink.astrius.driftward.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

// For accessibility
@Mixin(GameRenderer.class)
public class DisableNightVisionFlashMixin {
    @Overwrite
    public static float getNightVisionScale(LivingEntity entity, float f) {
        var effect = entity.getEffect(MobEffects.NIGHT_VISION);
        if (effect.isInfiniteDuration()) {
            return 1.0F;
        }
        return Math.min(effect.getDuration() / 200.0F, 1.0F);
    }
}
