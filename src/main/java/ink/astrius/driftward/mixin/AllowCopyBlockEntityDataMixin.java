// From https://github.com/DaFuqs/Spectrum/blob/1a0d2383a98b6ec494fde9e8527f2994fda4e952/src/main/java/de/dafuqs/spectrum/mixin/AllowCopyBlockEntityDataMixin.java
package ink.astrius.driftward.mixin;

import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrialSpawnerBlockEntity.class)
public abstract class AllowCopyBlockEntityDataMixin {
    @Inject(method = "onlyOpCanSetNbt", at = @At("HEAD"), cancellable = true)
    public void allowPlacingWithBlockData(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(false);
    }
}
