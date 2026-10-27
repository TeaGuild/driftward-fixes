package ink.astrius.driftward.mixin.debug;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import net.minecraft.ReportedException;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// "Accessing LegacyRandomSource from multiple threads" only reports the thread that noticed the
// conflict, not the one that caused it. With -Ddriftward.debugRandomThreads=true, remember the last
// thread that used each source and log its stack when the conflict is detected.
@Mixin(LegacyRandomSource.class)
public class LegacyRandomThreadsMixin {
    @Unique
    private static final boolean DRIFTWARD$ENABLED = Boolean.getBoolean("driftward.debugRandomThreads");
    @Unique
    private static final Logger DRIFTWARD$LOGGER = LogUtils.getLogger();

    @Unique
    private volatile Thread driftward$lastThread;

    @Inject(method = "next", at = @At("HEAD"))
    private void driftward$rememberThread(int bits, CallbackInfoReturnable<Integer> cir) {
        if (DRIFTWARD$ENABLED) {
            driftward$lastThread = Thread.currentThread();
        }
    }

    @Inject(method = "setSeed", at = @At("HEAD"))
    private void driftward$rememberThreadOnSeed(long seed, CallbackInfo ci) {
        if (DRIFTWARD$ENABLED) {
            driftward$lastThread = Thread.currentThread();
        }
    }

    @WrapOperation(
        method = {"next", "setSeed"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ThreadingDetector;makeThreadingException(Ljava/lang/String;Ljava/lang/Thread;)Lnet/minecraft/ReportedException;")
    )
    private ReportedException driftward$reportOtherThread(String name, Thread thread, Operation<ReportedException> original) {
        final var other = driftward$lastThread;
        if (DRIFTWARD$ENABLED && other != null && other != Thread.currentThread()) {
            final var trace = new StringBuilder();
            for (final var element : other.getStackTrace()) {
                trace.append("\n\tat ").append(element);
            }
            DRIFTWARD$LOGGER.error("{} was also used by thread {}:{}", name, other.getName(), trace);
        }
        return original.call(name, thread);
    }
}
