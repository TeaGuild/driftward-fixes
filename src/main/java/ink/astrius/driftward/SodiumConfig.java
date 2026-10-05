package ink.astrius.driftward;

import com.mojang.blaze3d.platform.Window;
import ink.astrius.driftward.config.ClientConfig;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@ConfigEntryPointForge(Driftward.MOD_ID)
public class SodiumConfig implements ConfigEntryPoint {
    final Window window = Minecraft.getInstance().getWindow();

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        final var scaleOptId = ResourceLocation.parse("driftward:modonomicon_scale");
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
                .addOption(
                    builder
                        .createIntegerOption(scaleOptId)
                        .setName(Component.translatable("driftward.settings.modonomicon_scale"))
                        .setTooltip(Component.translatable("driftward.settings.modonomicon_scale.desc"))
                        .setValidatorProvider((state) -> {
                            var savedValue = state.readIntOption(scaleOptId);
                            var realMax = this.window.calculateScale(0, Minecraft.getInstance().isEnforceUnicode());
                            var presentationMax = Math.max(savedValue, realMax);
                            return new SteppedValidator() {
                                @Override
                                public int min() {
                                    return 0;
                                }

                                @Override
                                public int max() {
                                    return presentationMax;
                                }

                                @Override
                                public int step() {
                                    return 1;
                                }

                                @Override
                                public boolean isValueValid(int value) {
                                    return value >= this.min();
                                }
                            };
                        }, ConfigState.UPDATE_ON_REBUILD, ConfigState.UPDATE_ON_APPLY)
                        .setDefaultValue(ClientConfig.DEFAULT_MODONOMICON_SCALE)
                        .setValueFormatter((v) -> (v == 0) ? Component.translatable("driftward.settings.modonomicon_scale.default") : Component.literal(v + "x"))
                        .setStorageHandler(ClientConfig.SPEC::save)
                        .setBinding(ClientConfig::setModonomiconScale, ClientConfig::getModonomiconScale)
                )
            );
    }
}
