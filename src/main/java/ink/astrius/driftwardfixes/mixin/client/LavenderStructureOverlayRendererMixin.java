package ink.astrius.driftwardfixes.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.wispforest.lavender.client.StructureOverlayRenderer;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Restriction(require = @Condition("lavender"))
@Mixin(StructureOverlayRenderer.class)
public class LavenderStructureOverlayRendererMixin {
    @Shadow
    @Final
    private static Map<BlockPos, StructureOverlayRenderer.OverlayEntry> ACTIVE_OVERLAYS;

    @Shadow
    @Nullable
    private static StructureOverlayRenderer.OverlayEntry PENDING_OVERLAY;

    @Inject(
        method = "lambda$static$1",
        at = @At("RETURN")
    )
    private static void enableStencil(CallbackInfoReturnable<RenderTarget> cir) {
        // Fixes "GL_INVALID_OPERATION" log spam when Supplementaries
        // (or any other mod enabling stencil) is installed
        // If stencil is not already enabled, this will have an opposite effect
        cir.getReturnValue().enableStencil();
    }

    @Inject(
        method = "lambda$initialize$9",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void earlyQuitRendering(WorldRenderContext context, CallbackInfo ci) {
        if (ACTIVE_OVERLAYS.isEmpty() && PENDING_OVERLAY == null) {
            ci.cancel();
        }
    }
}
