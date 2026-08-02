package ink.astrius.driftwardfixes.mixin.rope;

import dev.simulated_team.simulated.index.SimItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.item.RopeItem;

@Mixin(BlockItem.class)
public class RopeItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    public void useOn(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (!((Object) this instanceof RopeItem)) {
            return;
        }
        final var simulatedRope = SimItems.ROPE_COUPLING.get();
        final var simulatedRes = simulatedRope.useOn(ctx);
        if (simulatedRes != InteractionResult.PASS) {
            cir.setReturnValue(simulatedRes);
        }
    }
}
