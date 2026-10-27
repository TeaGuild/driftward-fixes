package ink.astrius.driftward.potions;

import de.dafuqs.spectrum.registries.SpectrumPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Optional;

public class PigmentPotionNames {
    // Try to construct something similar to `item.minecraft.potion.effect.*`. The problem is that
    // Minecraft uses a *potion* name here, while we only know the *effect* name, but the two
    // coincide often enough that it's easier than creating a whole new translation category.
    public static Optional<String> descriptionId(ItemStack stack, String prefix) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (
            !contents.potion().equals(Optional.of(SpectrumPotions.PIGMENT_POTION))
            || contents.customEffects().size() != 1
        ) {
            return Optional.empty();
        }
        return contents.customEffects().get(0).getEffect().unwrapKey()
            .map(key -> prefix + key.location().getPath());
    }
}
