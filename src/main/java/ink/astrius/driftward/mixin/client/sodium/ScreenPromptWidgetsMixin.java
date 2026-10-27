package ink.astrius.driftward.mixin.client.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// CC: Graphics walks `children()` of every screen when its widgets are cleared. Sodium's video
// settings screen includes its prompt's buttons there, but during `init()` they don't exist yet, and
// `List.of` throws on null, crashing the game when the settings are opened.
@Restriction(require = @Condition("sodium"))
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPrompt")
public class ScreenPromptWidgetsMixin {
    @WrapOperation(
        method = "getWidgets",
        at = @At(value = "INVOKE", target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"),
        remap = false
    )
    private List<Object> driftward$skipMissingButtons(Object first, Object second, Operation<List<Object>> original) {
        if (first != null && second != null) {
            return original.call(first, second);
        }
        return Stream.of(first, second).filter(Objects::nonNull).toList();
    }
}
