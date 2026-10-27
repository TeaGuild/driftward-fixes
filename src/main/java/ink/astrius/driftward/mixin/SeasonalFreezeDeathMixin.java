package ink.astrius.driftward.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.init.ModConfig;

import java.util.Locale;

// Freezing to death reads differently depending on the season: `death.attack.freeze[.player]`
// becomes `death.attack.freeze.<season>[.player]`. Dimensions without seasons and tropical biomes
// keep the vanilla message. Translations live in the pack's KubeJS `translations.js`.
@Restriction(require = @Condition("sereneseasons"))
@Mixin(DamageSource.class)
public abstract class SeasonalFreezeDeathMixin {
    @ModifyReturnValue(method = "getLocalizedDeathMessage", at = @At("RETURN"))
    private Component driftward$seasonalFreezeMessage(Component original, LivingEntity entity) {
        final var source = (DamageSource) (Object) this;
        if (!source.is(DamageTypes.FREEZE) || !(original.getContents() instanceof TranslatableContents contents)) {
            return original;
        }
        final var prefix = "death.attack." + source.type().msgId();
        if (!contents.getKey().startsWith(prefix)) {
            return original;
        }
        final var level = entity.level();
        if (!ModConfig.seasons.isDimensionWhitelisted(level.dimension())
            || SeasonHelper.usesTropicalSeasons(level.getBiome(entity.blockPosition()))) {
            return original;
        }
        final var season = SeasonHelper.getSeasonState(level).getSeason().name().toLowerCase(Locale.ROOT);
        final var key = prefix + "." + season + contents.getKey().substring(prefix.length());
        return Component.translatable(key, contents.getArgs());
    }
}
