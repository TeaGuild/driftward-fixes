// A copy of https://github.com/TheDeathlyCow/scorchful/blob/f78b9d542722002f8ecc5a508ec4db1518e5b9aa/src/main/java/com/github/thedeathlycow/scorchful/compat/ThirstWasTakenPlugin.java
// with an updated PlayerThirst class reference
package ink.astrius.driftward;

import cn.mlus.thirst.content.thirst.PlayerThirst;
import cn.mlus.thirst.foundation.common.capability.ModAttachment;
import com.github.thedeathlycow.scorchful.Scorchful;
import com.github.thedeathlycow.scorchful.api.ServerThirstPlugin;
import com.github.thedeathlycow.scorchful.config.ThirstWasTakenConfig;
import com.github.thedeathlycow.thermoo.api.temperature.TemperatureAware;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ScorchfulThristPlugin implements ServerThirstPlugin {
    @Override
    public boolean dehydrateFromSweating(Player player) {
        ThirstWasTakenConfig config = Scorchful.getConfig().integrationConfig.thirstWasTakenConfig;
        PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);

        if (thirst.getThirst() > config.getMinWaterLevelForSweat()
            && ((TemperatureAware) player).thermoo$getTemperature() > 0) {
            thirst.addExhaustion(player, config.getDehydrationConsumedBySweat());
            return true;
        }

        return false;
    }

    @Override
    public void rehydrateFromEnchantment(Player player, int waterCaptured, double rehydrationEfficiency) {
        PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);

        ThirstWasTakenConfig thirstWasTakenConfig = Scorchful.getConfig().integrationConfig.thirstWasTakenConfig;

        int maxWater = Mth.floor(rehydrationEfficiency * thirstWasTakenConfig.getMaxWaterLost());
        int waterToAdd = player.getRandom().nextIntBetweenInclusive(1, maxWater);
        thirst.drink(waterToAdd, waterToAdd);
    }

    @Override
    public int getRehydrationThreshold() {
        return Scorchful.getConfig().integrationConfig.thirstWasTakenConfig.getRehydrationDrinkSize();
    }
}
