package ink.astrius.driftward.mixin.rope;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.ponder.scenes.RopeScenes;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.registry.ModItems;

@Restriction(
    require = {
        @Condition("simulated"),
        @Condition("farmersdelight")
    }
)
@Mixin(RopeScenes.class)
public class RopeScenesMixin {
    @Definition(id = "ROPE_COUPLING", field = "Ldev/simulated_team/simulated/index/SimItems;ROPE_COUPLING:Lcom/tterrag/registrate/util/entry/ItemEntry;")
    @Definition(id = "asStack", method = "Lcom/tterrag/registrate/util/entry/ItemEntry;asStack()Lnet/minecraft/world/item/ItemStack;")
    @Expression("ROPE_COUPLING.asStack()")
    @Redirect(
        method = "ropeIntro",
        at = @At(value = "MIXINEXTRAS:EXPRESSION")
    )
    private static ItemStack replaceRope1(ItemEntry instance) {
        return new ItemStack(ModItems.ROPE.get());
    }

    @Definition(id = "ROPE_COUPLING", field = "Ldev/simulated_team/simulated/index/SimItems;ROPE_COUPLING:Lcom/tterrag/registrate/util/entry/ItemEntry;")
    @Definition(id = "asStack", method = "Lcom/tterrag/registrate/util/entry/ItemEntry;asStack()Lnet/minecraft/world/item/ItemStack;")
    @Expression("ROPE_COUPLING.asStack()")
    @Redirect(
        method = "ropeConnections",
        at = @At(value = "MIXINEXTRAS:EXPRESSION")
    )
    private static ItemStack replaceRope2(ItemEntry instance) {
        return new ItemStack(ModItems.ROPE.get());
    }
}
