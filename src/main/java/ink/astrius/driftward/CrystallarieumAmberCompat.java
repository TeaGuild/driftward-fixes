package ink.astrius.driftward;

import de.dafuqs.fractal.api.CreativeSubTabEvent;
import de.dafuqs.spectrum.api.item_group.ItemGroupIDs;
import de.dafuqs.spectrum.blocks.gemstone.SpectrumClusterBlock;
import de.dafuqs.spectrum.compat.SpectrumIntegrationPacks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.betterx.betterend.registry.EndBlocks;

public class CrystallarieumAmberCompat extends SpectrumIntegrationPacks.ModIntegrationPack {
    public static DeferredBlock<SpectrumClusterBlock> SMALL_AMBER_BUD = DriftwardReg.registerBlock(
        "small_amber_bud",
        () -> new SpectrumClusterBlock(
            BlockBehaviour.Properties
                .ofFullCopy(EndBlocks.AMBER_BLOCK)
                .pushReaction(PushReaction.DESTROY)
                .destroyTime(1.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion(),
            SpectrumClusterBlock.GrowthStage.SMALL
        )
    );
    public static DeferredBlock<SpectrumClusterBlock> LARGE_AMBER_BUD = DriftwardReg.registerBlock(
        "large_amber_bud",
        () -> new SpectrumClusterBlock(
            BlockBehaviour.Properties.ofFullCopy(SMALL_AMBER_BUD.get()),
            SpectrumClusterBlock.GrowthStage.LARGE
        )
    );
    public static DeferredBlock<SpectrumClusterBlock> AMBER_CLUSTER = DriftwardReg.registerBlock(
        "amber_cluster",
        () -> new SpectrumClusterBlock(
            BlockBehaviour.Properties.ofFullCopy(SMALL_AMBER_BUD.get()),
            SpectrumClusterBlock.GrowthStage.CLUSTER
        )
    );
    public static DeferredBlock<Block> PURE_AMBER_BLOCK = DriftwardReg.registerBlock(
        "pure_amber_block",
        () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .strength(0.3F)
                .sound(SoundType.GLASS)
        )
    );
    public static DeferredItem<Item> PURE_AMBER = DriftwardReg.ITEMS.registerSimpleItem(
        "pure_amber"
    );

    @Override
    public void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(CrystallarieumAmberCompat::addItemsToSubTabs);
    }

    @Override
    public void registerClient(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(SMALL_AMBER_BUD.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(LARGE_AMBER_BUD.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(AMBER_CLUSTER.get(), RenderType.cutout());
    }

    @SubscribeEvent
    public static void addItemsToSubTabs(CreativeSubTabEvent event) {
        ResourceLocation subGroupId = event.subGroup().getIdentifier();

        if (subGroupId.equals(ItemGroupIDs.SUBTAB_PURE_RESOURCES)) {
            event.getItemDisplayBuilder().accept(PURE_AMBER);
            event.getItemDisplayBuilder().accept(SMALL_AMBER_BUD);
            event.getItemDisplayBuilder().accept(LARGE_AMBER_BUD);
            event.getItemDisplayBuilder().accept(AMBER_CLUSTER);
            event.getItemDisplayBuilder().accept(PURE_AMBER_BLOCK);
        }
    }
}
