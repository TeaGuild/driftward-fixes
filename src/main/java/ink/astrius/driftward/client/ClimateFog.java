package ink.astrius.driftward.client;

import ink.astrius.driftward.climate.ClimateLocalWeather;
import ink.astrius.driftward.climate.ClimateSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

// Weather fog: pulls the terrain fog in where the weather layer has fog at the player
public final class ClimateFog {
    private ClimateFog() {}

    public static void registerClient() {
        NeoForge.EVENT_BUS.addListener(ClimateFog::onRenderFog);
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        var level = Minecraft.getInstance().level;
        if (level == null || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE
            || !ClimateLocalWeather.active(level)) {
            return;
        }
        float fog = Mth.clamp(ClimateSync.Grid.here(3), 0, 1);
        if (fog < 0.02f) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float thick = Math.max(24, far * 0.12f);
        event.setFarPlaneDistance(Mth.lerp(fog, far, thick));
        event.setNearPlaneDistance(Mth.lerp(fog, event.getNearPlaneDistance(), 0));
        event.setCanceled(true);
    }
}
