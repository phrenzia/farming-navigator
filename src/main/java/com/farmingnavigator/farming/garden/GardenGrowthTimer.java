package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class GardenGrowthTimer
{
    public Optional<GardenGrowthWindow> estimate(GardenPatchObservation observation, Instant now)
    {
        if (observation == null || observation.getObservedAt().isAfter(now)
                || observation.getGrowthRate() == FarmingGrowthRate.UNKNOWN) { return Optional.empty(); }
        GardenPatchState state = observation.getState();
        GardenCrop crop = state.getCrop();
        if (state.getCondition() != GardenPatchCondition.GROWING || crop == GardenCrop.UNKNOWN
                || state.getStage() < 0 || state.getStage() >= crop.getStages() - 1) { return Optional.empty(); }
        int minutes = crop.getGrowthCycleMinutes();
        if (observation.getGrowthRate() == FarmingGrowthRate.LEAGUES) { minutes /= 5; }
        Duration cycle = Duration.ofMinutes(minutes);
        Instant earliest = observation.getObservedAt().plus(cycle.multipliedBy(crop.getStages() - 2L - state.getStage()));
        return Optional.of(new GardenGrowthWindow(earliest, earliest.plus(cycle)));
    }
}
