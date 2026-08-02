package ink.astrius.driftwardfixes.mixin.client;

import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.content.items.rope.RopeItem.ClientRopeItemHandler;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientRopeItemHandler.class)
public class ClientRopeItemHandlerMixin {
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"
        )
    )
    private static boolean allowAnyItem(ItemEntry<RopeItem> instance, ItemStack itemStack) {
        return true;
    }
}
