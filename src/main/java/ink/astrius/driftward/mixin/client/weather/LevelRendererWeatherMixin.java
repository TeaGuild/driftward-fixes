package ink.astrius.driftward.mixin.client.weather;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ink.astrius.driftward.climate.ClimateLocalWeather;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Rain and snow are drawn, splash and sound only in columns where the local weather has precipitation
@Mixin(LevelRenderer.class)
public class LevelRendererWeatherMixin {
    @WrapOperation(
        method = {"renderSnowAndRain", "tickRain"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
        )
    )
    private Biome.Precipitation localPrecipitation(Biome biome, BlockPos pos, Operation<Biome.Precipitation> original) {
        var level = Minecraft.getInstance().level;
        if (level != null && ClimateLocalWeather.active(level) && ClimateLocalWeather.dry(level, pos)) {
            return Biome.Precipitation.NONE;
        }
        return original.call(biome, pos);
    }
}
