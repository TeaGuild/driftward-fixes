package ink.astrius.driftwardfixes.mixin.betterend;

import net.minecraft.core.BlockPos;
import org.betterx.betterend.world.features.CrashedShipFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrashedShipFeature.class)
public class CrashedShipFeatureMixin {
    @Redirect(
        method = "canSpawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;getX()I",
            ordinal = 1
        )
    )
    public int fixCanSpawnGetZ(BlockPos pos) {
        return pos.getZ();
    }
}
