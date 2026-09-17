package ink.astrius.driftward.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue END_SEA_LAYERS_CFG;
    private static int END_SEA_LAYERS;
    public static final int DEFAULT_END_SEA_LAYERS = 0;
    public static final int MAX_END_SEA_LAYERS = 48;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        END_SEA_LAYERS_CFG = b.defineInRange("endSeaLayers", DEFAULT_END_SEA_LAYERS, 0, MAX_END_SEA_LAYERS);
        SPEC = b.build();
    }

    public static int getEndSeaLayers() {
        return END_SEA_LAYERS;
    }

    public static void setEndSeaLayers(int value) {
        END_SEA_LAYERS = value;
        END_SEA_LAYERS_CFG.set(value);
    }

    public static void onConfigReload() {
        END_SEA_LAYERS = END_SEA_LAYERS_CFG.getAsInt();
    }
}
