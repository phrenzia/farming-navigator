package com.farmingnavigator.farming.state;

import java.time.Duration;
import java.util.Set;
import net.runelite.api.WorldType;

/** Mirrors RuneLite FarmingTracker's seasonal/non-Deadman growth-rate distinction. */
public enum FarmingGrowthRate
{
    UNKNOWN(0),
    NORMAL(20),
    LEAGUES(4);

    private final int herbCycleMinutes;

    FarmingGrowthRate(int herbCycleMinutes)
    {
        this.herbCycleMinutes = herbCycleMinutes;
    }

    public Duration getHerbCycle()
    {
        return Duration.ofMinutes(herbCycleMinutes);
    }

    public static FarmingGrowthRate fromWorldTypes(Set<WorldType> types)
    {
        if (types == null)
        {
            return UNKNOWN;
        }
        return types.contains(WorldType.SEASONAL) && !types.contains(WorldType.DEADMAN) ? LEAGUES : NORMAL;
    }
}
