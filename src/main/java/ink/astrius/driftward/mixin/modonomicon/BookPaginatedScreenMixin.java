package ink.astrius.driftward.mixin.modonomicon;

import com.klikli_dev.modonomicon.client.gui.book.BookPaginatedScreen;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.gui.book.index.BookCategoryIndexOnNodeScreen;
import com.klikli_dev.modonomicon.client.gui.book.search.BookSearchScreen;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ink.astrius.driftward.config.ClientConfig;
import ink.astrius.driftward.modonomicon.IScaleFactor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookPaginatedScreen.class)
public class BookPaginatedScreenMixin extends Screen implements IScaleFactor {
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
}
