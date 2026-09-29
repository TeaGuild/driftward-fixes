package ink.astrius.driftward.mixin.crittersandcompanions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bonsaistudi0s.crittersandcompanions.common.entity.GrapplingHookEntity;
import java.util.function.Predicate;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Restriction(require = @Condition("crittersandcompanions"))
@Mixin(EntityCollisionContext.class)
public abstract class EntityCollisionContextMixin {
    @Nullable
    @Shadow
    private Entity entity;

    @WrapOperation(
        method = "canStandOnFluid",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"
        )
    )
    private boolean canStandOnFluid(Predicate predicate, Object state, Operation<Boolean> original) {
        if (this.entity instanceof GrapplingHookEntity hook) {
            // Make sure ray casting stops at water so that entering the top level of water is
            // recognized reliably regardless of velocity
            if (!((FluidState)state).is(Fluids.WATER)) {
                return false;
            }
            var enchantment = hook.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FROST_WALKER);
            var frostWalkerLevel = hook.getItem().getEnchantmentLevel(enchantment);
            return frostWalkerLevel > 0;
        }
        return original.call(predicate, state);
    }
}
