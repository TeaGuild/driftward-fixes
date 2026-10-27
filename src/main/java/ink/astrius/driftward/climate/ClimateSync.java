package ink.astrius.driftward.climate;

import ink.astrius.driftward.Driftward;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// The client has no world seed, so it can't run the model. The server sends each player a grid of
// the climate around them: outdoor air temperature at the surface (for snow or rain) and the
// weather (whether anything falls, cloud, fog), plus the weather right where the player is (sky
// darkness, rain intensity, fog).
public record ClimateSync(
    int originX, int originZ, int spacing, int size, float lapse, float[] celsius, short[] surface,
    byte[] precipitation, byte[] thunder, byte[] cloud, byte[] fog,
    float hereRain, float hereThunder, float hereCloud, float hereFog, float hereCloudBase
) implements CustomPacketPayload {
    public static final Type<ClimateSync> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "climate_grid"));

    public static final StreamCodec<FriendlyByteBuf, ClimateSync> STREAM_CODEC = StreamCodec.of(
        (buf, grid) -> {
            buf.writeVarInt(grid.originX);
            buf.writeVarInt(grid.originZ);
            buf.writeVarInt(grid.spacing);
            buf.writeVarInt(grid.size);
            buf.writeFloat(grid.lapse);
            for (int i = 0; i < grid.size * grid.size; i++) {
                buf.writeFloat(grid.celsius[i]);
                buf.writeShort(grid.surface[i]);
                buf.writeByte(grid.precipitation[i]);
                buf.writeByte(grid.thunder[i]);
                buf.writeByte(grid.cloud[i]);
                buf.writeByte(grid.fog[i]);
            }
            buf.writeFloat(grid.hereRain);
            buf.writeFloat(grid.hereThunder);
            buf.writeFloat(grid.hereCloud);
            buf.writeFloat(grid.hereFog);
            buf.writeFloat(grid.hereCloudBase);
        },
        buf -> {
            int originX = buf.readVarInt();
            int originZ = buf.readVarInt();
            int spacing = buf.readVarInt();
            int size = Mth.clamp(buf.readVarInt(), 1, 64);
            float lapse = buf.readFloat();
            int n = size * size;
            var celsius = new float[n];
            var surface = new short[n];
            var precipitation = new byte[n];
            var thunder = new byte[n];
            var cloud = new byte[n];
            var fog = new byte[n];
            for (int i = 0; i < n; i++) {
                celsius[i] = buf.readFloat();
                surface[i] = buf.readShort();
                precipitation[i] = buf.readByte();
                thunder[i] = buf.readByte();
                cloud[i] = buf.readByte();
                fog[i] = buf.readByte();
            }
            return new ClimateSync(originX, originZ, spacing, size, lapse, celsius, surface,
                precipitation, thunder, cloud, fog, buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat());
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClimateSync::registerPayloads);
        NeoForge.EVENT_BUS.addListener(ClimateSync::onPlayerTick);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Optional: clients without the mod just keep Serene Seasons' own behaviour
        event.registrar("3").optional().playToClient(TYPE, STREAM_CODEC, ClimateSync::handle);
    }

    private static void handle(ClimateSync grid, IPayloadContext ctx) {
        Grid.receive(grid);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var c = ClimateConstants.get();
        if (player.tickCount % c.syncInterval() != 0 || !ClimatePrecipitation.applies(player.level())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, sample(player.serverLevel(), player.blockPosition()));
    }

    private static byte unit(double value) {
        return (byte) Math.round(Mth.clamp(value, 0, 1) * 255);
    }

    static ClimateSync sample(ServerLevel level, BlockPos center) {
        var c = ClimateConstants.get();
        int spacing = c.syncSpacing();
        int size = c.syncRadius() * 2 + 1;
        int originX = Math.floorDiv(center.getX(), spacing) * spacing - c.syncRadius() * spacing;
        int originZ = Math.floorDiv(center.getZ(), spacing) * spacing - c.syncRadius() * spacing;
        int n = size * size;
        var celsius = new float[n];
        var surface = new short[n];
        var precipitation = new byte[n];
        var thunder = new byte[n];
        var cloud = new byte[n];
        var fog = new byte[n];
        var pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int x = originX + i * spacing;
                int z = originZ + j * spacing;
                int k = i * size + j;
                pos.set(x, center.getY(), z);
                if (!level.isLoaded(pos)) {
                    celsius[k] = Float.NaN;
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                pos.setY(y);
                var biome = level.getBiome(pos);
                var noise = ClimateModel.sample(level, pos);
                var at = ClimateModel.live(level, pos, biome, true);
                var breakdown = ClimateModel.breakdown(level, pos, biome, noise, at);
                var weather = ClimateModel.weather(level, pos, biome, noise, at);
                celsius[k] = (float) ClimatePrecipitation.celsius(biome, breakdown);
                surface[k] = (short) y;
                precipitation[k] = unit(weather.precipitation());
                thunder[k] = unit(weather.thunder());
                cloud[k] = unit(weather.cloud());
                fog[k] = unit(weather.fog());
            }
        }
        var here = ClimateLocalWeather.at(level, center);
        return new ClimateSync(originX, originZ, spacing, size, (float) c.lapsePerBlock(), celsius, surface,
            precipitation, thunder, cloud, fog,
            (float) here.precipitation(), (float) here.thunder(), (float) here.cloud(), (float) here.fog(),
            (float) here.cloudBase());
    }

    // The last grid the client received
    public static final class Grid {
        static volatile ClimateSync current;
        // Weather at the player, eased from the previous update over a couple of seconds
        private static volatile float[] from = new float[5];
        private static volatile long receivedAt;

        private Grid() {}

        public static boolean present() {
            return current != null;
        }

        static void receive(ClimateSync grid) {
            from = new float[] {here(0), here(1), here(2), here(3), present() ? here(4) : grid.hereCloudBase};
            receivedAt = System.currentTimeMillis();
            current = grid;
        }

        // 0 rain, 1 thunder, 2 cloud, 3 fog, 4 cloud base (y) at the player
        public static float here(int which) {
            var grid = current;
            if (grid == null) {
                return 0;
            }
            float target = switch (which) {
                case 0 -> grid.hereRain;
                case 1 -> grid.hereThunder;
                case 2 -> grid.hereCloud;
                case 3 -> grid.hereFog;
                default -> grid.hereCloudBase;
            };
            float t = Mth.clamp((System.currentTimeMillis() - receivedAt) / 2000f, 0, 1);
            return Mth.lerp(t, from[which], target);
        }

        // Bilinear between the grid's columns; above a column's surface it cools with the lapse rate
        public static double celsius(BlockPos pos) {
            var grid = current;
            if (grid == null) {
                return Double.NaN;
            }
            double gx = (pos.getX() - grid.originX) / (double) grid.spacing;
            double gz = (pos.getZ() - grid.originZ) / (double) grid.spacing;
            if (gx < 0 || gz < 0 || gx > grid.size - 1 || gz > grid.size - 1) {
                return Double.NaN;
            }
            int i = Math.min((int) gx, grid.size - 2);
            int j = Math.min((int) gz, grid.size - 2);
            if (grid.size < 2) {
                return grid.at(0, 0, pos.getY());
            }
            double fx = gx - i;
            double fz = gz - j;
            double a = grid.at(i, j, pos.getY());
            double b = grid.at(i + 1, j, pos.getY());
            double c = grid.at(i, j + 1, pos.getY());
            double d = grid.at(i + 1, j + 1, pos.getY());
            if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c) || Double.isNaN(d)) {
                // Edge of the loaded area: nearest column
                return grid.at((int) Math.round(gx), (int) Math.round(gz), pos.getY());
            }
            return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
        }

        // Precipitation 0..1 at a column (nearest grid cell); NaN outside the grid
        public static double precipitation(BlockPos pos) {
            var grid = current;
            if (grid == null) {
                return Double.NaN;
            }
            int i = (int) Math.round((pos.getX() - grid.originX) / (double) grid.spacing);
            int j = (int) Math.round((pos.getZ() - grid.originZ) / (double) grid.spacing);
            if (i < 0 || j < 0 || i >= grid.size || j >= grid.size || Float.isNaN(grid.celsius[i * grid.size + j])) {
                return Double.NaN;
            }
            return (grid.precipitation[i * grid.size + j] & 0xff) / 255.0;
        }
    }

    private double at(int i, int j, int y) {
        int k = i * size + j;
        double t = celsius[k];
        return t - lapse * Math.max(0, y - surface[k]);
    }
}
