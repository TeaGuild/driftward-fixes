package ink.astrius.driftward.mixin.client.weather;

import ink.astrius.driftward.climate.ClimateLocalWeather;
import ink.astrius.driftward.climate.ClimateSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The Overworld's cloud layer sits at the local cloud base instead of y 192
@Mixin(DimensionSpecialEffects.class)
public class CloudHeightMixin {
    @Inject(method = "getCloudHeight", at = @At("HEAD"), cancellable = true)
    private void localCloudBase(CallbackInfoReturnable<Float> cir) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.dimension() == Level.OVERWORLD && level.effects() == (Object) this
            && ClimateLocalWeather.active(level)) {
            float base = ClimateSync.Grid.here(4);
            if (base > -1000 && base < 10000) {
                cir.setReturnValue(base);
            }
        }
    }
}
