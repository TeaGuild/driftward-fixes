package ink.astrius.driftward.reg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class DisenchantingRecipe implements Recipe<DisenchantingRecipe.Input> {
    public final SingleItemIngredient ingredient;
    public final Item result;
    public final Optional<Integer> xp;
    private final ItemStack resultStackStatic;

    public DisenchantingRecipe(SingleItemIngredient ingredient, Item result, Optional<Integer> xp) {
        this.ingredient = ingredient;
        this.result = result;
        this.xp = xp;
        resultStackStatic = new ItemStack(result);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient.ingredient);
        return list;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public boolean matches(DisenchantingRecipe.Input input, Level level) {
        final var firstMatches = ingredient.ingredient.test(input.item1);
        final var secondMatches = ingredient.ingredient.test(input.item2);
        return (firstMatches || secondMatches)
            && (firstMatches || input.item1.isEmpty())
            && (secondMatches || input.item2.isEmpty())
            && input.item1.getCount() + input.item2.getCount() <= result.getDefaultMaxStackSize();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return resultStackStatic;
    }

    @Override
    public ItemStack assemble(DisenchantingRecipe.Input input, HolderLookup.Provider registries) {
        return new ItemStack(result, input.item1.getCount() + input.item2.getCount());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DriftwardReg.DISENCHANTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return DriftwardReg.DISENCHANTING_TYPE.get();
    }

    public record Input(ItemStack item1, ItemStack item2) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            if (index == 0) {
                return item1;
            } else if (index == 1) {
                return item2;
            } else {
                throw new IllegalArgumentException("No item for index " + index);
            }
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Serializer implements RecipeSerializer<DisenchantingRecipe> {
        public static final MapCodec<DisenchantingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i
            .group(
                SingleItemIngredient.CODEC.fieldOf("input").forGetter(recipe -> recipe.ingredient),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.INT.validate(
                    value -> value >= 0
                        ? DataResult.success(value)
                        : DataResult.error(() -> "xp must be non-negative")
                ).optionalFieldOf("xp").forGetter(recipe -> recipe.xp)
            )
            .apply(i, DisenchantingRecipe::new)
        );
        private static final StreamCodec<RegistryFriendlyByteBuf, DisenchantingRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

        @Override
        public MapCodec<DisenchantingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DisenchantingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
