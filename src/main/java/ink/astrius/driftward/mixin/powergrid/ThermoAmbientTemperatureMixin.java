package ink.astrius.driftward.mixin.powergrid;

import com.github.thedeathlycow.thermoo.api.environment.EnvironmentLookup;
import com.github.thedeathlycow.thermoo.api.environment.component.EnvironmentComponentTypes;
import com.github.thedeathlycow.thermoo.api.environment.component.TemperatureRecordComponent;
import com.github.thedeathlycow.thermoo.api.util.TemperatureUnit;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Power Grid derives ambient temperature from the biome's base temperature once, on the first tick.
// Use Thermoo's environment instead (seasons, time of day, altitude) and keep it up to date, so
// thermometers show the real temperature and machines cool better in winter.
@Restriction(
    require = {
        @Condition("powergrid"),
        @Condition("thermoo")
    }
)
@Mixin(ThermalBehaviour.class)
public abstract class ThermoAmbientTemperatureMixin {
    @Unique
    private static final int DRIFTWARD$REFRESH_INTERVAL = 200;

    @Shadow(remap = false)
    private float cachedAmbientTemperature;

    @Unique
    private int driftward$ticksUntilRefresh = DRIFTWARD$REFRESH_INTERVAL;

    @Inject(method = "getAmbientTemperature", at = @At("HEAD"), cancellable = true, remap = false)
    private static void driftward$thermooAmbient(Level level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        final var record = EnvironmentLookup.getInstance()
            .findEnvironmentComponents(level, pos)
            .getOrDefault(EnvironmentComponentTypes.TEMPERATURE, TemperatureRecordComponent.DEFAULT);
        cir.setReturnValue((float) record.valueInUnit(TemperatureUnit.CELSIUS));
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void driftward$refreshAmbient(CallbackInfo ci) {
        if (--driftward$ticksUntilRefresh > 0) {
            return;
        }
        driftward$ticksUntilRefresh = DRIFTWARD$REFRESH_INTERVAL;
        final var self = (ThermalBehaviour) (Object) this;
        final var level = self.getWorld();
        if (level != null) {
            cachedAmbientTemperature = ThermalBehaviour.getAmbientTemperature(level, self.getPos());
        }
    }
}
