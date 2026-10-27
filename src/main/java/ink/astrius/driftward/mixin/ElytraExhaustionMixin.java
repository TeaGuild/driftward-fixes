package ink.astrius.driftward.mixin;

import ink.astrius.driftward.config.ServerConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class ElytraExhaustionMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void driftward$exhaustWhileGliding(CallbackInfo ci) {
        final var player = (Player) (Object) this;
        if (player.level().isClientSide() || !player.isFallFlying() || player.getAbilities().invulnerable) {
            return;
        }
        final var exhaustion = ServerConfig.ELYTRA_EXHAUSTION_PER_TICK.get().floatValue();
        if (exhaustion > 0) {
            player.causeFoodExhaustion(exhaustion);
        }
    }
}
