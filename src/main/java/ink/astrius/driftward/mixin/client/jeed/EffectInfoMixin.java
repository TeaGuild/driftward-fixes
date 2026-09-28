package ink.astrius.driftward.mixin.client.jeed;

import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.mehvahdjukaar.jeed.common.EffectInfo;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Restriction(require = @Condition("jeed"))
@Mixin(EffectInfo.class)
public class EffectInfoMixin {
    @Inject(
        method = "getDescription(Lnet/minecraft/core/Holder;)Lnet/minecraft/network/chat/Component;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void supportDesc(Holder<MobEffect> effect, CallbackInfoReturnable<Component> cir) {
        ResourceLocation name = effect.unwrapKey().get().location();
        String descriptionKey = "effect." + name.getNamespace() + "." + name.getPath() + ".desc";
        Component text = Component.translatable(descriptionKey);
        if (!text.getString().equals(descriptionKey)) {
            cir.setReturnValue(text);
        }
    }
}
