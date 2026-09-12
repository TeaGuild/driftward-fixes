package ink.astrius.driftward.mixin.scorchful;

import com.github.thedeathlycow.scorchful.Scorchful;
import com.github.thedeathlycow.scorchful.api.ServerThirstPlugin;
import ink.astrius.driftward.ScorchfulThristPlugin;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Restriction(
    require = {
        @Condition("scorchful"),
        @Condition("thirst"),
    }
)
@Mixin(Scorchful.class)
public class ReplaceThirstPluginMixin {
    @ModifyArg(
        method = "onInitialize",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/thedeathlycow/scorchful/api/ServerThirstPlugin;registerPlugin(Lcom/github/thedeathlycow/scorchful/api/ServerThirstPlugin;)V"
        )
    )
    public ServerThirstPlugin replaceThirstPlugin(ServerThirstPlugin plugin) {
        return new ScorchfulThristPlugin();
    }
}
