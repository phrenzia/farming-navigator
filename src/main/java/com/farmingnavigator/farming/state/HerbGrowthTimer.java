package com.farmingnavigator.farming.state;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * RuneLite Produce defines five herb stages (four growing stages, then harvest).
 * Without an observed farming-tick offset, the next cycle lies within one cycle.
 * https://github.com/runelite/runelite/blob/e98399f931c60bfa8e42f924da850f508ec8e00d/runelite-client/src/main/java/net/runelite/client/plugins/timetracking/farming/Produce.java
 * Disease, death, and later player actions can invalidate an estimate.
 */
public final class HerbGrowthTimer
{
    public Optional<HerbGrowthEstimate> estimate(HerbPatchObservation observation, Instant now)
    {
        if (observation == null || observation.getObservedAt().isAfter(now)
                || observation.getGrowthRate() == FarmingGrowthRate.UNKNOWN)
        {
            return Optional.empty();
        }
        HerbPatchState state = observation.getState();
        if (state.getCondition() != PatchCondition.GROWING || state.getCrop() == HerbCrop.UNKNOWN
                || state.getStage() < 0 || state.getStage() > 3)
        {
            return Optional.empty();
        }
        Duration cycle = observation.getGrowthRate().getHerbCycle();
        Instant earliest = observation.getObservedAt().plus(cycle.multipliedBy(3 - state.getStage()));
        Instant latest = earliest.plus(cycle);
        return Optional.of(new HerbGrowthEstimate(earliest, latest));
    }
}
