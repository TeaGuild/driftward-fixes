package ink.astrius.driftward.mixin.critters_hybrid_item_merge;

import com.bawnorton.mixinsquared.TargetHandler;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.hybridlabs.aquatic.item.HAItems;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.monster.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Restriction(
    require = {
        @Condition("hybrid_aquatic"),
        @Condition("crittersandcompanions")
    }
)
@Mixin(value = Drowned.class, priority = 1500)
public class DrownedMixinMixin {
    @TargetHandler(
        mixin = "io.github.bonsaistudi0s.crittersandcompanions.common.mixin.DrownedMixin",
        name = "addClam"
    )
    @Redirect(
        method = "@MixinSquared:Handler",
        at = @At(
            value = "INVOKE",
            target = "Ldev/architectury/registry/registries/RegistrySupplier;get()Ljava/lang/Object;"
        )
    )
    private Object replaceClam(RegistrySupplier<?> instance) {
        return HAItems.INSTANCE.getCLAM().get();
    }
}
