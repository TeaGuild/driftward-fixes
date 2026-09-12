package ink.astrius.driftward.mixin.scorchful;

import com.github.thedeathlycow.scorchful.attachment.EntityDesertVisionAttachment;
import com.llamalad7.mixinextras.sugar.Local;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Restriction(require = @Condition("scorchful"))
@Mixin(EntityDesertVisionAttachment.SyncHandler.class)
public class EntityDesertVisionAttachmentSyncHandlerMixin {
    @Redirect(
        method = "read(Lnet/neoforged/neoforge/attachment/IAttachmentHolder;Lnet/minecraft/network/RegistryFriendlyByteBuf;Lcom/github/thedeathlycow/scorchful/attachment/EntityDesertVisionAttachment;)Lcom/github/thedeathlycow/scorchful/attachment/EntityDesertVisionAttachment;",
        at = @At(
            value = "FIELD",
            target = "Lcom/github/thedeathlycow/scorchful/attachment/EntityDesertVisionAttachment;provider:Lnet/minecraft/world/entity/Entity;",
            opcode = Opcodes.GETFIELD
        )
    )
    public Entity fixProvider(
        EntityDesertVisionAttachment instance,
        @Local(argsOnly = true) IAttachmentHolder holder
    ) {
        return (Entity) holder;
    }

}
