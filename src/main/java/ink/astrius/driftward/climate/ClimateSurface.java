package ink.astrius.driftward.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayList;

// Snow and ice catch-up. Seasons only change snow and ice in chunks near players, so a chunk last
// seen in winter would still be snowed in when a player comes back in summer (and new chunks get
// worldgen snow regardless of the season). When a chunk loads, before it is sent to anyone, its
// surface is set to what fits the current climate: no history is replayed, so it needs no
// bookkeeping. Changes only happen well away from the snow line, so nothing flickers.
//
// - snow layers go where the daily mean is well below the snow line and the biome has
//   precipitation, and melt where it's well above; full snow blocks (often builds) are left alone
// - surface ice on water follows the water temperature, which lags the season: lakes freeze when
//   the water is at freezing and the air is cold, and thaw once the water warms. Ice on land is left
//   alone
public final class ClimateSurface {
    public static final TagKey<Biome> OCEAN =
        TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_ocean"));
    private static final TagKey<Biome> SEASONS_BLACKLIST =
        TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("sereneseasons", "blacklisted_biomes"));

    private ClimateSurface() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClimateSurface::onChunkLoad);
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        // Full chunks are promoted on the server thread; anything else isn't safe to edit here
        if (!level.getServer().isSameThread() || !ClimatePrecipitation.applies(level)
            || !ClimateConstants.get().surfaceEnabled()) {
            return;
        }
        patch(level, chunk);
    }

    // The climate of one 4×4 column cell at its daily mean
    private record Cell(Holder<Biome> biome, double snow, double air, double water, boolean frozenWater, boolean skip) {}

    public static int patch(ServerLevel level, LevelChunk chunk) {
        var c = ClimateConstants.get();
        var sampler = level.getChunkSource().randomState().sampler();
        double season = ClimateSeasons.factor(level, c.seasonPeak());
        double waterSeason = ClimateSeasons.factor(level, c.seasonPeak(), c.waterLag());
        long dailyMean = (long) c.diurnalPeakTick() + 6000;
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();

        var cells = new Cell[16];
        var pos = new BlockPos.MutableBlockPos();
        for (int cx = 0; cx < 4; cx++) {
            for (int cz = 0; cz < 4; cz++) {
                int x = x0 + cx * 4 + 2;
                int z = z0 + cz * 4 + 2;
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1;
                pos.set(x, y, z);
                var biome = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
                if (biome.is(SEASONS_BLACKLIST)) {
                    cells[cx * 4 + cz] = new Cell(biome, 0, 0, 0, false, true);
                    continue;
                }
                Climate.TargetPoint point = sampler.sample(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
                var noise = new ClimateModel.Noise(
                    Climate.unquantizeCoord(point.temperature()),
                    Climate.unquantizeCoord(point.humidity()),
                    Climate.unquantizeCoord(point.continentalness())
                );
                var at = new ClimateModel.Conditions(y, season, waterSeason, dailyMean, 0, 0, 0, false, null);
                var climate = ClimateModel.breakdown(level, pos, biome, noise, at);
                var correction = biome.getData(BiomeClimate.DATA_MAP);
                boolean glacier = correction != null && correction.glacier();
                double freezing = biome.is(OCEAN) ? c.seawaterMin() : c.freshwaterMin();
                cells[cx * 4 + cz] = new Cell(
                    biome,
                    ClimatePrecipitation.celsius(biome, climate),
                    climate.airCelsius(),
                    climate.waterCelsius(),
                    glacier || climate.waterCelsius() <= freezing + 0.1,
                    false
                );
            }
        }

        double snowLine = c.snowCelsius();
        double margin = c.surfaceMargin();
        var changed = new ArrayList<BlockPos>();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                var cell = cells[(lx >> 2) * 4 + (lz >> 2)];
                if (cell.skip()) {
                    continue;
                }
                int x = x0 + lx;
                int z = z0 + lz;
                int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
                var ground = new BlockPos(x, top, z);
                var above = ground.above();
                var groundState = chunk.getBlockState(ground);
                var aboveState = chunk.getBlockState(above);

                // Thin snow isn't motion-blocking, so it sits above the heightmap's top block;
                // thick snow is the top block itself
                if (aboveState.is(Blocks.SNOW) || groundState.is(Blocks.SNOW)) {
                    if (cell.snow() > snowLine + margin) {
                        set(chunk, aboveState.is(Blocks.SNOW) ? above : ground, Blocks.AIR.defaultBlockState(), changed);
                    }
                    continue;
                }

                // Surface ice over water
                if (groundState.is(Blocks.ICE) && chunk.getFluidState(ground.below()).is(FluidTags.WATER)) {
                    if (!cell.frozenWater() && cell.water() > snowLine + margin) {
                        set(chunk, ground, Blocks.WATER.defaultBlockState(), changed);
                    }
                    continue;
                }
                var fluid = groundState.getFluidState();
                if (fluid.is(FluidTags.WATER) && fluid.isSource() && groundState.is(Blocks.WATER)
                    && aboveState.isAir() && cell.frozenWater() && cell.air() < snowLine - margin
                    && level.getBrightness(LightLayer.BLOCK, above) < 10) {
                    set(chunk, ground, Blocks.ICE.defaultBlockState(), changed);
                    continue;
                }

                // New snow cover
                if (aboveState.isAir() && cell.snow() < snowLine - margin && cell.biome().value().hasPrecipitation()
                    && canHoldSnow(chunk, ground, groundState) && level.getBrightness(LightLayer.BLOCK, above) < 10) {
                    set(chunk, above, Blocks.SNOW.defaultBlockState(), changed);
                }
            }
        }

        if (!changed.isEmpty()) {
            chunk.setUnsaved(true);
            var light = level.getChunkSource().getLightEngine();
            for (var p : changed) {
                light.checkBlock(p);
            }
        }
        return changed.size();
    }

    // SnowLayerBlock.canSurvive without a Level: full top face, not ice or barrier
    private static boolean canHoldSnow(LevelChunk chunk, BlockPos ground, BlockState groundState) {
        if (groundState.is(Blocks.ICE) || groundState.is(Blocks.PACKED_ICE) || groundState.is(Blocks.BARRIER)
            || !groundState.getFluidState().isEmpty()) {
            return false;
        }
        if (groundState.is(Blocks.HONEY_BLOCK) || groundState.is(Blocks.SOUL_SAND)) {
            return true;
        }
        return Block.isFaceFull(groundState.getCollisionShape(chunk, ground), Direction.UP)
            || groundState.getBlock() instanceof SnowLayerBlock && groundState.getValue(SnowLayerBlock.LAYERS) == 8;
    }

    // Straight into the chunk section: no block updates, neighbours or fluid ticks (the chunk
    // isn't in the world yet), just heightmaps
    private static void set(LevelChunk chunk, BlockPos pos, BlockState state, ArrayList<BlockPos> changed) {
        var section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
        for (var heightmap : chunk.getHeightmaps()) {
            heightmap.getValue().update(pos.getX() & 15, pos.getY(), pos.getZ() & 15, state);
        }
        changed.add(pos);
    }
}
