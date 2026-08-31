package ink.astrius.driftward.mixin.cobblegen;

import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FluidInteractionRegistry.class)
public class FluidInteractionRegistryMixin {
    @Redirect(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidInteractionRegistry;addInteraction(Lnet/neoforged/neoforge/fluids/FluidType;Lnet/neoforged/neoforge/fluids/FluidInteractionRegistry$InteractionInformation;)V",
            ordinal = 0
        )
    )
    private static void lavaWaterInteraction(FluidType source, FluidInteractionRegistry.InteractionInformation interaction) {
    }
}
