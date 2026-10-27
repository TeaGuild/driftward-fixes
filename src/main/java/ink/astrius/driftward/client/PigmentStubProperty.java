package ink.astrius.driftward.client;

import de.dafuqs.spectrum.registries.SpectrumPotions;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Optional;

// `driftward:pigment_stub` is 1 for pigment potions without effects, so the pack's item models can
// give these "blank" potions their own rainbow look.
public class PigmentStubProperty {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("driftward", "pigment_stub");

    public static void registerClient(IEventBus modBus) {
        modBus.addListener(PigmentStubProperty::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (final var item : new Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
                ItemProperties.register(item, ID, (stack, level, entity, seed) -> {
                    final var contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                    final var isPigment = contents.potion().equals(Optional.of(SpectrumPotions.PIGMENT_POTION));
                    return isPigment && !contents.hasEffects() ? 1.0F : 0.0F;
                });
            }
        });
    }
}
