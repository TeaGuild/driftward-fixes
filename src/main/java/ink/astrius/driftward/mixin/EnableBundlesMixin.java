package ink.astrius.driftward.mixin;

import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreativeModeTabs.class)
public class EnableBundlesMixin {
    @Redirect(
        method = "lambda$bootstrap$17",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/flag/FeatureFlagSet;contains(Lnet/minecraft/world/flag/FeatureFlag;)Z"
        )
    )
    private static boolean alwaysEnableBundles(FeatureFlagSet instance, FeatureFlag flag) {
        return true;
    }
}
