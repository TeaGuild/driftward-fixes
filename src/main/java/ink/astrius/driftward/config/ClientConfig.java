package ink.astrius.driftward.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue END_SEA_LAYERS_CFG;
    private static int END_SEA_LAYERS;
    public static final int DEFAULT_END_SEA_LAYERS = 0;
    public static final int MAX_END_SEA_LAYERS = 48;
    public static final ModConfigSpec.IntValue MODONOMICON_SCALE_CFG;
    public static final int DEFAULT_MODONOMICON_SCALE = 4;
    private static int MODONOMICON_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        END_SEA_LAYERS_CFG = b.defineInRange("endSeaLayers", DEFAULT_END_SEA_LAYERS, 0, MAX_END_SEA_LAYERS);
        MODONOMICON_SCALE_CFG = b.defineInRange("modonomiconScale", DEFAULT_MODONOMICON_SCALE, 0, 20);
        SPEC = b.build();
    }

    public static int getEndSeaLayers() {
        return END_SEA_LAYERS;
    }

    public static void setEndSeaLayers(int value) {
        END_SEA_LAYERS = value;
        END_SEA_LAYERS_CFG.set(value);
    }

    public static int getModonomiconScale() {
        return MODONOMICON_SCALE;
    }

    public static void setModonomiconScale(int value) {
        MODONOMICON_SCALE = value;
        MODONOMICON_SCALE_CFG.set(value);
    }

    public static void onConfigReload() {
        END_SEA_LAYERS = END_SEA_LAYERS_CFG.getAsInt();
        MODONOMICON_SCALE = MODONOMICON_SCALE_CFG.getAsInt();
    }
}
