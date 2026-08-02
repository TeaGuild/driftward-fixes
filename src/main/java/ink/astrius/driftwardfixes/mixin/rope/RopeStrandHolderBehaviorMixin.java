package ink.astrius.driftwardfixes.mixin.rope;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.registry.ModItems;

@Mixin(RopeStrandHolderBehavior.class)
public class RopeStrandHolderBehaviorMixin {
    @Redirect(
        method = "destroyRope",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;get()Ljava/lang/Object;"
        )
    )
    public Object returnFdRope(ItemEntry instance) {
        return ModItems.ROPE.get();
    }
}
