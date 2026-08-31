package ink.astrius.driftward.mixin.crittersandcompanions;

import io.github.bonsaistudi0s.crittersandcompanions.common.entity.*;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Restriction(require = @Condition("crittersandcompanions"))
@Mixin(Animal.class)
public class AnimalMixin {
    @Inject(
        method = "removeWhenFarAway",
        at = @At("HEAD"),
        cancellable = true
    )
    public void removeWhenFarAway(double distanceToClosestPlayer, CallbackInfoReturnable<Boolean> cir) {
        final var animal = (Animal) (Object) this;
        if (animal instanceof TamableAnimal tamable) {
            if (
                tamable instanceof DragonflyEntity
                    || tamable instanceof LadybugEntity
                    || tamable instanceof RolyPolyEntity
                    || tamable instanceof SnailEntity
                    || tamable instanceof StagBeetleEntity
                    || tamable instanceof StickBugEntity
                    || tamable instanceof WeevilEntity
            ) {
                cir.setReturnValue(!(tamable.isTame() || tamable.isPersistenceRequired()));
            }
        } else if (animal instanceof LeafInsectEntity || animal instanceof OtterEntity) {
            cir.setReturnValue(!animal.isPersistenceRequired());
        }
    }
}
