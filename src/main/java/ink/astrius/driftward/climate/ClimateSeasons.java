package ink.astrius.driftward.climate;

import com.github.thedeathlycow.thermoo.api.season.ThermooSeason;
import ink.astrius.driftward.reg.ModLoaded;
import net.minecraft.world.level.Level;
import sereneseasons.api.season.SeasonHelper;

import java.util.OptionalDouble;

// Where in the year we are: the Serene Seasons year as 0 (start of spring) .. 1, and the seasonal
// factor -1 (coldest) .. 1 (warmest).
public final class ClimateSeasons {
    public static final String[] SUB_SEASONS = {
        "early_spring", "mid_spring", "late_spring",
        "early_summer", "mid_summer", "late_summer",
        "early_autumn", "mid_autumn", "late_autumn",
        "early_winter", "mid_winter", "late_winter",
    };

    private ClimateSeasons() {}

    // Empty without Serene Seasons, which is the only source of a continuous year
    public static OptionalDouble year(Level level) {
        if (!ModLoaded.SERENE_SEASONS) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(SereneSeasons.year(level));
    }

    public static double factorAt(double year, double peak) {
        return Math.cos(2 * Math.PI * (year - peak));
    }

    // `lag` delays the curve by that fraction of a year (water warms and cools late)
    public static double factor(Level level, double peak, double lag) {
        var year = year(level);
        if (year.isPresent()) {
            return factorAt(year.getAsDouble() - lag, peak);
        }
        // Thermoo only knows discrete seasons
        return ThermooSeason.getCurrentSeason(level).map(season -> switch (season) {
            case SUMMER -> 1.0;
            case WINTER -> -1.0;
            default -> 0.0;
        }).orElse(0.0);
    }

    public static double factor(Level level, double peak) {
        return factor(level, peak, 0);
    }

    public static String subSeason(double year) {
        int i = (int) Math.floor((year - Math.floor(year)) * SUB_SEASONS.length);
        return SUB_SEASONS[Math.min(i, SUB_SEASONS.length - 1)];
    }

    // Middle of a named sub-season, or -1
    public static double yearOf(String subSeason) {
        for (int i = 0; i < SUB_SEASONS.length; i++) {
            if (SUB_SEASONS[i].equals(subSeason)) {
                return (i + 0.5) / SUB_SEASONS.length;
            }
        }
        return -1;
    }

    // Separate class so that Serene Seasons is only loaded when present
    private static final class SereneSeasons {
        static double year(Level level) {
            var state = SeasonHelper.getSeasonState(level);
            return (double) state.getSeasonCycleTicks() / state.getCycleDuration();
        }
    }
}
