package ink.astrius.driftwardfixes.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class GlStacktracesMixin2 {
    @Shadow
    @Final
    public Options options;

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V"
        )
    )
    public void meow(int debugVerbosity, boolean synchronous) {
        RenderSystem.initRenderer(this.options.glDebugVerbosity, true);
    }
}
