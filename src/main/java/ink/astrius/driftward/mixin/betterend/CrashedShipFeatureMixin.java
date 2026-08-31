package ink.astrius.driftward.mixin.betterend;

import com.llamalad7.mixinextras.sugar.Local;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import org.betterx.betterend.blocks.entities.PedestalBlockEntity;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.world.features.CrashedShipFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Restriction(require = @Condition("betterend"))
@Mixin(CrashedShipFeature.class)
public class CrashedShipFeatureMixin {
    // region crashed ship loot
    // from https://github.com/muon-rw/BetterEnd-Crashed-Ships/blob/03b2ec197397465aab63b9175d066d1cd7ba2a50/src/main/java/dev/muon/betterendshipsfix/mixin/CrashedShipFeatureMixin.java#L42

    @Unique
    private static final ResourceKey<LootTable> END_CITY_TREASURE = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/end_city_treasure"));

    @Inject(
        method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z",
        at = @At(value = "RETURN")
    )
    private void injectLootTables(
        FeaturePlaceContext<?> featureConfig,
        CallbackInfoReturnable<Boolean> cir,
        @Local(name = "random") RandomSource random,
        @Local(name = "world") WorldGenLevel world,
        @Local(name = "bounds") BoundingBox bounds
    ) {
        if (cir.getReturnValue()) {
            for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                if (world.getBlockState(pos).getBlock() instanceof ChestBlock) {
                    BlockEntity te = world.getBlockEntity(pos);
                    if (te instanceof ChestBlockEntity) {
                        ((ChestBlockEntity) te).setLootTable(END_CITY_TREASURE, random.nextLong());
                    }
                } else if (world.getBlockState(pos).getBlock() == EndBlocks.PURPUR_PEDESTAL) {
                    placeDamagedElytra(world, pos, random);
                }
            }
        }
    }

    // endregion

    @Unique
    private void placeDamagedElytra(WorldGenLevel world, BlockPos pos, RandomSource random) {
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        elytra.setDamageValue(random.nextInt(432));
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof PedestalBlockEntity) {
            ((PedestalBlockEntity) te).setItem(1, elytra);
        }
    }

    @Redirect(
        method = "canSpawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;getX()I",
            ordinal = 1
        )
    )
    public int fixCanSpawnGetZ(BlockPos pos) {
        return pos.getZ();
    }
}
