package ink.astrius.driftward.mixin.sereneseasons;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ink.astrius.driftward.climate.ClimateConstants;
import ink.astrius.driftward.climate.ClimatePrecipitation;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.season.RandomUpdateHandler;

// Serene Seasons melts snow and ice only in seasons whose config allows it (never in winter) and
// judges ice by the air. With the climate model, melting runs all year at one rate and the
// temperature alone decides; ice goes by the water under it.
@Restriction(require = @Condition("sereneseasons"))
@Mixin(value = RandomUpdateHandler.class, remap = false)
public class RandomUpdateHandlerMixin {
    @WrapOperation(
        method = "onWorldTick",
        at = @At(value = "INVOKE", target = "Lsereneseasons/config/SeasonsConfig$SeasonProperties;meltChance()F")
    )
    private static float climateMeltChance(
        SeasonsConfig.SeasonProperties properties, Operation<Float> original, @Local ServerLevel level
    ) {
        return ClimatePrecipitation.applies(level) ? (float) ClimateConstants.get().meltPercent() : original.call(properties);
    }

    @WrapOperation(
        method = "onWorldTick",
        at = @At(value = "INVOKE", target = "Lsereneseasons/config/SeasonsConfig$SeasonProperties;meltRolls()I")
    )
    private static int climateMeltRolls(
        SeasonsConfig.SeasonProperties properties, Operation<Integer> original, @Local ServerLevel level
    ) {
        return ClimatePrecipitation.applies(level) ? ClimateConstants.get().meltRolls() : original.call(properties);
    }

    @WrapOperation(
        method = "meltInChunk",
        at = @At(
            value = "INVOKE",
            target = "Lsereneseasons/season/SeasonHooks;getBiomeTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)F",
            ordinal = 1
        )
    )
    private static float iceByWater(Level level, Holder<Biome> biome, BlockPos pos, Operation<Float> original) {
        double temperature = ClimatePrecipitation.iceVanillaTemperature(level, biome, pos);
        return Double.isNaN(temperature) ? original.call(level, biome, pos) : (float) temperature;
    }
}
