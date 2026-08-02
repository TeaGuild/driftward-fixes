package ink.astrius.driftwardfixes.mixin.rope;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.index.SimPonderTags;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.registry.ModItems;

@Mixin(SimPonderTags.class)
public class SimPonderTagsMixin {
    @Definition(id = "ROPE_COUPLING", field = "Ldev/simulated_team/simulated/index/SimItems;ROPE_COUPLING:Lcom/tterrag/registrate/util/entry/ItemEntry;")
    @Definition(id = "asItem", method = "Lcom/tterrag/registrate/util/entry/ItemEntry;asItem()Lnet/minecraft/world/item/Item;")
    @Expression("ROPE_COUPLING.asItem()")
    @Redirect(
        method = "register",
        at = @At(value = "MIXINEXTRAS:EXPRESSION")
    )
    private static Item replaceRope(ItemEntry<?> instance) {
        return ModItems.ROPE.get();
    }
}
