package ink.astrius.driftward.mixin.client.weather;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ink.astrius.driftward.climate.ClimateLocalWeather;
import ink.astrius.driftward.climate.ClimateSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Rain and snow are drawn, splash and sound only in columns where the local weather has
// precipitation and the ground is below the cloud base. Vanilla's cloud layer sits at the local cloud
// base, thins out under clear skies and darkens under storms.
@Mixin(LevelRenderer.class)
public class LevelRendererWeatherMixin {
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void clearSky(CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.dimension() == Level.OVERWORLD && ClimateLocalWeather.active(level)
            && ClimateSync.Grid.here(2) < 0.15f) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = {"renderSnowAndRain", "tickRain"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
        )
    )
    private Biome.Precipitation localPrecipitation(Biome biome, BlockPos pos, Operation<Biome.Precipitation> original) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level != null && ClimateLocalWeather.active(level)) {
            // Columns whose ground reaches above the cloud base stay dry; the rest get rain or snow
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            if (ClimateLocalWeather.dry(level, pos) || ground > ClimateSync.Grid.here(4)) {
                return Biome.Precipitation.NONE;
            }
        }
        return original.call(biome, pos);
    }
}
