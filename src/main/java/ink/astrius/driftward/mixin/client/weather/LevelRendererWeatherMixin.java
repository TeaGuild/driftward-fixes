package ink.astrius.driftward.mixin.client.weather;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ink.astrius.driftward.client.ClimateClouds;
import ink.astrius.driftward.climate.ClimateLocalWeather;
import ink.astrius.driftward.climate.ClimateSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// Rain and snow are drawn, splash and sound only in columns where the local weather has
// precipitation and the ground is below the cloud base. The cloud layer's texture comes from the
// weather (ClimateClouds), so vanilla's own drift is off.
@Mixin(LevelRenderer.class)
public class LevelRendererWeatherMixin {
    // Our cloud texture is anchored to the world and drifts with the wind itself
    @ModifyConstant(method = "renderClouds", constant = @Constant(floatValue = 0.03f))
    private float noVanillaDrift(float drift) {
        return ClimateClouds.installed() ? 0 : drift;
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
            // Vanilla asks with the bottom of the stretch it's about to draw (the ground, or ten blocks
            // below the camera); nothing falls there if that's above the cloud base. Rain splashes and
            // sounds ask with the ground.
            if (ClimateLocalWeather.dry(level, pos) || pos.getY() > ClimateClouds.cloudBase()) {
                return Biome.Precipitation.NONE;
            }
        }
        return original.call(biome, pos);
    }
}
