package ink.astrius.driftwardfixes.mixin.rope;

import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.registry.ModItems;

@Restriction(
    require = {
        @Condition("simulated"),
        @Condition("farmersdelight")
    }
)
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
