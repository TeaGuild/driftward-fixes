package ink.astrius.driftward.mixin.screenshots;

import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(ScreenshotViewerConfig.class)
public class ScreenshotViewerConfigMixin {
    // this makes the `screenshotFolder == thumbnailFolder` check
    // in ScreenshotThumbnailManager less correct and
    // also relies on the assumption that the working directory
    // is the instance directory but i don't care
    @Redirect(
        method = "validDirectory",
        at = @At(
            value = "INVOKE",
            target = "Ljava/io/File;isAbsolute()Z"
        )
    )
    public boolean canBeRelative(File instance) {
        return true;
    }

    // `screenshots/` doesn't exist until the first screenshot, so the default failed validation,
    // got "corrected" to itself and rewritten, and the file watcher reloaded it every 2 seconds
    @Redirect(
        method = "validDirectory",
        at = @At(
            value = "INVOKE",
            target = "Ljava/io/File;exists()Z"
        )
    )
    public boolean createIfMissing(File instance) {
        return instance.exists() || instance.mkdirs();
    }

    @Redirect(
        method = {"lambda$new$0", "lambda$new$1"},
        at = @At(
            value = "INVOKE",
            target = "Ljava/io/File;getAbsolutePath()Ljava/lang/String;"
        )
    )
    private static String doNotTheCat(File instance) {
        return instance.getPath();
    }
}
