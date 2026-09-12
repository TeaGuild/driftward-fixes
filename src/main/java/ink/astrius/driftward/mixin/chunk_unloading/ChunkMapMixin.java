package ink.astrius.driftward.mixin.chunk_unloading;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    protected abstract void lambda$scheduleUnload$12(ChunkHolder par1, long par2);

    @Unique
    ThreadLocal<CompletableFuture<?>> driftward$currentSaveSyncFuture = new ThreadLocal<>();

    @WrapOperation(
        method = "scheduleUnload",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;thenRunAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
        )
    )
    public CompletableFuture<?> wrapLambda(
        CompletableFuture<?> instance,
        Runnable action,
        Executor executor,
        Operation<CompletableFuture<Void>> original,
        @Local(argsOnly = true) long chunkPos,
        @Local(argsOnly = true) ChunkHolder chunkHolder
    ) {
        Runnable wrapper = () -> {
            driftward$currentSaveSyncFuture.set(instance);
            this.lambda$scheduleUnload$12(chunkHolder, chunkPos);
            driftward$currentSaveSyncFuture.remove();
        };
        return original.call(instance, wrapper, executor);
    }

    @Redirect(
        method = "lambda$scheduleUnload$12",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkHolder;isReadyForSaving()Z"
        )
    )
    public boolean replaceIsReadyForSavingCheck(ChunkHolder instance, @Local(argsOnly = true) ChunkHolder chunkHolder) {
        return chunkHolder.getSaveSyncFuture() == driftward$currentSaveSyncFuture.get();
    }
}
