package ink.astrius.driftward.mixin.client;

import com.mojang.blaze3d.platform.GlDebug;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlDebug.class)
public class GlStacktracesMixin1 {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(
        method = "printDebugLog",
        at = @At("TAIL")
    )
    private static void meow(int source, int type, int id, int severity, int messageLength, long message, long userParam, CallbackInfo ci) {
        StringBuilder sb = new StringBuilder();
        for (var el : Thread.currentThread().getStackTrace()) {
            sb.append(el);
            sb.append('\n');
        }
        LOGGER.info("Trace: {}", sb);
    }
}
