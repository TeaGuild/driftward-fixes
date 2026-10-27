package ink.astrius.driftward.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.IntStream;

// Renders a layer of the climate model over an area to a PNG, straight from the world generator
// (no chunks are loaded, so it works far beyond explored land). Safe to run off the server thread.
public final class ClimateMap {
    public enum Layer {
        TEMPERATURE, MEAN, AMPLITUDE, HUMIDITY, DRYNESS, SNOW, BIOME,
        PRESSURE, WIND, CLOUD, PRECIPITATION, FOG;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    // What the map shows: one moment of the year and day, with clear or current weather
    // `gameTime` places the weather; the season and time of day can differ from it
    public record Moment(String name, double season, long dayTime, long gameTime) {}

    public record Result(Path file, int width, int height, double min, double max, double average, double belowSnow) {}

    private static final int LEGEND = 14;
    private static final int HEIGHT_SPACING = 32;
    private static final int MAX_HEIGHT_SAMPLES = 160;
    private static final int WEATHER_SPACING = 96;

    private ClimateMap() {}

    public static Result render(ServerLevel level, BlockPos center, int radius, int step, Layer layer, Moment moment)
        throws IOException {
        var c = ClimateConstants.get();
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        var random = source.randomState();
        var sampler = random.sampler();
        boolean deeperDown = level.dimension().location().equals(ClimateModel.DEEPER_DOWN);

        int size = radius * 2 / step + 1;
        int x0 = center.getX() - radius;
        int z0 = center.getZ() - radius;

        // Heights first: pooling reads the neighbourhood from this grid. A full noise column per pixel
        // is slow (Terralith), so heights come from a coarser grid, bilinear in between; the model
        // only needs them to a few blocks.
        // A full noise column per sample is the expensive part of a map, so big maps space them wider
        int hs = Math.max(1, Math.min(
            Math.max(HEIGHT_SPACING / step, (size - 1 + MAX_HEIGHT_SAMPLES - 1) / MAX_HEIGHT_SAMPLES), size - 1));
        int coarse = (size - 1) / hs + 2;
        var coarseHeights = new int[coarse * coarse];
        IntStream.range(0, coarse).parallel().forEach(a -> {
            for (int b = 0; b < coarse; b++) {
                coarseHeights[a * coarse + b] = deeperDown ? center.getY()
                    : generator.getBaseHeight(x0 + a * hs * step, z0 + b * hs * step, Heightmap.Types.WORLD_SURFACE_WG, level, random);
            }
        });
        var heights = new int[size * size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int a = i / hs, b = j / hs;
                double fa = (double) (i % hs) / hs, fb = (double) (j % hs) / hs;
                double h = Mth.lerp(fb,
                    Mth.lerp(fa, coarseHeights[a * coarse + b], coarseHeights[(a + 1) * coarse + b]),
                    Mth.lerp(fa, coarseHeights[a * coarse + b + 1], coarseHeights[(a + 1) * coarse + b + 1]));
                heights[i * size + j] = (int) Math.round(h);
            }
        }
        java.util.function.IntBinaryOperator terrain = (x, z) -> {
            int i = Mth.clamp(Math.round((x - x0) / (float) step), 0, size - 1);
            int j = Mth.clamp(Math.round((z - z0) / (float) step), 0, size - 1);
            return heights[i * size + j];
        };

        // Weather varies over kilometres: compute its smooth part on a coarse grid, interpolate, and let
        // each pixel's biome decide whether anything falls
        int ws = Math.max(1, Math.min(WEATHER_SPACING / step, size - 1));
        int wc = (size - 1) / ws + 2;
        var coarseWeather = new ClimateWeather.Raw[wc * wc];
        IntStream.range(0, wc).parallel().forEach(a -> {
            var p = new BlockPos.MutableBlockPos();
            for (int b = 0; b < wc; b++) {
                int x = x0 + a * ws * step;
                int z = z0 + b * ws * step;
                int y = terrain.applyAsInt(x, z);
                p.set(x, y, z);
                int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(y), qz = QuartPos.fromBlock(z);
                var biome = generator.getBiomeSource().getNoiseBiome(qx, qy, qz, sampler);
                Climate.TargetPoint point = sampler.sample(qx, qy, qz);
                var noise = new ClimateModel.Noise(
                    Climate.unquantizeCoord(point.temperature()),
                    Climate.unquantizeCoord(point.humidity()),
                    Climate.unquantizeCoord(point.continentalness())
                );
                var at = new ClimateModel.Conditions(
                    y, moment.season(), moment.season(), moment.dayTime(), 0, 0, 0, 0, false, terrain
                );
                var base = ClimateModel.breakdown(level, p, biome, noise, at);
                coarseWeather[a * wc + b] = ClimateWeather.at(level.getSeed(), x, z, y, moment.gameTime(), base, true, terrain).raw();
            }
        });

        var values = new double[size * size];
        var celsius = new double[size * size];
        @SuppressWarnings("unchecked")
        Holder<Biome>[] biomes = new Holder[size * size];
        var weather = new ClimateWeather.State[size * size];
        IntStream.range(0, size).parallel().forEach(i -> {
            var pos = new BlockPos.MutableBlockPos();
            for (int j = 0; j < size; j++) {
                int k = i * size + j;
                int x = x0 + i * step;
                int z = z0 + j * step;
                int y = heights[k];
                pos.set(x, y, z);
                int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(y), qz = QuartPos.fromBlock(z);
                var biome = generator.getBiomeSource().getNoiseBiome(qx, qy, qz, sampler);
                Climate.TargetPoint point = sampler.sample(qx, qy, qz);
                var noise = new ClimateModel.Noise(
                    Climate.unquantizeCoord(point.temperature()),
                    Climate.unquantizeCoord(point.humidity()),
                    Climate.unquantizeCoord(point.continentalness())
                );
                var at = new ClimateModel.Conditions(
                    y, moment.season(), moment.season(), moment.dayTime(), 0, 0, 0, 0, false, terrain
                );
                int a = i / ws, bb = j / ws;
                double fa = (double) (i % ws) / ws, fb = (double) (j % ws) / ws;
                var raw = ClimateWeather.Raw.lerp(fb,
                    ClimateWeather.Raw.lerp(fa, coarseWeather[a * wc + bb], coarseWeather[(a + 1) * wc + bb]),
                    ClimateWeather.Raw.lerp(fa, coarseWeather[a * wc + bb + 1], coarseWeather[(a + 1) * wc + bb + 1]));
                var w = ClimateWeather.finish(raw, biome.value().hasPrecipitation());
                weather[k] = w;
                var b = ClimateModel.breakdown(level, pos, biome, noise, at.withWeather(w.precipitation(), w.thunder(), w.cloud()));
                biomes[k] = biome;
                celsius[k] = ClimatePrecipitation.celsius(biome, b);
                values[k] = switch (layer) {
                    case TEMPERATURE, BIOME -> b.airCelsius();
                    case SNOW -> celsius[k];
                    case MEAN -> b.mean();
                    case AMPLITUDE -> b.amplitude();
                    case HUMIDITY -> b.relativeHumidity();
                    case DRYNESS -> b.dryness();
                    case PRESSURE -> w.pressure();
                    case WIND -> w.windSpeed();
                    case CLOUD -> w.cloud();
                    case PRECIPITATION -> w.precipitation();
                    case FOG -> w.fog();
                };
            }
        });

        var image = new BufferedImage(size, size + LEGEND, BufferedImage.TYPE_INT_RGB);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;
        int below = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int k = i * size + j;
                double v = values[k];
                min = Math.min(min, v);
                max = Math.max(max, v);
                sum += v;
                if (celsius[k] < c.snowCelsius()) {
                    below++;
                }
                int rgb = switch (layer) {
                    case TEMPERATURE, MEAN -> temperatureColor(v);
                    case AMPLITUDE -> ramp(v / 20, 0xf3eef8, 0x9e9ac8, 0x3f007d);
                    case HUMIDITY -> ramp(v, 0x8c6d3f, 0xe8dcb0, 0x4daf4a, 0x2166ac);
                    case DRYNESS -> ramp(v, 0x4daf4a, 0xfee08b, 0xd73027);
                    case SNOW -> celsius[k] < c.snowCelsius() ? 0xeef6ff : 0x5b8c3a;
                    case BIOME -> biomeColor(biomes[k]);
                    case PRESSURE -> ramp((v + 18) / 36, 0x2c4fb5, 0xf2f2f2, 0xd7301f);
                    case WIND -> ramp(v / 20, 0xf7fbff, 0x6baed6, 0x08306b, 0x7a0177);
                    case CLOUD -> ramp(v, 0x3a7bd5, 0xd9d9d9, 0x525252);
                    case PRECIPITATION -> v <= 0 ? 0xf2efe6 : ramp(v, 0xa6dba0, 0x1b7837, 0x40004b);
                    case FOG -> ramp(v, 0x1f3b4d, 0xbdd7e7, 0xffffff);
                };
                // Biome borders, slightly darker, for orientation
                boolean border = i + 1 < size && biomes[k] != biomes[k + size]
                    || j + 1 < size && biomes[k] != biomes[k + 1];
                boolean weatherLayer = layer == Layer.PRESSURE || layer == Layer.WIND || layer == Layer.CLOUD
                    || layer == Layer.PRECIPITATION || layer == Layer.FOG;
                if (border && layer != Layer.BIOME && !weatherLayer) {
                    rgb = shade(rgb, 0.8);
                }
                // The snow line as a dark contour on temperature maps
                if (layer == Layer.TEMPERATURE && isContour(celsius, size, i, j, c.snowCelsius())) {
                    rgb = 0x202020;
                }
                image.setRGB(i, j, rgb);
            }
        }
        // Centre marker
        int m = size / 2;
        for (int d = -3; d <= 3; d++) {
            image.setRGB(Mth.clamp(m + d, 0, size - 1), m, 0xff00ff);
            image.setRGB(m, Mth.clamp(m + d, 0, size - 1), 0xff00ff);
        }
        if (layer == Layer.WIND || layer == Layer.PRESSURE) {
            drawArrows(image, size, weather);
        }
        if (layer == Layer.PRECIPITATION || layer == Layer.CLOUD) {
            for (int k = 0; k < size * size; k++) {
                if (weather[k].thunder() > 0.3 && (k / size + k % size) % 3 == 0) {
                    image.setRGB(k / size, k % size, 0xffd700);
                }
            }
        }
        drawLegend(image, size, layer);

        var dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("driftward").resolve("climate");
        Files.createDirectories(dir);
        var file = dir.resolve(String.format(Locale.ROOT, "%s-%s-%d_%d-r%d.png",
            layer.id(), moment.name(), center.getX(), center.getZ(), radius));
        ImageIO.write(image, "png", file.toFile());
        return new Result(file.toAbsolutePath().normalize(), size, size, min, max, sum / (size * size),
            (double) below / (size * size));
    }

    // Wind direction every 24 pixels: a line pointing downwind with a dot at its head
    private static void drawArrows(BufferedImage image, int size, ClimateWeather.State[] weather) {
        for (int i = 12; i < size; i += 24) {
            for (int j = 12; j < size; j += 24) {
                var w = weather[i * size + j];
                double speed = w.windSpeed();
                if (speed < 0.5) {
                    continue;
                }
                double len = Math.min(11, 3 + speed * 0.5);
                double dx = w.windX() / speed, dz = w.windZ() / speed;
                for (double t = -len; t <= len; t += 0.5) {
                    int px = (int) Math.round(i + dx * t), pz = (int) Math.round(j + dz * t);
                    if (px >= 0 && pz >= 0 && px < size && pz < size) {
                        image.setRGB(px, pz, 0x111111);
                    }
                }
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        int px = (int) Math.round(i + dx * len) + a, pz = (int) Math.round(j + dz * len) + b;
                        if (px >= 0 && pz >= 0 && px < size && pz < size) {
                            image.setRGB(px, pz, 0x111111);
                        }
                    }
                }
            }
        }
    }

    private static boolean isContour(double[] celsius, int size, int i, int j, double line) {
        boolean here = celsius[i * size + j] < line;
        return i + 1 < size && (celsius[(i + 1) * size + j] < line) != here
            || j + 1 < size && (celsius[i * size + j + 1] < line) != here;
    }

    // Temperature scale shared by the map and its legend, −30 .. 45 °C
    private static final double[] T_STOPS = {-30, -20, -10, 0, 0.01, 6, 12, 20, 28, 36, 45};
    private static final int[] T_COLORS = {
        0x3b1d6e, 0x2c4fb5, 0x3a8fd6, 0xbfe3f2, 0xd9efc9, 0x9fd49a, 0x5cb85c, 0xe6d84a, 0xf08c2e, 0xd7301f, 0x7a0000,
    };

    public static int temperatureColor(double celsius) {
        if (celsius <= T_STOPS[0]) {
            return T_COLORS[0];
        }
        for (int i = 1; i < T_STOPS.length; i++) {
            if (celsius <= T_STOPS[i]) {
                double f = (celsius - T_STOPS[i - 1]) / (T_STOPS[i] - T_STOPS[i - 1]);
                return mix(T_COLORS[i - 1], T_COLORS[i], f);
            }
        }
        return T_COLORS[T_COLORS.length - 1];
    }

    private static void drawLegend(BufferedImage image, int width, Layer layer) {
        for (int x = 0; x < width; x++) {
            double f = (double) x / Math.max(1, width - 1);
            int rgb = switch (layer) {
                case TEMPERATURE, MEAN -> temperatureColor(-30 + 75 * f);
                case AMPLITUDE -> ramp(f, 0xf3eef8, 0x9e9ac8, 0x3f007d);
                case HUMIDITY -> ramp(f, 0x8c6d3f, 0xe8dcb0, 0x4daf4a, 0x2166ac);
                case DRYNESS -> ramp(f, 0x4daf4a, 0xfee08b, 0xd73027);
                case SNOW, BIOME -> 0x000000;
                case PRESSURE -> ramp(f, 0x2c4fb5, 0xf2f2f2, 0xd7301f);
                case WIND -> ramp(f, 0xf7fbff, 0x6baed6, 0x08306b, 0x7a0177);
                case CLOUD -> ramp(f, 0x3a7bd5, 0xd9d9d9, 0x525252);
                case PRECIPITATION -> ramp(f, 0xa6dba0, 0x1b7837, 0x40004b);
                case FOG -> ramp(f, 0x1f3b4d, 0xbdd7e7, 0xffffff);
            };
            for (int y = 0; y < LEGEND; y++) {
                image.setRGB(x, image.getHeight() - LEGEND + y, y < 2 ? 0x000000 : rgb);
            }
        }
        // Ticks every 10 °C on temperature legends, longer at 0
        if (layer == Layer.TEMPERATURE || layer == Layer.MEAN) {
            for (int t = -30; t <= 45; t += 10) {
                int x = (int) Math.round((t + 30) / 75.0 * (width - 1));
                int len = t == 0 ? LEGEND : LEGEND / 2;
                for (int y = 0; y < len; y++) {
                    image.setRGB(Mth.clamp(x, 0, width - 1), image.getHeight() - y - 1, 0x000000);
                }
            }
        }
    }

    private static int biomeColor(Holder<Biome> biome) {
        int h = biome.unwrapKey().map(k -> k.location().toString().hashCode()).orElse(0);
        float hue = (h & 0xffff) / 65536f;
        return java.awt.Color.HSBtoRGB(hue, 0.45f + ((h >>> 16) & 0xff) / 255f * 0.3f, 0.85f) & 0xffffff;
    }

    private static int ramp(double f, int... colors) {
        f = Mth.clamp(f, 0, 1) * (colors.length - 1);
        int i = Math.min((int) f, colors.length - 2);
        return mix(colors[i], colors[i + 1], f - i);
    }

    private static int mix(int a, int b, double f) {
        int r = (int) Math.round(Mth.lerp(f, (a >> 16) & 0xff, (b >> 16) & 0xff));
        int g = (int) Math.round(Mth.lerp(f, (a >> 8) & 0xff, (b >> 8) & 0xff));
        int bl = (int) Math.round(Mth.lerp(f, a & 0xff, b & 0xff));
        return r << 16 | g << 8 | bl;
    }

    private static int shade(int rgb, double f) {
        return mix(0, rgb, f);
    }
}
