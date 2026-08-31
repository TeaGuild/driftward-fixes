package ink.astrius.driftward.mixin.grindstone;

import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static ink.astrius.driftward.DriftwardTags.CUSTOM_ENDER_PEARLS;

// because GrindstoneInputSlotMixin is required for this and it depends on supplementaries
@Restriction(require = @Condition("supplementaries"))
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenu {
    @Shadow
    @Final
    Container repairSlots;

    @Shadow
    @Final
    private Container resultSlots;

    protected GrindstoneMenuMixin(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void fixes$updatePearlResult(CallbackInfo ci) {
        ItemStack stack1 = this.repairSlots.getItem(0);
        ItemStack stack2 = this.repairSlots.getItem(1);

        boolean pearls1 = stack1.is(CUSTOM_ENDER_PEARLS);
        boolean pearls2 = stack2.is(CUSTOM_ENDER_PEARLS);

        if ((pearls1 && stack2.isEmpty()) || (pearls2 && stack1.isEmpty()) || (pearls1 && pearls2)) {
            int count = stack1.getCount() + stack2.getCount();
            if (count <= Items.ENDER_PEARL.getDefaultMaxStackSize()) {
                this.resultSlots.setItem(0, new ItemStack(Items.ENDER_PEARL, count));
                this.broadcastChanges();
                ci.cancel();
            }
        }
    }
}
