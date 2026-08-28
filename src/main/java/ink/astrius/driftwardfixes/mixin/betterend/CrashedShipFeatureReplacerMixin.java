package ink.astrius.driftwardfixes.mixin.betterend;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static ink.astrius.driftwardfixes.DriftwardFixes.REMOVE_FROM_CRASHED_SHIP;

@Mixin(targets = "org.betterx.betterend.world.features.CrashedShipFeature$1")
public class CrashedShipFeatureReplacerMixin {
    @SuppressWarnings("OverwriteAuthorRequired")
    @Overwrite
    public StructureTemplate.StructureBlockInfo processBlock(
        LevelReader worldView,
        BlockPos pos,
        BlockPos blockPos,
        StructureTemplate.StructureBlockInfo structureBlockInfo,
        StructureTemplate.StructureBlockInfo structureBlockInfo2,
        StructurePlaceSettings structurePlacementData
    ) {
        BlockState state = structureBlockInfo2.state();
        if (state.is(REMOVE_FROM_CRASHED_SHIP)) {
            return new StructureTemplate.StructureBlockInfo(structureBlockInfo2.pos(), DefaultFeature.AIR, null);
        }
        return structureBlockInfo2;
    }
}
