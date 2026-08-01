package ink.astrius.driftwardfixes.mixin;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WrenchItem.class)
public class WrenchItemMixin {
    @Redirect(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            ordinal = 1
        )
    )
    public InteractionResult trySupplementaries(Item instance, UseOnContext p_41427_) {
        final var supplementariesWrench = (net.mehvahdjukaar.supplementaries.common.items.WrenchItem) ModRegistry.WRENCH.get();
        return supplementariesWrench.useOn(p_41427_);
    }
}
