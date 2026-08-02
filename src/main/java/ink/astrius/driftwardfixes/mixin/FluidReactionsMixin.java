package ink.astrius.driftwardfixes.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.fluids.FluidReactions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidReactions.class)
public class FluidReactionsMixin {
    @ModifyExpressionValue(
        method = "handlePipeFlowCollisionFallback",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;COBBLESTONE:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static Block pipeFlowCobbledDeepslate(Block original) {
        return Blocks.COBBLED_DEEPSLATE;
    }

    @ModifyExpressionValue(
        method = "handlePipeSpillCollisionFallback",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;COBBLESTONE:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static Block pipeSpillCobbledDeepslate(Block original) {
        return Blocks.COBBLED_DEEPSLATE;
    }

    @ModifyExpressionValue(
        method = "handlePipeSpillCollisionFallback",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;STONE:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static Block pipeSpillDeepslate(Block original) {
        return Blocks.DEEPSLATE;
    }
}
