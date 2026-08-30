package ink.astrius.driftwardfixes;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Driftward {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("driftward");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("driftward");
    public static final DeferredRegister<CriterionTrigger<?>> CRITERIA_TRIGGERS = DeferredRegister.create(
        BuiltInRegistries.TRIGGER_TYPES,
        DriftwardFixes.MOD_ID
    );

    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        final var res = BLOCKS.register(name, block);
        ITEMS.register(name, (b) -> new BlockItem(res.get(), new Item.Properties()));
        return res;
    }
}
