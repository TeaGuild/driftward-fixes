package ink.astrius.driftwardfixes.mixin.rope;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimPonderScenes;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.createmod.ponder.api.registration.MultiSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.Arrays;
import java.util.function.Function;

@Restriction(
    require = {
        @Condition("simulated"),
        @Condition("farmersdelight")
    }
)
@Mixin(SimPonderScenes.class)
public class SimPonderScenesMixin {
    @Redirect(
        method = "register",
        at = @At(
            value = "INVOKE",
            target = "Lnet/createmod/ponder/api/registration/PonderSceneRegistrationHelper;withKeyFunction(Ljava/util/function/Function;)Lnet/createmod/ponder/api/registration/PonderSceneRegistrationHelper;"
        )
    )
    private static PonderSceneRegistrationHelper<DeferredHolder<?, ?>> upcastLambda(
        PonderSceneRegistrationHelper<ResourceLocation> instance,
        Function<?, ?> stFunction
    ) {
        return instance.withKeyFunction(DeferredHolder::getId);
    }

    @WrapOperation(
        method = "register",
        at = @At(
            value = "INVOKE",
            target = "Lnet/createmod/ponder/api/registration/PonderSceneRegistrationHelper;forComponents([Ljava/lang/Object;)Lnet/createmod/ponder/api/registration/MultiSceneBuilder;"
        )
    )
    private static MultiSceneBuilder replaceRope(PonderSceneRegistrationHelper instance, Object[] ts, Operation<MultiSceneBuilder> original) {
        return original.call(
            instance,
            Arrays.stream(ts)
                .map(x -> x == SimItems.ROPE_COUPLING ? ModItems.ROPE : x)
                .toArray()
        );
    }
}
