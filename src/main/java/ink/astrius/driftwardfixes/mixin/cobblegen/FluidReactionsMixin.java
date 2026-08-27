package ink.astrius.driftwardfixes.mixin.cobblegen;

import com.simibubi.create.content.fluids.FluidReactions;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Restriction(require = @Condition("create"))
@Mixin(FluidReactions.class)
public class FluidReactionsMixin {
    @Redirect(
        method = "handlePipeFlowCollisionFallback",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;COBBLESTONE:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static Block pipeFlowCobbledDeepslate() {
        return Blocks.COBBLED_DEEPSLATE;
    }

    @Redirect(
        method = "handlePipeSpillCollisionFallback",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;COBBLESTONE:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static Block pipeSpillCobbledDeepslate() {
        return Blocks.COBBLED_DEEPSLATE;
    }

    @Redirect(
        method = "handlePipeSpillCollisionFallback",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;STONE:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static Block pipeSpillDeepslate() {
        return Blocks.DEEPSLATE;
    }
}
