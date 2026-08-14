package ink.astrius.driftwardfixes.mixin.yungs_better_end_island;

import com.yungnickyoung.minecraft.betterendisland.world.util.WorldgenUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static ink.astrius.driftwardfixes.DriftwardFixes.END_BASE;

@Mixin(WorldgenUtils.class)
public class WorldgenUtilsMixin {
    @Redirect(
        method = {"getLowestBlockPosAt", "getSurfacePosAt"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
        )
    )
    private static boolean replaceEndStoneWithTag(BlockState instance, Block block) {
        return instance.is(END_BASE);
    }
}
