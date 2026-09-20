package ink.astrius.driftward.reg;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public final class SingleItemIngredient {
    public static final Codec<SingleItemIngredient> CODEC =
        Codec
            .xor(
                BuiltInRegistries.ITEM.byNameCodec(),
                TagKey.hashedCodec(Registries.ITEM)
            )
            .xmap(
                either -> either.map(SingleItemIngredient::ofItem, SingleItemIngredient::ofTag),
                ingredient -> ingredient.item != null
                    ? Either.left(ingredient.item)
                    : Either.right(ingredient.tag)
            );

    private @Nullable Item item;
    private @Nullable TagKey<Item> tag;
    public Ingredient ingredient;

    private SingleItemIngredient() {
    }

    public static SingleItemIngredient ofItem(Item item) {
        final var res = new SingleItemIngredient();
        res.item = item;
        res.ingredient = Ingredient.of(item);
        return res;
    }

    public static SingleItemIngredient ofTag(TagKey<Item> tag) {
        final var res = new SingleItemIngredient();
        res.tag = tag;
        res.ingredient = Ingredient.of(tag);
        return res;
    }
}
