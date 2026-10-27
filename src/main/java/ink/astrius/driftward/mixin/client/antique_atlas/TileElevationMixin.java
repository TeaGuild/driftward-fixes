package ink.astrius.driftward.mixin.client.antique_atlas;

import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Antique Atlas picks lowland tiles (valley/low/mid/high/peak) by the surface height above sea level
// with thresholds tuned for vanilla: 10/20/35/50 blocks. With Terralith and Tectonic most raised
// lowland draws as hills. Rescale the height so the bands become 10/40/100/180 blocks instead.
// The method takes and returns no Minecraft types, so it needs no remapping under Connector.
@Restriction(require = @Condition("antique_atlas"))
@Mixin(targets = "folk.sisby.antique_atlas.TileElevation", remap = false)
public abstract class TileElevationMixin {
    @ModifyVariable(method = "fromBlocksAboveSea", at = @At("HEAD"), argsOnly = true, remap = false)
    private static int driftward$rescaleForTallTerrain(int height) {
        if (height < 10) {
            return height;
        }
        if (height < 40) {
            return 10 + (height - 10) / 3;
        }
        if (height < 100) {
            return 20 + (height - 40) / 4;
        }
        if (height < 180) {
            return 35 + (height - 100) / 6;
        }
        return 50;
    }
}
