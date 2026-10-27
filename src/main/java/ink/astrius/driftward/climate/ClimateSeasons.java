package ink.astrius.driftward.climate;

import com.github.thedeathlycow.thermoo.api.season.ThermooSeason;
import ink.astrius.driftward.reg.ModLoaded;
import net.minecraft.world.level.Level;
import sereneseasons.api.season.SeasonHelper;

// Where in the year we are, as -1 (coldest) .. 1 (warmest).
final class ClimateSeasons {
    private ClimateSeasons() {}

    static double factor(Level level, double peak) {
        if (ModLoaded.SERENE_SEASONS) {
            return SereneSeasons.factor(level, peak);
        }
        // Thermoo only knows discrete seasons
        return ThermooSeason.getCurrentSeason(level).map(season -> switch (season) {
            case SUMMER -> 1.0;
            case WINTER -> -1.0;
            default -> 0.0;
        }).orElse(0.0);
    }

    // Separate class so that Serene Seasons is only loaded when present
    private static final class SereneSeasons {
        static double factor(Level level, double peak) {
            var state = SeasonHelper.getSeasonState(level);
            double year = (double) state.getSeasonCycleTicks() / state.getCycleDuration();
            return Math.cos(2 * Math.PI * (year - peak));
        }
    }
}
