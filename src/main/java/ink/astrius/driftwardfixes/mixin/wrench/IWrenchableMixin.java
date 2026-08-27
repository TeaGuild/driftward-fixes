package ink.astrius.driftwardfixes.mixin.wrench;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.mehvahdjukaar.supplementaries.common.items.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static ink.astrius.driftwardfixes.DriftwardFixes.ROTATED_WITH_WRENCH;

@Restriction(
    require = {
        @Condition("create"),
        @Condition("supplementaries")
    }
)
@Mixin(IWrenchable.class)
public interface IWrenchableMixin {
    @Inject(
        method = "onWrenched",
        at = @At("TAIL")
    )
    default void onSuccess(BlockState state, UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.SUCCESS && (context.getPlayer() instanceof ServerPlayer player)) {
            ROTATED_WITH_WRENCH.get().trigger(player, context.getClickedPos(), context.getItemInHand());
        }
        WrenchItem.playTurningEffects(
            context.getClickedPos(),
            false,
            context.getClickedFace(),
            context.getLevel(),
            context.getPlayer()
        );
    }

    @Redirect(
        method = "onWrenched",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/equipment/wrench/IWrenchable;playRotateSound(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    default void disableCreateSounds(Level level, BlockPos pos) {
        // seems like `level.getBlockState(pos) != state` <=> `level instanceof ServerLevel`
        // in this case. not sure if it's true but whatever
    }
}
