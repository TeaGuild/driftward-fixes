package ink.astrius.driftward.mixin;

import net.minecraft.world.level.block.CampfireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CampfireBlock.class)
public class CampfireLitByDefaultMixin {
    @Redirect(
        method = "getStateForPlacement",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Boolean;valueOf(Z)Ljava/lang/Boolean;",
            ordinal = 2
        )
    )
    public Boolean unlitByDefault(boolean b) {
        return Boolean.FALSE;
    }
}
