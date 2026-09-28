package ink.astrius.driftward.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tech.thatgravyboat.creeperoverhaul.common.block.TinyCactusBlock;

@Mixin(TinyCactusBlock.class)
public abstract class GrowTinyCactusMixin implements BonemealableBlock {
    @Override
    @Unique
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return Blocks.CACTUS.defaultBlockState().canSurvive(level, pos);
    }

    @Override
    @Unique
    public boolean isBonemealSuccess(Level level, RandomSource rng, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    @Unique
    public void performBonemeal(ServerLevel level, RandomSource rng, BlockPos pos, BlockState state) {
        level.setBlock(pos, Blocks.CACTUS.defaultBlockState(), Block.UPDATE_ALL);
    }
}
