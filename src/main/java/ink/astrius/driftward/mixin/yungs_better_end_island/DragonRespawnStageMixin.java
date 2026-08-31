package ink.astrius.driftward.mixin.yungs_better_end_island;

import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static ink.astrius.driftward.DriftwardTags.END_BASE;

@Restriction(require = @Condition("betterendisland"))
@Mixin(targets = "com.yungnickyoung.minecraft.betterendisland.world.DragonRespawnStage$3")
public class DragonRespawnStageMixin {
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
        )
    )
    public boolean replaceEndStoneWithTag(BlockState instance, Block block) {
        return instance.is(END_BASE);
    }
}
