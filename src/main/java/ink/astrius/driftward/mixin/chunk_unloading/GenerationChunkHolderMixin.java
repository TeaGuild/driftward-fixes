package ink.astrius.driftward.mixin.chunk_unloading;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.GenerationChunkHolder;
import org.spongepowered.asm.mixin.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("OverwriteAuthorRequired")
@Mixin(GenerationChunkHolder.class)
public class GenerationChunkHolderMixin {
    @Shadow
    @Final
    private AtomicInteger generationRefCount;

    @Unique
    private volatile CompletableFuture<Void> driftward$generationSaveSyncFuture = CompletableFuture.completedFuture(null);

    @Overwrite
    public int getGenerationRefCount() {
        return 0;
    }

    @Overwrite
    public void increaseGenerationRefCount() {
        if (this.generationRefCount.getAndIncrement() == 0) {
            this.driftward$generationSaveSyncFuture = new CompletableFuture<>();
            ((ChunkHolder) (Object) this).addSaveDependency(this.driftward$generationSaveSyncFuture);
        }
    }

    @Overwrite
    public void decreaseGenerationRefCount() {
        final var completableFuture = this.driftward$generationSaveSyncFuture;
        int i = this.generationRefCount.decrementAndGet();
        if (i == 0) {
            completableFuture.complete(null);
        }
        if (i < 0) {
            throw new IllegalStateException("More releases than claims. Count: " + i);
        }
    }
}
