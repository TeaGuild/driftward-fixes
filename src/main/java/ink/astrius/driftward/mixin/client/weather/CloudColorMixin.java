package ink.astrius.driftward.mixin.client.weather;

import ink.astrius.driftward.climate.ClimateLocalWeather;
import ink.astrius.driftward.climate.ClimateSync;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Thick and stormy cloud is darker
@Mixin(ClientLevel.class)
public class CloudColorMixin {
    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void stormClouds(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        var level = (ClientLevel) (Object) this;
        if (level.dimension() != Level.OVERWORLD || !ClimateLocalWeather.active(level)) {
            return;
        }
        float cloud = ClimateSync.Grid.here(2);
        float storm = ClimateSync.Grid.here(1);
        double dark = 0.35 * Mth.clamp((cloud - 0.6f) / 0.4f, 0, 1) + 0.25 * storm;
        cir.setReturnValue(cir.getReturnValue().scale(1 - Mth.clamp(dark, 0, 0.6)));
    }
}
