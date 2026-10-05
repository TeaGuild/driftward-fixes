package ink.astrius.driftward.mixin.modonomicon;

import com.klikli_dev.modonomicon.client.gui.book.index.BookCategoryIndexOnNodeScreen;
import com.klikli_dev.modonomicon.client.gui.book.index.BookCategoryIndexScreen;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ink.astrius.driftward.modonomicon.IScaleFactor;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BookCategoryIndexScreen.class)
public class BookCategoryIndexScreenMixin {
    @WrapMethod(method = "render")
    public void scaleRender(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick, Operation<Void> original) {
        if (!((Object) this instanceof BookCategoryIndexOnNodeScreen)) {
            guiGraphics.pose().pushPose();
            final var scaleFactor = ((IScaleFactor) this).driftward$getScaleFactor();
            guiGraphics.pose().scale(scaleFactor, scaleFactor, scaleFactor);
            original.call(guiGraphics, (int) (pMouseX / scaleFactor), (int) (pMouseY / scaleFactor), pPartialTick);
            guiGraphics.pose().popPose();
        } else {
            // Called by child class that overrides render, coords are already scaled by other mixin
            original.call(guiGraphics, pMouseX, pMouseY, pPartialTick);
        }
    }
}
