package ink.astrius.driftward.mixin.weather;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ink.astrius.driftward.climate.ClimateLocalWeather;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Lightning only strikes under local thunderstorms, not wherever it rains while a storm is somewhere
@Mixin(ServerLevel.class)
public class ServerLevelLightningMixin {
    @WrapOperation(
        method = "tickChunk",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isThundering()Z")
    )
    private boolean localThunder(ServerLevel level, Operation<Boolean> original, @Local(argsOnly = true) LevelChunk chunk) {
        if (!original.call(level)) {
            return false;
        }
        return !ClimateLocalWeather.active(level)
            || ClimateLocalWeather.thundering(level, chunk.getPos().getMiddleBlockPosition(level.getSeaLevel()));
    }
}
