package ink.astrius.driftward.mixin.client.jeed;

import com.llamalad7.mixinextras.sugar.Local;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.mehvahdjukaar.jeed.common.EffectInfo;
import net.mehvahdjukaar.jeed.common.EffectRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("jeed"))
@Mixin(EffectRenderer.class)
public class EffectRendererMixin {
    @Redirect(
        method = "getTooltipsWithDescription",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
            ordinal = 4
        )
    )
    private static MutableComponent supportDesc(String key, @Local ResourceLocation res) {
        return (MutableComponent)EffectInfo.getDescription(BuiltInRegistries.MOB_EFFECT.getHolder(res).get());
    }
}
