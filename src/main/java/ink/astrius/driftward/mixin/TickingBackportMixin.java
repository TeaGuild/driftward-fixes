package ink.astrius.driftward.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkCache.class)
public class TickingBackportMixin {
    @Shadow
    @Final
    public ChunkMap chunkMap;

    @ModifyExpressionValue(
        method = "tickChunks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/DistanceManager;shouldForceTicks(J)Z"
        )
    )
    public boolean saveShouldForceTicks(
        boolean original,
        @Share("shouldForceTicks") LocalBooleanRef shouldForceTicks
    ) {
        shouldForceTicks.set(original);
        return original;
    }

    @Redirect(
        method = "tickChunks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkMap;anyPlayerCloseEnoughForSpawning(Lnet/minecraft/world/level/ChunkPos;)Z"
        )
    )
    public boolean mod1(ChunkMap instance, ChunkPos chunkPos) {
        return true;
    }

    @WrapOperation(
        method = "tickChunks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;incrementInhabitedTime(J)V"
        )
    )
    public void mod2(
        LevelChunk instance,
        long l,
        Operation<Void> original,
        @Local ChunkPos chunkpos,
        @Share("doTickPart1") LocalBooleanRef doTickPart1,
        @Share("shouldForceTicks") LocalBooleanRef shouldForceTicks
    ) {
        boolean res = this.chunkMap.anyPlayerCloseEnoughForSpawning(chunkpos) || shouldForceTicks.get();
        doTickPart1.set(res);
        if (res) {
            original.call(instance, l);
        }
    }

    @WrapOperation(
        method = "tickChunks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/border/WorldBorder;isWithinBounds(Lnet/minecraft/world/level/ChunkPos;)Z"
        )
    )
    public boolean mod3(
        WorldBorder instance,
        ChunkPos chunkPos,
        Operation<Boolean> original,
        @Share("doTickPart1") LocalBooleanRef doTickPart1
    ) {
        return doTickPart1.get() && original.call(instance, chunkPos);
    }
}
