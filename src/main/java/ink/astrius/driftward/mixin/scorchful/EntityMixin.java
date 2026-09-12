package ink.astrius.driftward.mixin.scorchful;

import com.github.thedeathlycow.scorchful.attachment.EntityDesertVisionAttachment;
import com.github.thedeathlycow.scorchful.attachment.ScorchfulEntityAttachments;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Restriction(require = @Condition("scorchful"))
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract Level level();

    @Inject(
        method = "tick",
        at = @At("TAIL")
    )
    public void tickVision(CallbackInfo ci) {
        if (this.level().isClientSide) {
            return;
        }
        Entity instance = (Entity) (Object) this;
        EntityDesertVisionAttachment attachment = instance.getData(ScorchfulEntityAttachments.ENTITY_DESERT_VISION);
        if (attachment.hasDesertVision()) {
            attachment.serverTick();
        }
    }

}
