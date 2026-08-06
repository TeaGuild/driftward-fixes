package ink.astrius.driftwardfixes.mixin;

import io.github.bonsaistudi0s.crittersandcompanions.common.entity.GrapplingHookEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ThrowableProjectile.class)
public class GrapplingHookEntityMixin {
    @Inject(
        method = "canUsePortal",
        at = @At("HEAD"),
        cancellable = true
    )
    public void canUsePortal(boolean allowPassengers, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof GrapplingHookEntity) {
            cir.setReturnValue(false);
        }
    }
}
