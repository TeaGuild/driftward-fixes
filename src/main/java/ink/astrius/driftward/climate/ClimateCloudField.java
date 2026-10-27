package ink.astrius.driftward.climate;

import ink.astrius.driftward.Driftward;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.ConcurrentHashMap;

// Cloud cover over the sky a player can see (±512 blocks), for the client's cloud layer. Computed
// from the world generator like maps (no chunks loaded), on a coarse grid, cached per cell so nearby
// players share the work, and sent every few seconds.
public record ClimateCloudField(
    int originX, int originZ, int spacing, int size, byte[] cover, float windX, float windZ, long gameTime, int shapeSeed
) implements CustomPacketPayload {
    public static final Type<ClimateCloudField> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Driftward.MOD_ID, "cloud_field"));
    public static final int SPACING = 64;
    public static final int RADIUS = 8;
    private static final int INTERVAL = 200;
    private static final int SURFACE = 63;

    public static final StreamCodec<FriendlyByteBuf, ClimateCloudField> STREAM_CODEC = StreamCodec.of(
        (buf, f) -> {
            buf.writeVarInt(f.originX);
            buf.writeVarInt(f.originZ);
            buf.writeVarInt(f.spacing);
            buf.writeVarInt(f.size);
            buf.writeByteArray(f.cover);
            buf.writeFloat(f.windX);
            buf.writeFloat(f.windZ);
            buf.writeVarLong(f.gameTime);
            buf.writeInt(f.shapeSeed);
        },
        buf -> new ClimateCloudField(
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), Mth.clamp(buf.readVarInt(), 1, 64),
            buf.readByteArray(4096), buf.readFloat(), buf.readFloat(), buf.readVarLong(), buf.readInt()
        )
    );

    private static final ConcurrentHashMap<Long, Byte> CACHE = new ConcurrentHashMap<>();
    private static volatile long cacheBucket = Long.MIN_VALUE;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClimateCloudField::onPlayerTick);
    }

    static void handle(ClimateCloudField field, IPayloadContext ctx) {
        Received.receive(field);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INTERVAL != 5
            || !ClimateLocalWeather.active(player.level())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, sample(player.serverLevel(), player.blockPosition()));
    }

    static ClimateCloudField sample(ServerLevel level, BlockPos center) {
        int size = RADIUS * 2 + 1;
        int originX = Math.floorDiv(center.getX(), SPACING) * SPACING - RADIUS * SPACING;
        int originZ = Math.floorDiv(center.getZ(), SPACING) * SPACING - RADIUS * SPACING;
        long bucket = level.getGameTime() / INTERVAL;
        if (bucket != cacheBucket) {
            CACHE.clear();
            cacheBucket = bucket;
        }
        var cover = new byte[size * size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int x = originX + i * SPACING;
                int z = originZ + j * SPACING;
                cover[i * size + j] = CACHE.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> cell(level, x, z));
            }
        }
        var here = ClimateLocalWeather.at(level, center);
        return new ClimateCloudField(originX, originZ, SPACING, size, cover, (float) here.windX(), (float) here.windZ(),
            level.getGameTime(), Long.hashCode(level.getSeed() * 0x9E3779B97F4A7C15L));
    }

    private static byte cell(ServerLevel level, int x, int z) {
        var c = ClimateConstants.get();
        var source = level.getChunkSource();
        var sampler = source.randomState().sampler();
        int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(SURFACE), qz = QuartPos.fromBlock(z);
        var biome = source.getGenerator().getBiomeSource().getNoiseBiome(qx, qy, qz, sampler);
        Climate.TargetPoint point = sampler.sample(qx, qy, qz);
        var noise = new ClimateModel.Noise(
            Climate.unquantizeCoord(point.temperature()),
            Climate.unquantizeCoord(point.humidity()),
            Climate.unquantizeCoord(point.continentalness())
        );
        var pos = new BlockPos(x, SURFACE, z);
        var at = new ClimateModel.Conditions(SURFACE,
            ClimateSeasons.factor(level, c.seasonPeak()), ClimateSeasons.factor(level, c.seasonPeak(), c.waterLag()),
            level.getDayTime(), 0, 0, 0, 0, false, null);
        var base = ClimateModel.breakdown(level, pos, biome, noise, at);
        var weather = ClimateWeather.at(level.getSeed(), x, z, SURFACE, level.getGameTime(), base,
            biome.value().hasPrecipitation(), null);
        return (byte) Math.round(Mth.clamp(weather.cloud(), 0, 1) * 255);
    }

    // The client's copy: the latest field and the one before, for blending between updates
    public static final class Received {
        static volatile ClimateCloudField current;
        static volatile ClimateCloudField previous;
        static volatile long receivedAt;

        private Received() {}

        static void receive(ClimateCloudField field) {
            previous = current;
            current = field;
            receivedAt = System.currentTimeMillis();
        }

        public static ClimateCloudField current() {
            return current;
        }

        public static void clear() {
            current = null;
            previous = null;
        }

        // Cover 0..1 at a world position, blended from the previous field over ten seconds; NaN without data
        public static double cover(double x, double z) {
            var now = current;
            if (now == null) {
                return Double.NaN;
            }
            double a = now.coverAt(x, z);
            var before = previous;
            if (before == null) {
                return a;
            }
            double t = Mth.clamp((System.currentTimeMillis() - receivedAt) / 10000.0, 0, 1);
            double b = before.coverAt(x, z);
            return Double.isNaN(b) ? a : Mth.lerp(t, b, a);
        }
    }

    // Bilinear; outside the field, the nearest edge
    double coverAt(double x, double z) {
        double gx = Mth.clamp((x - originX) / spacing, 0, size - 1.001);
        double gz = Mth.clamp((z - originZ) / spacing, 0, size - 1.001);
        int i = (int) gx, j = (int) gz;
        double fx = gx - i, fz = gz - j;
        int i1 = Math.min(i + 1, size - 1), j1 = Math.min(j + 1, size - 1);
        double a = (cover[i * size + j] & 0xff) / 255.0;
        double b = (cover[i1 * size + j] & 0xff) / 255.0;
        double c = (cover[i * size + j1] & 0xff) / 255.0;
        double d = (cover[i1 * size + j1] & 0xff) / 255.0;
        return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
    }
}
