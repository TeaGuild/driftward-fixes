package ink.astrius.driftward.mixin;

import de.dafuqs.spectrum.registries.SpectrumPotions;
import java.util.Optional;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("spectrum"))
@Mixin(PotionItem.class)
public abstract class PigmentPotionNameMixin extends Item {
    public PigmentPotionNameMixin() {
        super(null);
    }

    @Inject(method = "getDescriptionId", at = @At("HEAD"), cancellable = true)
    private void customizePigmentPotionName(ItemStack stack, CallbackInfoReturnable<String> cir) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (
            contents.potion().equals(Optional.of(SpectrumPotions.PIGMENT_POTION))
            && contents.customEffects().size() == 1
        ) {
            var key = contents.customEffects().get(0).getEffect().unwrapKey();
            if (key.isPresent()) {
                // Try to construct something similar to `item.minecraft.potion.effect.*`.
                // The problem is that Minecraft uses a *potion* name here, while we only know the
                // *effect* name, but the two coincide often enough that it's easier than creating
                // a whole new translation category.
                var name = this.getDescriptionId() + ".effect." + key.get().location().getPath();
                cir.setReturnValue(name);
            }
        }
    }
}
