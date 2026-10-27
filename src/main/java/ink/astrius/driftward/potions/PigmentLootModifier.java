package ink.astrius.driftward.potions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.dafuqs.spectrum.registries.SpectrumPotions;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;

// Vanilla potion types only come from brewing, which the pack disables in favor of the Potion
// Workshop. Loot tables still hand them out through `set_potion`, so turn every such item into the
// pigment potion with the same effects instead of patching tables one by one.
public class PigmentLootModifier extends LootModifier {
    public static final MapCodec<PigmentLootModifier> CODEC = RecordCodecBuilder.mapCodec(
        inst -> codecStart(inst).apply(inst, PigmentLootModifier::new)
    );

    public PigmentLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        for (final var stack : loot) {
            final var contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null || contents.potion().isEmpty()) {
                continue;
            }
            final var potion = contents.potion().get();
            if (potion.is(SpectrumPotions.PIGMENT_POTION) || potion.value().getEffects().isEmpty()) {
                // Water, awkward, mundane and thick potions stay as they are
                continue;
            }
            final var effects = new ArrayList<MobEffectInstance>();
            potion.value().getEffects().forEach(effect -> effects.add(new MobEffectInstance(effect)));
            effects.addAll(contents.customEffects());
            stack.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.of(SpectrumPotions.PIGMENT_POTION), Optional.empty(), effects)
            );
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
