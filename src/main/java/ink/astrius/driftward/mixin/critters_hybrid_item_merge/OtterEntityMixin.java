package ink.astrius.driftward.mixin.critters_hybrid_item_merge;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.hybridlabs.aquatic.item.HAItems;
import ink.astrius.driftward.DriftwardTags;
import io.github.bonsaistudi0s.crittersandcompanions.common.entity.OtterEntity;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Restriction(
    require = {
        @Condition("hybrid_aquatic"),
        @Condition("crittersandcompanions")
    }
)
@Mixin(OtterEntity.class)
public class OtterEntityMixin {
    @Definition(id = "CLAM", field = "Lio/github/bonsaistudi0s/crittersandcompanions/common/registry/CACItems;CLAM:Ldev/architectury/registry/registries/RegistrySupplier;")
    @Definition(id = "get", method = "Ldev/architectury/registry/registries/RegistrySupplier;get()Ljava/lang/Object;")
    @Expression("CLAM.get()")
    @Redirect(
        method = {"breakingClamOnLand", "breakAndEat", "eatOrOpen", "animation", "isHungryAt", "startEating"},
        at = @At("MIXINEXTRAS:EXPRESSION"),
        require = 6
    )
    private Object replaceClam(RegistrySupplier<?> instance) {
        return HAItems.INSTANCE.getCLAM().get();
    }

    @Definition(id = "PEARL", field = "Lio/github/bonsaistudi0s/crittersandcompanions/common/registry/CACItems;PEARL:Ldev/architectury/registry/registries/RegistrySupplier;")
    @Definition(id = "get", method = "Ldev/architectury/registry/registries/RegistrySupplier;get()Ljava/lang/Object;")
    @Expression("PEARL.get()")
    @Redirect(method = "eatOrOpen", at = @At("MIXINEXTRAS:EXPRESSION"))
    private Object replacePearl(RegistrySupplier<?> instance) {
        return HAItems.INSTANCE.getPEARL().get();
    }
}
