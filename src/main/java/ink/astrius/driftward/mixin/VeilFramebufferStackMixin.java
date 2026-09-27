package ink.astrius.driftward.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.api.client.render.framebuffer.FramebufferStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL30C.*;

@Mixin(FramebufferStack.class)
public class VeilFramebufferStackMixin {
    @Shadow
    @Final
    private static List<Object> STATE_STACK;

    @Shadow
    private static ResourceLocation lastPop;

    @Unique
    private static final Constructor<?> DRIFTWARD$STATE_CTOR;

    static {
        try {
            final var cls = Class.forName("foundry.veil.api.client.render.framebuffer.FramebufferStack$State");
            final var ctor = cls.getDeclaredConstructor(Vector4i.class, int.class, int.class, int.class, ResourceLocation.class);
            ctor.setAccessible(true);
            DRIFTWARD$STATE_CTOR = ctor;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(
        method = "push",
        at = @At(
            value = "NEW",
            target = "()Lorg/joml/Vector4i;"
        ),
        cancellable = true
    )
    private static void cachedGetViewport(ResourceLocation name, CallbackInfo ci) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        // Get viewport from cache instead of asking the driver
        Vector4i viewport = new Vector4i(
            GlStateManager.Viewport.x(),
            GlStateManager.Viewport.y(),
            GlStateManager.Viewport.width(),
            GlStateManager.Viewport.height()
        );
        STATE_STACK.add(DRIFTWARD$STATE_CTOR.newInstance(
            viewport,
            glGetInteger(GL_FRAMEBUFFER_BINDING),
            glGetInteger(GL_READ_FRAMEBUFFER_BINDING),
            glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),
            name
        ));
        lastPop = null;
        ci.cancel();
    }
}
