package ink.astrius.driftward;

import ink.astrius.driftward.config.ClientConfig;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@ConfigEntryPointForge(Driftward.MOD_ID)
public class SodiumConfig implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
            .addPage(builder.createOptionPage()
                .setName(Component.literal("Driftward"))
                .addOption(
                    builder
                        .createIntegerOption(ResourceLocation.parse("driftward:end_sea_layers"))
                        .setName(Component.translatable("driftward.settings.end_sea_layers"))
                        .setTooltip(Component.translatable("driftward.settings.end_sea_layers.desc"))
                        .setRange(0, ClientConfig.MAX_END_SEA_LAYERS, 1)
                        .setDefaultValue(ClientConfig.DEFAULT_END_SEA_LAYERS)
                        .setValueFormatter(x -> Component.literal(String.valueOf(x)))
                        .setStorageHandler(ClientConfig.SPEC::save)
                        .setBinding(ClientConfig::setEndSeaLayers, ClientConfig::getEndSeaLayers)
                )
            );
    }
}
