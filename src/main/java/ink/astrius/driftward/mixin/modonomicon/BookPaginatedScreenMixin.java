package ink.astrius.driftward.mixin.modonomicon;

import com.klikli_dev.modonomicon.client.gui.book.BookPaginatedScreen;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.gui.book.index.BookCategoryIndexOnNodeScreen;
import com.klikli_dev.modonomicon.client.gui.book.search.BookSearchScreen;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ink.astrius.driftward.config.ClientConfig;
import ink.astrius.driftward.modonomicon.IScaleFactor;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.client.InputType;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Restriction(require = @Condition("modonomicon"))
@Mixin(BookPaginatedScreen.class)
public abstract class BookPaginatedScreenMixin extends Screen implements IScaleFactor {
    @Shadow
    protected abstract void flipPage(boolean left, boolean playSound);

    @Unique
    private float driftward$scaleFactor;

    protected BookPaginatedScreenMixin() {
        super(null);
    }

    @Override
    @Unique
    public float driftward$getScaleFactor() {
        return driftward$scaleFactor;
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void initScale(CallbackInfo ci) {
        final var window = minecraft.getWindow();
        final var globalScale = window.getGuiScale();
        final var maxScale = window.calculateScale(0, minecraft.isEnforceUnicode());
        final var bookScale = Math.min(ClientConfig.getModonomiconScale(), maxScale);

        if (bookScale > 0 && bookScale != globalScale) {
            driftward$scaleFactor = (float) (bookScale / globalScale);

            window.setGuiScale(bookScale);
            width = window.getGuiScaledWidth();
            height = window.getGuiScaledHeight();
            window.setGuiScale(globalScale);
        } else {
            driftward$scaleFactor = 1;
        }
    }

    @WrapMethod(method = "mouseClicked")
    public boolean scaleClicks(double pMouseX, double pMouseY, int pButton, Operation<Boolean> original) {
        final var mouseClickedOverriden =
            (Object) this instanceof BookCategoryIndexOnNodeScreen
                || (Object) this instanceof BookEntryScreen
                || (Object) this instanceof BookSearchScreen;
        if (!mouseClickedOverriden) {
            return original.call(
                (double) (pMouseX / driftward$scaleFactor),
                (double) (pMouseY / driftward$scaleFactor),
                pButton
            );
        } else {
            // Called by child class that overrides mouseClicked, coords are already scaled by other mixin
            return original.call(pMouseX, pMouseY, pButton);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void handleArrows(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.flipPage(true, true);
            cir.setReturnValue(true);
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.flipPage(false, true);
            cir.setReturnValue(true);
        } else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            cir.setReturnValue(true);
        } else {
            return;
        }
        // Unfocus everything
        this.minecraft.setLastInputType(InputType.NONE);
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            // Unfocus everything
            this.minecraft.setLastInputType(InputType.NONE);
            return true;
        } else {
            return false;
        }
    }
}
