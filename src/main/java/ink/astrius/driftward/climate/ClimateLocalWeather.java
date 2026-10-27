package ink.astrius.driftward.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.concurrent.ConcurrentHashMap;

// Local weather in the game: rain and snow fall only where the weather layer has precipitation.
//
// - Serene Seasons' precipitation hooks (server and client) and the client's rain renderer ask
//   `dry` per column.
// - Vanilla still has one global rain flag (sky, sleeping through storms, mods reading it); it
//   follows whether it's raining or thundering near any player, so global rain means "somewhere".
// - The client's rain and thunder levels (sky darkness, rain loudness) come from the weather at the
//   player, through the climate sync.
public final class ClimateLocalWeather {
    private static final double WET = 0.05;
    private static final int CACHE_TICKS = 40;
    private static final ConcurrentHashMap<Long, ClimateWeather.State> CACHE = new ConcurrentHashMap<>();
    private static volatile long cacheBucket = Long.MIN_VALUE;

    private ClimateLocalWeather() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClimateLocalWeather::onLevelTick);
    }

    public static boolean active(Level level) {
        // On the client nothing is known (and the synced config may not be there) until a grid arrives
        if (!(level instanceof ServerLevel) && !ClimateSync.Grid.present()) {
            return false;
        }
        var c = ClimateConstants.get();
        return ClimatePrecipitation.applies(level) && c.weatherEnabled() && c.weatherDrivesRain();
    }

    // Weather at a loaded server position, cached per 16×16 column for a couple of seconds
    public static ClimateWeather.State at(ServerLevel level, BlockPos pos) {
        long bucket = level.getGameTime() / CACHE_TICKS;
        if (bucket != cacheBucket) {
            CACHE.clear();
            cacheBucket = bucket;
        }
        long key = BlockPos.asLong(pos.getX() >> 4, 0, pos.getZ() >> 4);
        var cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (!level.isLoaded(pos)) {
            return ClimateWeather.CALM;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        var surface = new BlockPos(pos.getX(), y, pos.getZ());
        var biome = level.getBiome(surface);
        var weather = ClimateModel.weather(level, surface, biome, ClimateModel.sample(level, surface),
            ClimateModel.live(level, surface, biome, true));
        if (CACHE.size() > 4096) {
            CACHE.clear();
        }
        CACHE.put(key, weather);
        return weather;
    }

    // Nothing falls at this position (only meaningful when `active`): no precipitation in the
    // column, or the position is above the cloud base
    public static boolean dry(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            var w = at(serverLevel, pos);
            return w.precipitation() < WET || pos.getY() > w.cloudBase();
        }
        double precipitation = ClimateSync.Grid.precipitation(pos);
        return !Double.isNaN(precipitation) && precipitation < WET;
    }

    public static boolean thundering(ServerLevel level, BlockPos pos) {
        return at(level, pos).thunder() > 0.3;
    }

    // Keep vanilla's global flags in step with the weather around the players
    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % CACHE_TICKS != 0 || !active(level)) {
            return;
        }
        double rain = 0, thunder = 0;
        for (var player : level.players()) {
            var w = at(level, player.blockPosition());
            rain = Math.max(rain, w.precipitation());
            thunder = Math.max(thunder, w.thunder());
        }
        boolean raining = rain >= WET;
        boolean thundering = raining && thunder > 0.3;
        if (raining != level.isRaining() || thundering != level.isThundering()) {
            level.setWeatherParameters(raining ? 0 : 6000, raining ? 6000 : 0, raining, thundering);
        }
    }
}
