package ink.astrius.driftwardfixes.mixin.wrench;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static ink.astrius.driftwardfixes.DriftwardFixes.ROTATED_WITH_WRENCH;

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
    public InteractionResult trySupplementaries(Item instance, UseOnContext context) {
        final var supplementariesWrench = (net.mehvahdjukaar.supplementaries.common.items.WrenchItem) ModRegistry.WRENCH.get();
        final var supplementariesRes = supplementariesWrench.useOn(context);
        if (supplementariesRes.consumesAction() && (context.getPlayer() instanceof ServerPlayer player)) {
            ROTATED_WITH_WRENCH.get().trigger(player, context.getClickedPos(), context.getItemInHand());
        }
        return supplementariesRes;
    }
}
