package ink.astrius.driftward.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.simulated_team.simulated.content.end_sea.EndSeaRenderer;
import ink.astrius.driftward.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndSeaRenderer.class)
public class EndSeaRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void cancelRendering(Camera camera, GameRenderer gameRenderer, CallbackInfo ci) {
        if (ClientConfig.getEndSeaLayers() == 0) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderLayers", at = @At(value = "CONSTANT", args = "intValue=48"))
    private static int setLayerCount(int original) {
        return ClientConfig.getEndSeaLayers();
    }
}
