package ink.astrius.driftward.mixin.betterend_silk;

import io.github.bonsaistudi0s.crittersandcompanions.common.registry.CACItems;
import net.minecraft.world.item.Item;
import org.betterx.betterend.blocks.SilkMothHiveBlock;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SilkMothHiveBlock.class)
public class SilkMothHiveBlockMixin {
    @Redirect(
        method = "useItemOn",
        at = @At(
            value = "FIELD",
            target = "Lorg/betterx/betterend/registry/EndItems;SILK_FIBER:Lnet/minecraft/world/item/Item;",
            opcode = Opcodes.GETSTATIC
        )
    )
    public Item replaceSilk() {
        return CACItems.SILK.get();
    }
}
