package ink.astrius.driftwardfixes.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.mehvahdjukaar.supplementaries.client.TextUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Restriction(require = @Condition("supplementaries"))
@Mixin(TextUtils.class)
public class BeeMovieMixin {
    @Redirect(
        method = "splitToLines",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;")
    )
    private static List<FormattedCharSequence> fixBeeMovie(
        Font instance,
        FormattedText text,
        int maxWidth,
        @Local(name = "text") String rawText
    ) {
        final var clean = rawText.replace("§r", "").replace("§0", "");
        return Language.getInstance().getVisualOrder(instance.getSplitter().splitLines(clean, maxWidth, Style.EMPTY));
    }
}
