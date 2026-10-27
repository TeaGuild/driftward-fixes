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

// The client has no world seed, so it can't run the model. For snow and rain rendering, the server
// sends each player a grid of outdoor air temperatures at the surface around them.
public record ClimateSync(
    int originX, int originZ, int spacing, int size, float lapse, float[] celsius, short[] surface
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
            }
        },
        buf -> {
            int originX = buf.readVarInt();
            int originZ = buf.readVarInt();
            int spacing = buf.readVarInt();
            int size = Mth.clamp(buf.readVarInt(), 1, 64);
            float lapse = buf.readFloat();
            var celsius = new float[size * size];
            var surface = new short[size * size];
            for (int i = 0; i < size * size; i++) {
                celsius[i] = buf.readFloat();
                surface[i] = buf.readShort();
            }
            return new ClimateSync(originX, originZ, spacing, size, lapse, celsius, surface);
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
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, ClimateSync::handle);
    }

    private static void handle(ClimateSync grid, IPayloadContext ctx) {
        Grid.current = grid;
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

    static ClimateSync sample(ServerLevel level, BlockPos center) {
        var c = ClimateConstants.get();
        int spacing = c.syncSpacing();
        int size = c.syncRadius() * 2 + 1;
        int originX = Math.floorDiv(center.getX(), spacing) * spacing - c.syncRadius() * spacing;
        int originZ = Math.floorDiv(center.getZ(), spacing) * spacing - c.syncRadius() * spacing;
        var celsius = new float[size * size];
        var surface = new short[size * size];
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
                var at = ClimateModel.live(level, pos, true);
                var breakdown = ClimateModel.breakdown(level, pos, level.getBiome(pos), ClimateModel.sample(level, pos), at);
                celsius[k] = (float) breakdown.airCelsius();
                surface[k] = (short) y;
            }
        }
        return new ClimateSync(originX, originZ, spacing, size, (float) c.lapsePerBlock(), celsius, surface);
    }

    // The last grid the client received
    public static final class Grid {
        static volatile ClimateSync current;

        private Grid() {}

        public static boolean present() {
            return current != null;
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
    }

    private double at(int i, int j, int y) {
        int k = i * size + j;
        double t = celsius[k];
        return t - lapse * Math.max(0, y - surface[k]);
    }
}
