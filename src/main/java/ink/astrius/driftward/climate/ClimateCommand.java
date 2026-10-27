package ink.astrius.driftward.climate;

import com.github.thedeathlycow.thermoo.api.environment.EnvironmentLookup;
import com.github.thedeathlycow.thermoo.api.environment.component.EnvironmentComponentTypes;
import com.github.thedeathlycow.thermoo.api.util.TemperatureUnit;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ink.astrius.driftward.reg.ModLoaded;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

// /driftward climate probe [pos]   every term of the model at a position, plus what Thermoo applies
// /driftward climate year [pos]    day and night temperatures through the year
// /driftward climate map <radius> [step] [layer] [season] [time]   PNG of the model over an area
// /driftward climate reload        re-read the climate config section
public final class ClimateCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("driftward/climate");
    private static final SimpleCommandExceptionType NOT_SERVER_LEVEL =
        new SimpleCommandExceptionType(Component.literal("Climate commands need a world with climate noise"));
    private static final String[] TIMES = {"now", "day", "night", "average"};
    private static final String[] SEASON_KEYWORDS = {"now", "annual", "warmest", "coldest"};
    private static final int MAX_SIZE = 2048;
    // Map rendering gets its own low-priority pool with half the cores; parallel streams inside a
    // task of this pool stay in it
    private static final ForkJoinPool MAP_POOL = new ForkJoinPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
        pool -> {
            var thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName("Driftward climate map " + thread.getPoolIndex());
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        },
        null, false
    );
    private static final AtomicBoolean RENDERING = new AtomicBoolean();

    private ClimateCommand() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> register(event.getDispatcher()));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var layer = Commands.argument("layer", StringArgumentType.word())
            .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                Arrays.stream(ClimateMap.Layer.values()).map(ClimateMap.Layer::id), b));
        var season = Commands.argument("season", StringArgumentType.word())
            .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                Stream.concat(Arrays.stream(SEASON_KEYWORDS), Arrays.stream(ClimateSeasons.SUB_SEASONS)), b));
        var time = Commands.argument("time", StringArgumentType.word())
            .suggests((ctx, b) -> SharedSuggestionProvider.suggest(TIMES, b));

        dispatcher.register(Commands.literal("driftward").then(Commands.literal("climate")
            .requires(s -> s.hasPermission(2))
            .then(Commands.literal("probe")
                .executes(ctx -> probe(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition())))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> probe(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
            .then(Commands.literal("year")
                .executes(ctx -> year(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition())))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> year(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
            .then(Commands.literal("map")
                .then(Commands.argument("radius", IntegerArgumentType.integer(16, 16384))
                    .executes(ClimateCommand::map)
                    .then(Commands.argument("step", IntegerArgumentType.integer(1, 512))
                        .executes(ClimateCommand::map)
                        .then(layer
                            .executes(ClimateCommand::map)
                            .then(season
                                .executes(ClimateCommand::map)
                                .then(time
                                    .executes(ClimateCommand::map)
                                    .then(Commands.argument("ahead", IntegerArgumentType.integer(-240, 240))
                                        .executes(ClimateCommand::map))))))))
            .then(Commands.literal("surface")
                .executes(ctx -> surface(ctx.getSource(), 4))
                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 32))
                    .executes(ctx -> surface(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("reload").executes(ctx -> {
                ClimateConstants.reload();
                ClimateModel.clearCache();
                ctx.getSource().sendSuccess(() -> Component.literal("Climate constants reloaded from the server config"), true);
                return 1;
            }))
        ));
    }

    private static ServerLevel level(CommandSourceStack source) throws CommandSyntaxException {
        if (ClimateModel.sample(source.getLevel(), BlockPos.ZERO) == null) {
            throw NOT_SERVER_LEVEL.create();
        }
        return source.getLevel();
    }

    // Probe

    private static int probe(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        var level = level(source);
        var biome = level.getBiome(pos);
        var b = ClimateModel.breakdown(level, pos, biome);
        var c = ClimateConstants.get();
        var out = Component.empty();

        out.append(header("Climate at %d %d %d".formatted(pos.getX(), pos.getY(), pos.getZ())))
            .append(" ").append(Component.literal(id(biome)).withStyle(ChatFormatting.GRAY));
        if (b.noise() != null) {
            line(out, "noise", "temperature %.3f, humidity %.3f, continentalness %.3f",
                b.noise().temperature(), b.noise().humidity(), b.noise().continentalness());
        }
        line(out, "humidity", "index %.2f, dryness %.2f, relative %.0f%%",
            b.humidity(), b.dryness(), b.relativeHumidity() * 100);
        line(out, "annual mean", "%s base%s%s%s → %s",
            c(b.baseMean()),
            b.offset() != 0 ? ", offset " + signed(b.offset()) : "",
            b.lapse() != 0 ? ", altitude " + signed(-b.lapse()) : "",
            b.cave() > 0 ? ", cave %.0f%% at depth %.0f".formatted(b.cave() * 100, b.depth()) : "",
            c(b.mean()));
        var year = ClimateSeasons.year(level);
        line(out, "season", "swing ±%.1f (warmth %.0f%%) × %.2f%s → %s",
            b.amplitude(), b.warmth() * 100, b.season(),
            year.isPresent() ? " [%s, %.3f]".formatted(ClimateSeasons.subSeason(year.getAsDouble()), year.getAsDouble()) : "",
            signed(b.seasonal()));
        line(out, "day", "swing ±%.1f × %.2f (time %d) → %s",
            b.diurnalAmplitude(), b.day(), level.getDayTime() % 24000, signed(b.diurnal()));
        line(out, "shelter", "%.0f%%", b.shelter() * 100);
        var w = ClimateModel.weather(level, pos, biome, b.noise(), ClimateModel.live(level, pos, biome, false));
        line(out, "weather", "pressure %+.0f hPa, wind %.0f m/s from %.0f°, cloud %.0f%%, precipitation %.0f%%, thunder %.0f%%, fog %.0f%%",
            w.pressure(), w.windSpeed(), w.windFrom(), w.cloud() * 100, w.precipitation() * 100, w.thunder() * 100, w.fog() * 100);
        line(out, "rain", "vanilla rain %.2f, thunder %.2f%s → %s",
            level.getRainLevel(1), level.getThunderLevel(1),
            ClimateConstants.get().weatherDrivesRain() ? " (model uses the weather layer)" : "", signed(b.weather()));
        line(out, "wind", "%s", signed(b.wind()));
        line(out, "pooling", "valley %.0f%% → %s", b.valley() * 100, signed(b.pooling()));
        line(out, "air", "%s", c(b.airCelsius()));
        if (b.inWater()) {
            line(out, "water", "%s", c(b.waterCelsius()));
        }
        out.append("\n").append(Component.literal("model ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(c(b.celsius())).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        if (ModLoaded.THERMOO) {
            out.append(Component.literal("   Thermoo ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(ThermooReadout.read(level, pos)).withStyle(ChatFormatting.WHITE));
        }
        double vanilla = ClimatePrecipitation.vanillaTemperature(level, biome, pos);
        line(out, "precipitation", "%s (vanilla-scale temperature %.2f, snow below %s)",
            !biome.value().hasPrecipitation() ? "none" : !Double.isNaN(vanilla) && vanilla < 0.15 ? "snow" : "rain",
            Double.isNaN(vanilla) ? biome.value().getBaseTemperature() : vanilla, c(c.snowCelsius()));

        source.sendSuccess(() -> out, false);
        return (int) Math.round(b.celsius());
    }

    // Surface: the chunk-load snow and ice catch-up, run now on loaded chunks around the source

    private static int surface(CommandSourceStack source, int radius) throws CommandSyntaxException {
        var level = level(source);
        var center = new net.minecraft.world.level.ChunkPos(BlockPos.containing(source.getPosition()));
        int chunks = 0, blocks = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                var chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                if (chunk == null) {
                    continue;
                }
                int changed = ClimateSurface.patch(level, chunk);
                if (changed > 0) {
                    // Loaded chunks are already on clients
                    level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false)
                        .forEach(p -> p.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
                            chunk, level.getLightEngine(), null, null)));
                }
                chunks++;
                blocks += changed;
            }
        }
        int c = chunks, b = blocks;
        source.sendSuccess(() -> Component.literal("Snow and ice brought in line with the climate: %d blocks in %d chunks".formatted(b, c)), true);
        return blocks;
    }

    // Year

    private static int year(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        var level = level(source);
        var c = ClimateConstants.get();
        var biome = level.getBiome(pos);
        var noise = ClimateModel.sample(level, pos);
        var live = ClimateModel.live(level, pos, biome, false);
        var out = Component.empty();
        out.append(header("Year at %d %d %d".formatted(pos.getX(), pos.getY(), pos.getZ())))
            .append(" ").append(Component.literal(id(biome) + ", clear weather").withStyle(ChatFormatting.GRAY));
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (var name : ClimateSeasons.SUB_SEASONS) {
            double y = ClimateSeasons.yearOf(name);
            double season = ClimateSeasons.factorAt(y, c.seasonPeak());
            double water = ClimateSeasons.factorAt(y - c.waterLag(), c.seasonPeak());
            double day = at(level, pos, biome, noise, live, season, water, (long) c.diurnalPeakTick()).airCelsius();
            var night = at(level, pos, biome, noise, live, season, water, (long) c.diurnalPeakTick() + 12000);
            lo = Math.min(lo, night.airCelsius());
            hi = Math.max(hi, day);
            out.append("\n").append(Component.literal("%-13s".formatted(name)).withStyle(ChatFormatting.GRAY))
                .append(colored(day)).append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY))
                .append(colored(night.airCelsius()))
                .append(Component.literal("   water " + c(night.waterCelsius())).withStyle(ChatFormatting.DARK_AQUA));
        }
        double range = hi - lo;
        out.append("\n").append(Component.literal("day / night; extremes %s .. %s (%.1f range)".formatted(c(lo), c(hi), range))
            .withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> out, false);
        return 1;
    }

    private static ClimateModel.Breakdown at(
        ServerLevel level, BlockPos pos, net.minecraft.core.Holder<Biome> biome, ClimateModel.Noise noise,
        ClimateModel.Conditions live, double season, double water, long time
    ) {
        var at = new ClimateModel.Conditions(
            live.surface(), season, water, time, 0, 0, 0, live.shelter(), live.inWater(), live.terrain()
        );
        return ClimateModel.breakdown(level, pos, biome, noise, at);
    }

    // Map

    private static int map(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var level = level(source);
        var c = ClimateConstants.get();
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int step = optional(ctx, "step", -1, Integer.class);
        if (step < 0) {
            step = Math.max(1, (int) Math.ceil(radius * 2.0 / 512));
        }
        if (radius * 2 / step + 1 > MAX_SIZE) {
            step = (int) Math.ceil(radius * 2.0 / (MAX_SIZE - 1));
        }
        var layerName = optional(ctx, "layer", "temperature", String.class);
        var layer = Arrays.stream(ClimateMap.Layer.values()).filter(l -> l.id().equals(layerName)).findFirst()
            .orElseThrow(() -> new SimpleCommandExceptionType(Component.literal("Unknown layer " + layerName)).create());
        var seasonName = optional(ctx, "season", "now", String.class);
        var timeName = optional(ctx, "time", "now", String.class);

        double season = switch (seasonName) {
            case "now" -> ClimateSeasons.factor(level, c.seasonPeak());
            case "annual" -> 0;
            case "warmest" -> 1;
            case "coldest" -> -1;
            default -> {
                double y = ClimateSeasons.yearOf(seasonName);
                if (y < 0) {
                    throw new SimpleCommandExceptionType(Component.literal("Unknown season " + seasonName)).create();
                }
                yield ClimateSeasons.factorAt(y, c.seasonPeak());
            }
        };
        long time = switch (timeName) {
            case "now" -> level.getDayTime();
            case "day" -> (long) c.diurnalPeakTick();
            case "night" -> (long) c.diurnalPeakTick() + 12000;
            case "average" -> (long) c.diurnalPeakTick() + 6000;
            default -> throw new SimpleCommandExceptionType(Component.literal("Unknown time " + timeName)).create();
        };
        int ahead = optional(ctx, "ahead", 0, Integer.class);
        var moment = new ClimateMap.Moment(seasonName + "-" + timeName + (ahead != 0 ? "-plus" + ahead + "h" : ""),
            season, time + ahead * 1000L, level.getGameTime() + ahead * 1000L);

        var center = BlockPos.containing(source.getPosition());
        int size = radius * 2 / step + 1;
        int finalStep = step;
        source.sendSuccess(() -> Component.literal("Rendering a %d×%d %s map (%d blocks per pixel)..."
            .formatted(size, size, layer.id(), finalStep)).withStyle(ChatFormatting.GRAY), false);
        if (!RENDERING.compareAndSet(false, true)) {
            throw new SimpleCommandExceptionType(Component.literal("A climate map is already rendering")).create();
        }
        long start = System.nanoTime();
        // Not Util.backgroundExecutor(): that pool generates chunks, and a map would starve it
        CompletableFuture.supplyAsync(() -> {
            try {
                return ClimateMap.render(level, center, radius, finalStep, layer, moment);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                RENDERING.set(false);
            }
        }, MAP_POOL).whenComplete((result, error) -> level.getServer().execute(() -> {
            if (error != null) {
                LOGGER.error("Climate map failed", error);
                source.sendFailure(Component.literal("Climate map failed: " + error.getCause()));
                return;
            }
            double seconds = (System.nanoTime() - start) / 1e9;
            LOGGER.info("Climate map {} ({}, {}x{}, {} s): {} .. {}, average {}", result.file(), moment.name(),
                result.width(), result.height(), "%.1f".formatted(seconds),
                "%.2f".formatted(result.min()), "%.2f".formatted(result.max()), "%.2f".formatted(result.average()));
            var out = Component.empty().append(header("Climate map"))
                .append(Component.literal(" %s, %s, %.1f s".formatted(layer.id(), moment.name(), seconds)).withStyle(ChatFormatting.GRAY));
            if (layer != ClimateMap.Layer.BIOME) {
                String unit = switch (layer) {
                    case PRESSURE -> " hPa";
                    case WIND -> " m/s";
                    case HUMIDITY, DRYNESS, CLOUD, PRECIPITATION, FOG -> "";
                    default -> " °C";
                };
                line(out, "range", "%.2f%s .. %.2f%s, average %.2f%s", result.min(), unit, result.max(), unit, result.average(), unit);
            }
            line(out, "below snow line", "%.0f%% of the area", result.belowSnow() * 100);
            if (layer == ClimateMap.Layer.TEMPERATURE || layer == ClimateMap.Layer.MEAN) {
                line(out, "legend", "−30 .. 45 °C left to right, ticks every 10 °C, long tick at 0; dark contour = snow line");
            }
            var path = result.file().toString();
            out.append("\n").append(Component.literal(path).withStyle(s -> s
                .withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open the map")))));
            source.sendSuccess(() -> out, false);
        }));
        return 1;
    }

    private static <T> T optional(CommandContext<CommandSourceStack> ctx, String name, T fallback, Class<T> type) {
        try {
            return ctx.getArgument(name, type);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // Formatting

    private static MutableComponent header(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD);
    }

    private static void line(MutableComponent out, String label, String format, Object... args) {
        out.append("\n").append(Component.literal(label + " ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.format(Locale.ROOT, format, args)));
    }

    private static Component colored(double celsius) {
        return Component.literal("%6s".formatted(c(celsius)))
            .withStyle(s -> s.withColor(ClimateMap.temperatureColor(celsius)));
    }

    private static String c(double celsius) {
        return String.format(Locale.ROOT, "%.1f °C", celsius);
    }

    private static String signed(double celsius) {
        return String.format(Locale.ROOT, "%+.1f", celsius);
    }

    private static String id(net.minecraft.core.Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }

    // Separate class so that Thermoo is only loaded when present
    private static final class ThermooReadout {
        static String read(ServerLevel level, BlockPos pos) {
            var components = EnvironmentLookup.getInstance().findEnvironmentComponents(level, pos);
            var temperature = components.get(EnvironmentComponentTypes.TEMPERATURE);
            var humidity = components.get(EnvironmentComponentTypes.RELATIVE_HUMIDITY);
            return (temperature == null ? "no temperature" : c(temperature.valueInUnit(TemperatureUnit.CELSIUS)))
                + (humidity == null ? "" : ", %.0f%% humidity".formatted(humidity * 100));
        }
    }
}
