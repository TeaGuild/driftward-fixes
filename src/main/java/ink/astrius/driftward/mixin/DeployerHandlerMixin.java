package ink.astrius.driftward.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerHandler;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Restriction(require = @Condition("create"))
@Mixin(DeployerHandler.class)
public class DeployerHandlerMixin {
    @Unique
    private static final TagKey<Item> DRIFTWARD$DEPLOYER_BLACKLIST = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("create", "deployer_blacklist")
    );

    @Inject(method = "shouldActivate", at = @At("HEAD"), cancellable = true)
    private static void handleBlacklist(ItemStack held, Level world, BlockPos targetPos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (held.is(DRIFTWARD$DEPLOYER_BLACKLIST)) {
            cir.setReturnValue(false);
        }
    }
}
