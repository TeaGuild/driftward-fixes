package ink.astrius.driftward.mixin.betterend;

import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import org.betterx.betterend.world.features.NBTFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Restriction(require = @Condition("betterend"))
@Mixin(NBTFeature.class)
public abstract class NBTFeatureMixin {
    @Shadow
    protected abstract int getAverageY(WorldGenLevel world, BlockPos center);

    @SuppressWarnings("OverwriteAuthorRequired")
    @Overwrite
    protected BlockPos getGround(WorldGenLevel world, BlockPos center) {
        int y = getAverageY(world, center);
        return new BlockPos(center.getX(), y, center.getZ());
    }
}
