package ink.astrius.driftwardfixes.mixin.betterend;

import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.betterx.bclib.util.StructureErode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static ink.astrius.driftwardfixes.DriftwardTags.ERODE;

@Restriction(require = @Condition("betterend"))
@Mixin(StructureErode.class)
public class StructureErodeMixin {
    @Inject(method = "ignore", at = @At("HEAD"), cancellable = true)
    private static void ignore(
        BlockState state,
        WorldGenLevel world,
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (state.is(ERODE)) {
            cir.setReturnValue(false);
        }
    }
}
