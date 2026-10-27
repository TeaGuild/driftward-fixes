package ink.astrius.driftward.reg;

import ink.astrius.driftward.Driftward;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

import java.util.function.Supplier;

public class DriftwardReg {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Driftward.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Driftward.MOD_ID);
    public static final DeferredRegister<CriterionTrigger<?>> CRITERIA_TRIGGERS =
        DeferredRegister.create(Registries.TRIGGER_TYPE, Driftward.MOD_ID);
    public static final Supplier<ItemUsedOnLocationTrigger> ROTATED_WITH_WRENCH =
        DriftwardReg.CRITERIA_TRIGGERS.register(
            "rotated_with_wrench", ItemUsedOnLocationTrigger::new
        );

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Driftward.MOD_ID);
    public static final Supplier<DisenchantingRecipe.Serializer> DISENCHANTING_SERIALIZER =
        RECIPE_SERIALIZERS.register("disenchanting", DisenchantingRecipe.Serializer::new);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, Driftward.MOD_ID);
    public static final Supplier<RecipeType<DisenchantingRecipe>> DISENCHANTING_TYPE =
        RECIPE_TYPES.register(
            "disenchanting",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "disenchanting"))
        );

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Driftward.MOD_ID);

    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        final var res = BLOCKS.register(name, block);
        ITEMS.register(name, (b) -> new BlockItem(res.get(), new Item.Properties()));
        return res;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        CRITERIA_TRIGGERS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        RECIPE_TYPES.register(modBus);
        LOOT_MODIFIERS.register(modBus);
    }
}
