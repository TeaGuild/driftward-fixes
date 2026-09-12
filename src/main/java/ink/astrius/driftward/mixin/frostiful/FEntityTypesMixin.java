package ink.astrius.driftward.mixin.frostiful;

import com.github.thedeathlycow.frostiful.registry.FEntityTypes;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.MobCategory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Restriction(require = @Condition("frostiful"))
@Mixin(FEntityTypes.class)
public class FEntityTypesMixin {
    @Redirect(
        method = "<clinit>",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/MobCategory;CREATURE:Lnet/minecraft/world/entity/MobCategory;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static MobCategory makeGlacialArrowMisc() {
        return MobCategory.MISC;
    }

    @Redirect(
        method = "<clinit>",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/MobCategory;AMBIENT:Lnet/minecraft/world/entity/MobCategory;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static MobCategory makeFreezingWindMisc() {
        return MobCategory.MISC;
    }
}
