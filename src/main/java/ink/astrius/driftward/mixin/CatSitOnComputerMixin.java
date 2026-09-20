package ink.astrius.driftward.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import dan200.computercraft.shared.computer.core.ComputerState;
import ink.astrius.driftward.Driftward;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.CatSitOnBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Restriction(require = @Condition("computercraft"))
@Mixin(CatSitOnBlockGoal.class)
public class CatSitOnComputerMixin {
    @Inject(
        method = "isValidTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
            ordinal = 0
        ),
        cancellable = true
    )
    public void sitOnComputers(
        LevelReader level,
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir,
        @Local BlockState blockstate
    ) {
        final var isComputer = blockstate.is(ModRegistry.Blocks.COMPUTER_NORMAL.get()) || blockstate.is(ModRegistry.Blocks.COMPUTER_ADVANCED.get()) || blockstate.is(ModRegistry.Blocks.COMPUTER_COMMAND.get());
        if (isComputer && blockstate.getValue(ComputerBlock.STATE) != ComputerState.OFF) {
            cir.setReturnValue(true);
        }
        final var isMonitor = blockstate.is(ModRegistry.Blocks.MONITOR_NORMAL.get()) || blockstate.is(ModRegistry.Blocks.MONITOR_ADVANCED.get());
        if (isMonitor) {
            cir.setReturnValue(true);
        }
        final var isTerminal = Driftward.TERMINAL_NORMAL != null && (blockstate.is(Driftward.TERMINAL_NORMAL) || blockstate.is(Driftward.TERMINAL_ADVANCED));
        if (isTerminal) {
            cir.setReturnValue(true);
        }
    }
}
