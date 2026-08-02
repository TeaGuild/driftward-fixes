package ink.astrius.driftwardfixes.mixin.cobblegen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.LavaFluid;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LavaFluid.class)
public class LavaFluidMixin {
    @ModifyExpressionValue(
        method = "spreadTo",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;STONE:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC
        )
    )
    public Block generateDeepslate(Block original) {
        return Blocks.DEEPSLATE;
    }
}
