package ink.astrius.driftward.mixin;

import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.details.ItemDetails;
import dan200.computercraft.shared.util.NBTUtil;
import java.util.Map;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("computercraft"))
@Mixin(ItemDetails.class)
public class RawNbtMixin {
    @Inject(method = "fill", at = @At("HEAD"))
    private static void addRawNbt(Map<? super String, Object> data, ItemStack stack, CallbackInfo ci) {
        var components = stack.getComponentsPatch();
        if (!components.isEmpty()) {
            // Follows `ItemDetails.getComponentHash`
            var server = CommonHooks.getServer();
            var registries = server == null ? RegistryAccess.EMPTY : server.registryAccess();
            var nbt = DataComponentPatch.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), components)
                .result().orElse(null);
            data.put("rawNBT", NBTUtil.toLua(nbt));
        }
    }
}
