package ink.astrius.driftward.mixin.weather;

import ink.astrius.driftward.climate.ClimateLocalWeather;
import ink.astrius.driftward.climate.ClimateSync;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// On the client, rain and thunder levels (sky darkness, rain loudness and density) follow the
// weather where the player is, not the global flag. Rain in sight and overcast skies darken it too,
// so a storm darkens the sky as it comes near.
@Mixin(Level.class)
public abstract class LevelWeatherMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void localRain(float partialTick, CallbackInfoReturnable<Float> cir) {
        var level = (Level) (Object) this;
        if (level.isClientSide() && ClimateLocalWeather.active(level)) {
            float cloud = Math.max(ClimateSync.Grid.here(2), ClimateSync.Grid.here(6));
            float overcast = 0.5f * Mth.clamp((cloud - 0.3f) / 0.55f, 0, 1);
            float rain = ClimateSync.Grid.here(0) > 0.05f ? Math.max(0.3f, ClimateSync.Grid.here(0)) : 0;
            cir.setReturnValue(Math.max(Math.max(rain, ClimateSync.Grid.here(5)), overcast));
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void localThunder(float partialTick, CallbackInfoReturnable<Float> cir) {
        var level = (Level) (Object) this;
        if (level.isClientSide() && ClimateLocalWeather.active(level)) {
            cir.setReturnValue(ClimateSync.Grid.here(1) * level.getRainLevel(partialTick));
        }
    }
}
