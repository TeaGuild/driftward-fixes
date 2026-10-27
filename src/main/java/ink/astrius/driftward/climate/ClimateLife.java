package ink.astrius.driftward.climate;

import ink.astrius.driftward.Driftward;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

// Crops and mob spawns that follow the climate model: frost damages and kills crops (shelter
// counts, so greenhouses work), cold and warm mobs spawn only where it is cold or warm.
public final class ClimateLife {
    private static final TagKey<Block> WINTER_CROPS =
        TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("sereneseasons", "winter_crops"));
    public static final TagKey<EntityType<?>> COLD_SPAWNING =
        TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "cold_spawning"));
    public static final TagKey<EntityType<?>> WARM_SPAWNING =
        TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "warm_spawning"));

    private ClimateLife() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClimateLife::onCropGrow);
        NeoForge.EVENT_BUS.addListener(ClimateLife::onSpawnPosition);
    }

    private static void onCropGrow(CropGrowEvent.Pre event) {
        var c = ClimateConstants.get();
        if (!(event.getLevel() instanceof ServerLevel level) || !c.enabled() || !c.cropsEnabled()
            || level.dimension() != Level.OVERWORLD) {
            return;
        }
        var state = event.getState();
        var block = state.getBlock();
        if (!(block instanceof CropBlock || block instanceof StemBlock || state.is(BlockTags.CROPS))) {
            return;
        }
        var pos = event.getPos();
        double celsius = ClimateModel.temperature(level, pos, level.getBiome(pos));
        double hardy = state.is(WINTER_CROPS) ? c.hardyOffset() : 0;

        if (celsius >= c.cropMinGrowth() - hardy) {
            return;
        }
        event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        if (!(block instanceof CropBlock crop)) {
            return;
        }
        if (celsius < c.cropKill() - hardy) {
            // Hard frost: the plant dies and leaves its seed
            level.destroyBlock(pos, false);
            Block.popResource(level, pos, new ItemStack(block));
        } else if (celsius < c.cropFrost() - hardy && level.random.nextDouble() < c.cropFrostChance()) {
            int age = crop.getAge(state);
            if (age > 0) {
                level.setBlock(pos, crop.getStateForAge(age - 1), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void onSpawnPosition(MobSpawnEvent.PositionCheck event) {
        var c = ClimateConstants.get();
        if (!c.enabled() || !c.mobsEnabled()) {
            return;
        }
        var type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        var entity = event.getEntity();
        boolean cold = entity.getType().is(COLD_SPAWNING);
        boolean warm = entity.getType().is(WARM_SPAWNING);
        if (!cold && !warm || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        var pos = entity.blockPosition();
        if (!level.isLoaded(pos)) {
            return;
        }
        double celsius = ClimateModel.temperature(level, pos, level.getBiome(pos));
        if (cold && celsius > c.coldMobMax() || warm && celsius < c.warmMobMin()) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
