package ink.astrius.driftward.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import ink.astrius.driftward.foliage_distances.DistancePropagator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.WorldGenLevel;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TreeFeature.class)
public class TreeFeatureMixin {
    // Vanilla has a bug in the BFS implementation and also doesn't correctly propagate updates
    // involving pre-existing logs and leaves, which causes issues for Terralith trees.
    @Inject(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;encapsulatingPositions(Ljava/lang/Iterable;)Ljava/util/Optional;"
        ),
        cancellable = true
    )
    public void updateLeaves(
        CallbackInfoReturnable<Boolean> cir,
        @Local WorldGenLevel level,
        @Local(ordinal = 1) Set<BlockPos> newLogs,
        @Local(ordinal = 2) Set<BlockPos> newLeaves
    ) {
        cir.setReturnValue(DistancePropagator.updateLeaves(level, newLogs, newLeaves));
    }
}
