package ink.astrius.driftward.mixin.modonomicon;

import com.klikli_dev.modonomicon.client.gui.book.bookmarks.BookBookmarksScreen;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryDoublePageScreen;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntrySinglePageScreen;
import com.klikli_dev.modonomicon.client.gui.book.index.BookCategoryIndexOnNodeScreen;
import com.klikli_dev.modonomicon.client.gui.book.index.BookParentIndexScreen;
import com.klikli_dev.modonomicon.client.gui.book.search.BookSearchScreen;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ink.astrius.driftward.modonomicon.IScaleFactor;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({
    BookCategoryIndexOnNodeScreen.class,
    BookBookmarksScreen.class,
    BookEntryDoublePageScreen.class,
    BookEntrySinglePageScreen.class,
    BookParentIndexScreen.class,
    BookSearchScreen.class
})
public class SimpleRenderMixin {
    @WrapMethod(method = "render")
    public void scaleRender(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick, Operation<Void> original) {
        guiGraphics.pose().pushPose();
        final var scaleFactor = ((IScaleFactor) this).driftward$getScaleFactor();
        guiGraphics.pose().scale(scaleFactor, scaleFactor, scaleFactor);
        original.call(guiGraphics, (int) (pMouseX / scaleFactor), (int) (pMouseY / scaleFactor), pPartialTick);
        guiGraphics.pose().popPose();
    }
}
