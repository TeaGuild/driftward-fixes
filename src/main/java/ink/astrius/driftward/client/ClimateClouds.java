package ink.astrius.driftward.client;

import com.mojang.blaze3d.platform.NativeImage;
import ink.astrius.driftward.climate.ClimateCloudField;
import ink.astrius.driftward.climate.ClimateLocalWeather;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

// The cloud layer from the weather: vanilla draws its clouds as boxes whose pixels come from
// clouds.png (256×256, 12 blocks per pixel, wrapping). We register our own texture under that name,
// anchored to world coordinates and rebuilt twice a second: a pixel is cloud where a drifting shape
// noise falls under the local cloud cover, so overcast closes up and fair weather breaks apart.
public final class ClimateClouds {
    private static final ResourceLocation LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final int SIZE = 256;
    private static final int CELL = 12;
    // Shape drift per tick per m/s of wind
    private static final double DRIFT = 0.012;

    private static DynamicTexture texture;
    private static boolean installed;
    private static ImprovedNoise[] shapes;
    private static int shapeSeed;

    private ClimateClouds() {}

    public static void registerClient() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    public static boolean installed() {
        return installed;
    }

    private static void tick() {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var field = ClimateCloudField.Received.current();
        if (level == null || level.dimension() != Level.OVERWORLD || field == null || !ClimateLocalWeather.active(level)) {
            if (level == null) {
                ClimateCloudField.Received.clear();
            }
            uninstall(minecraft);
            return;
        }
        if (installed && level.getGameTime() % 10 != 0) {
            return;
        }
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        rebuild(field, level.getGameTime(), camera.x, camera.z);
        if (!installed) {
            minecraft.getTextureManager().register(LOCATION, texture);
            installed = true;
        }
    }

    private static void uninstall(Minecraft minecraft) {
        if (installed) {
            // Vanilla reloads its own clouds.png the next time it's bound
            minecraft.getTextureManager().release(LOCATION);
            installed = false;
            texture = null;
        }
    }

    private static double shape(double x, double z) {
        double n = 0.62 * shapes[0].noise(x / 110, 0, z / 110) + 0.38 * shapes[1].noise(x / 40, 0.5, z / 40);
        return Mth.clamp(0.5 + n * 0.95, 0, 1);
    }

    private static void rebuild(ClimateCloudField field, long gameTime, double cameraX, double cameraZ) {
        if (shapes == null || shapeSeed != field.shapeSeed()) {
            var random = RandomSource.create(field.shapeSeed());
            shapes = new ImprovedNoise[] {new ImprovedNoise(random), new ImprovedNoise(random)};
            shapeSeed = field.shapeSeed();
        }
        if (texture == null) {
            texture = new DynamicTexture(SIZE, SIZE, false);
        }
        NativeImage pixels = texture.getPixels();
        double shiftX = field.windX() * gameTime * DRIFT;
        double shiftZ = field.windZ() * gameTime * DRIFT;
        // Vanilla maps texel (u, v) to x = 12u and z = 12(v - 0.33), repeating every 256 texels; take
        // the copy nearest the camera
        int baseU = Mth.floor(cameraX / CELL);
        int baseV = Mth.floor(cameraZ / CELL + 0.33);
        for (int u = 0; u < SIZE; u++) {
            int tu = u + SIZE * Math.round((baseU - u) / (float) SIZE);
            double x = tu * CELL + CELL / 2.0;
            for (int v = 0; v < SIZE; v++) {
                int tv = v + SIZE * Math.round((baseV - v) / (float) SIZE);
                double z = (tv - 0.33) * CELL + CELL / 2.0;
                double cover = ClimateCloudField.Received.cover(x, z);
                boolean cloud = !Double.isNaN(cover) && shape(x - shiftX, z - shiftZ) < cover;
                pixels.setPixelRGBA(u, v, cloud ? 0xFFFFFFFF : 0);
            }
        }
        texture.upload();
    }
}
