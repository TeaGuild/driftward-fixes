package ink.astrius.driftward.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Locale;
import java.util.Optional;

// Nearest weather of a kind, searched outward in rings from the world generator (no chunks are
// loaded), like the maps
public final class ClimateFind {
    public enum Kind {
        RAIN, SNOW, STORM, FOG, CLEAR;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Found(BlockPos pos, double distance, ClimateWeather.State weather, double celsius) {}

    private ClimateFind() {}

    public static Optional<Found> nearest(ServerLevel level, BlockPos from, Kind kind, int radius, int step) {
        var c = ClimateConstants.get();
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        var random = source.randomState();
        var sampler = random.sampler();
        double season = ClimateSeasons.factor(level, c.seasonPeak());
        double waterSeason = ClimateSeasons.factor(level, c.seasonPeak(), c.waterLag());
        long dayTime = level.getDayTime();
        long gameTime = level.getGameTime();
        java.util.function.IntBinaryOperator terrain = (x, z) ->
            generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
        var pos = new BlockPos.MutableBlockPos();

        for (int ring = 0; ring * step <= radius; ring++) {
            Found best = null;
            for (int i = -ring; i <= ring; i++) {
                for (int j = -ring; j <= ring; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != ring) {
                        continue;
                    }
                    int x = from.getX() + i * step;
                    int z = from.getZ() + j * step;
                    int y = terrain.applyAsInt(x, z);
                    pos.set(x, y, z);
                    int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(y), qz = QuartPos.fromBlock(z);
                    var biome = generator.getBiomeSource().getNoiseBiome(qx, qy, qz, sampler);
                    Climate.TargetPoint point = sampler.sample(qx, qy, qz);
                    var noise = new ClimateModel.Noise(
                        Climate.unquantizeCoord(point.temperature()),
                        Climate.unquantizeCoord(point.humidity()),
                        Climate.unquantizeCoord(point.continentalness())
                    );
                    var at = new ClimateModel.Conditions(y, season, waterSeason, dayTime, 0, 0, 0, 0, false, null);
                    var base = ClimateModel.breakdown(level, pos, biome, noise, at);
                    var weather = ClimateWeather.at(level.getSeed(), x, z, y, gameTime, base,
                        biome.value().hasPrecipitation(), terrain);
                    var climate = ClimateModel.breakdown(level, pos, biome, noise,
                        at.withWeather(weather.precipitation(), weather.thunder(), weather.cloud()));
                    double celsius = ClimatePrecipitation.celsius(biome, climate);
                    boolean wet = weather.precipitation() >= 0.05;
                    boolean match = switch (kind) {
                        case RAIN -> wet && celsius >= c.snowCelsius();
                        case SNOW -> wet && celsius < c.snowCelsius();
                        case STORM -> weather.thunder() > 0.3;
                        case FOG -> weather.fog() > 0.3;
                        case CLEAR -> weather.cloud() < 0.15;
                    };
                    if (!match) {
                        continue;
                    }
                    double distance = Math.hypot(x - from.getX(), z - from.getZ());
                    if (best == null || distance < best.distance()) {
                        best = new Found(pos.immutable(), distance, weather, climate.airCelsius());
                    }
                }
            }
            if (best != null) {
                return Optional.of(best);
            }
        }
        return Optional.empty();
    }
}
