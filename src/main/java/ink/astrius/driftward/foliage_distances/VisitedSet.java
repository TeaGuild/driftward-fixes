package ink.astrius.driftward.foliage_distances;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class VisitedSet {
    private BoundingBox bbox;
    private DiscreteVoxelShape data;

    VisitedSet(BoundingBox bbox) {
        this.bbox = bbox;
        this.data = new BitSetDiscreteVoxelShape(bbox.getXSpan(), bbox.getYSpan(), bbox.getZSpan());
    }

    public boolean contains(BlockPos pos) {
        return this.data.isFull(pos.getX() - this.bbox.minX(), pos.getY() - this.bbox.minY(), pos.getZ() - this.bbox.minZ());
    }

    public void add(BlockPos pos) {
        this.data.fill(pos.getX() - this.bbox.minX(), pos.getY() - this.bbox.minY(), pos.getZ() - this.bbox.minZ());
    }
}
