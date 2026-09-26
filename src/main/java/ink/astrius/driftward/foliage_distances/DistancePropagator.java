package ink.astrius.driftward.foliage_distances;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import static net.minecraft.world.level.block.LeavesBlock.DECAY_DISTANCE;
import static net.minecraft.world.level.block.LeavesBlock.DISTANCE;

public class DistancePropagator {
    // Vanilla has a bug in the BFS implementation and also doesn't correctly propagate updates
    // involving pre-existing logs and leaves, which causes issues for Terralith trees.
    public static boolean updateLeaves(
        LevelAccessor level,
        Set<BlockPos> newLogs,
        Set<BlockPos> newLeaves
    ) {
        var optBbox = BoundingBox.encapsulatingPositions(Iterables.concat(newLogs, newLeaves));
        if (!optBbox.isPresent()) {
            return false;
        }
        BoundingBox bbox = optBbox.get();

        // Logs and leaves can affect pre-existing blocks at a distance of at most 6 away.
        bbox = bbox.inflatedBy(DECAY_DISTANCE - 1);

        Set<BlockPos> logs = Sets.newHashSet();
        logs.addAll(newLogs);

        // Find old logs which can affect new leaves, or old leaves by adding a path via new leaves.
        Queue<BlockPosAndDistance> queue = new ArrayDeque();
        VisitedSet knownDistance = new VisitedSet(bbox);
        for (BlockPos pos : newLeaves) {
            queue.add(new BlockPosAndDistance(pos, 0));
            knownDistance.add(pos);
        }
        MutableBlockPos neighbor = new MutableBlockPos();
        BlockPosAndDistance block;
        while ((block = queue.poll()) != null) {
            for (Direction direction : Direction.values()) {
                neighbor.setWithOffset(block.pos(), direction);
                if (knownDistance.contains(neighbor)) {
                    continue;
                }
                knownDistance.add(neighbor);
                var blockState = level.getBlockState(neighbor);
                var neighborDistance = block.distance() + 1;
                if (blockState.is(BlockTags.LOGS)) {
                    logs.add(neighbor.immutable());
                } else if (
                    // A neighbor at distance `DECAY_DISTANCE - 1` can only propagate
                    // `DECAY_DISTANCE` to its own neighbors, so visiting it is useless.
                    neighborDistance < DECAY_DISTANCE - 1
                    && blockState.hasProperty(DISTANCE)
                ) {
                    queue.add(new BlockPosAndDistance(neighbor.immutable(), neighborDistance));
                }
            }
        }

        // Relax all leaves distances from the relevant logs.
        knownDistance = new VisitedSet(bbox);
        for (BlockPos pos : logs) {
            queue.add(new BlockPosAndDistance(pos, 0));
            knownDistance.add(pos);
        }
        while ((block = queue.poll()) != null) {
            for (Direction direction : Direction.values()) {
                neighbor.setWithOffset(block.pos(), direction);
                if (!bbox.isInside(neighbor)) {
                    // Old leaves outside of the bbox may be reachable from old logs, but no path
                    // crossing the bbox could have been affected, so such edges can be skipped.
                    continue;
                }
                if (knownDistance.contains(neighbor)) {
                    continue;
                }
                knownDistance.add(neighbor);
                var blockState = level.getBlockState(neighbor);
                if (!blockState.hasProperty(DISTANCE)) {
                    continue;
                }
                var neighborDistance = block.distance() + 1;
                if (blockState.getValue(DISTANCE) > neighborDistance) {
                    level.setBlock(
                        neighbor,
                        blockState.setValue(DISTANCE, neighborDistance),
                        Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE
                    );
                }
                // Even if the leaf already had `distance` set to `neighborDistance`, we still need
                // to recurse through it, because it may connect to a new leaf that doesn't have
                // a distance assigned yet. We don't need to do so if `distance < neighborDistance`,
                // because such leaves must be populated from an irrelevant log and thus be
                // irrelevant themselves, but handling this explicitly is too much work.
                if (neighborDistance < DECAY_DISTANCE - 1) {
                    queue.add(new BlockPosAndDistance(neighbor.immutable(), neighborDistance));
                }
            }
        }

        return true;
    }
}
