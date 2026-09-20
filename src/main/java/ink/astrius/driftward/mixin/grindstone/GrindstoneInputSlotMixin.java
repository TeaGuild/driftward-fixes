package ink.astrius.driftward.mixin.grindstone;

import ink.astrius.driftward.reg.DisenchantingRecipe;
import ink.astrius.driftward.reg.DriftwardReg;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.world.inventory.GrindstoneMenu$2", "net.minecraft.world.inventory.GrindstoneMenu$3"})
public class GrindstoneInputSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void fixes$allowCustomRecipes(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        final var input = new DisenchantingRecipe.Input(stack, ItemStack.EMPTY);
        final var recipes = ServerLifecycleHooks.getCurrentServer().getRecipeManager();
        final var recipe = recipes.getRecipeFor(
            DriftwardReg.DISENCHANTING_TYPE.get(),
            input,
            null
        );
        if (recipe.isPresent()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
