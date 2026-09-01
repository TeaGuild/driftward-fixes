package ink.astrius.driftward.mixin.betterend;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.resources.ResourceLocation;
import org.betterx.betterend.registry.EndEnchantments;
import org.betterx.wover.enchantment.api.EnchantmentKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Restriction(require = @Condition("betterend"))
@Mixin(EndEnchantments.class)
public class EndEnchantmentsMixin {
    @Definition(id = "RESONANCE", field = "Lorg/betterx/betterend/registry/EndEnchantments;RESONANCE:Lorg/betterx/wover/enchantment/api/EnchantmentKey;")
    @Definition(id = "createKey", method = "Lorg/betterx/wover/enchantment/api/EnchantmentManager;createKey(Lnet/minecraft/resources/ResourceLocation;)Lorg/betterx/wover/enchantment/api/EnchantmentKey;")
    @Expression("RESONANCE = @(createKey(?))")
    @Redirect(method = "<clinit>", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static EnchantmentKey removeResonance(ResourceLocation id) {
        return null;
    }
}
