package ink.astrius.driftward.mixin.crittersandcompanions;

import dev.hybridlabs.aquatic.item.HAItems;
import io.github.bonsaistudi0s.crittersandcompanions.common.entity.OtterEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

// TODO: remove this mixin and add this method to critters_hybrid_item_merge.OtterEntityMixin
//  when https://github.com/bonsaistudi0s/CrittersAndCompanions/pull/191 is merged

@Mixin(OtterEntity.class)
public abstract class OtterEntityMixin extends Animal {
    private OtterEntityMixin() {
        super(null, null);
    }

    @Shadow
    public abstract boolean isEating();

    @Overwrite
    public InteractionResult mobInteract(Player player, InteractionHand interactionHand) {
        ItemStack handStack = player.getItemInHand(interactionHand);
        if (!this.isFood(handStack)) {
            return InteractionResult.PASS;
        }
        if (this.isEating()) {
            return InteractionResult.CONSUME;
        }

        boolean isClam = handStack.is(HAItems.INSTANCE.getCLAM().get());
        boolean canFallInLove = this.getAge() == 0 && this.canFallInLove();
        boolean shouldStartEating = isClam || canFallInLove || this.isBaby();
        if (!shouldStartEating) {
            return InteractionResult.CONSUME;
        }

        this.setItemInHand(InteractionHand.MAIN_HAND, handStack.split(1));
        if (canFallInLove) {
            this.setInLove(player);
        }
        if (this.isBaby()) {
            this.ageUp(getSpeedUpSecondsWhenFeeding(-this.getAge()), true);
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}
