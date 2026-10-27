package ink.astrius.driftward.mixin.sereneseasons;

import ink.astrius.driftward.climate.ClimatePrecipitation;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sereneseasons.season.SeasonHooks;

// Every seasonal snow, ice, melt and rain-or-snow check in Serene Seasons (server and client) asks
// this method for the biome temperature; answer from the climate model instead
@Restriction(require = @Condition("sereneseasons"))
@Mixin(value = SeasonHooks.class, remap = false)
public class SeasonHooksMixin {
    @Inject(
        method = "getBiomeTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void climateTemperature(Level level, Holder<Biome> biome, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        double temperature = ClimatePrecipitation.vanillaTemperature(level, biome, pos);
        if (!Double.isNaN(temperature)) {
            cir.setReturnValue((float) temperature);
        }
    }
}
