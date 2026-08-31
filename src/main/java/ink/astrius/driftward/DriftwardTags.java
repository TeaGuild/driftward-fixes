package ink.astrius.driftward;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class DriftwardTags {
    public static final TagKey<Item> CUSTOM_ENDER_PEARLS = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("endermanoverhaul", "ender_pearls")
    );
    public static final TagKey<Block> END_BASE = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "end_base")
    );
    public static final TagKey<Block> ERODE = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "erode")
    );
    public static final TagKey<Block> REMOVE_FROM_CRASHED_SHIP = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "remove_from_crashed_ship")
    );
}
