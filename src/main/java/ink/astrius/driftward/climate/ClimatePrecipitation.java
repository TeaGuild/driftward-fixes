package ink.astrius.driftward.climate;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

// Snow, ice, melting and rain-or-snow from the model instead of vanilla biome temperature.
// Serene Seasons routes all of those through one hook, which `SeasonHooksMixin` redirects here.
public final class ClimatePrecipitation {
    // `isRainingAt` runs for every entity every tick while it rains, so answers are cached per
    // 4×4×4 cell for a couple of seconds
    private static final int CACHE_TICKS = 40;
    private static final Long2DoubleMap CACHE = new Long2DoubleOpenHashMap();
    private static long cacheBucket = Long.MIN_VALUE;

    private static final TagKey<Biome> SEASONS_BLACKLIST =
        TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("sereneseasons", "blacklisted_biomes"));

    private ClimatePrecipitation() {}

    public static boolean applies(Level level) {
        var c = ClimateConstants.get();
        return c.enabled() && c.precipitationEnabled() && level.dimension() == Level.OVERWORLD;
    }

    // The vanilla-scale temperature at a position, or NaN to leave it to Serene Seasons
    public static double vanillaTemperature(Level level, Holder<Biome> biome, BlockPos pos) {
        // On the client, nothing to do (and the synced server config may not be there yet) until
        // the server has sent a grid
        if (level.dimension() != Level.OVERWORLD || (!(level instanceof ServerLevel) && !ClimateSync.Grid.present())) {
            return Double.NaN;
        }
        // Biomes the pack keeps out of seasons (e.g. glaciers that shouldn't melt) stay vanilla
        if (!applies(level) || biome.is(SEASONS_BLACKLIST)) {
            return Double.NaN;
        }
        double celsius = level instanceof ServerLevel serverLevel ? serverCelsius(serverLevel, biome, pos)
            : ClimateSync.Grid.celsius(pos);
        return Double.isNaN(celsius) ? Double.NaN : ClimateConstants.get().toVanilla(celsius);
    }

    private static double serverCelsius(ServerLevel level, Holder<Biome> biome, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return Double.NaN;
        }
        long key = BlockPos.asLong(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
        long bucket = level.getGameTime() / CACHE_TICKS;
        synchronized (CACHE) {
            if (bucket != cacheBucket) {
                CACHE.clear();
                cacheBucket = bucket;
            }
            double cached = CACHE.getOrDefault(key, Double.NaN);
            if (!Double.isNaN(cached)) {
                return cached;
            }
        }
        var at = ClimateModel.live(level, pos, true);
        double celsius = ClimateModel.breakdown(level, pos, biome, ClimateModel.sample(level, pos), at).airCelsius();
        synchronized (CACHE) {
            CACHE.put(key, celsius);
        }
        return celsius;
    }
}
