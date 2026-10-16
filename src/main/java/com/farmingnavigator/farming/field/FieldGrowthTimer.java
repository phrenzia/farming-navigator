package com.farmingnavigator.farming.field;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class FieldGrowthTimer
{
    public Optional<FieldGrowthWindow> estimate(FieldPatchObservation observation, Instant now)
    {
        if (observation == null || observation.getObservedAt().isAfter(now)
                || observation.getGrowthRate() == FarmingGrowthRate.UNKNOWN) { return Optional.empty(); }
        FieldPatchState state = observation.getState();
        FieldCrop crop = state.getCrop();
        if (state.getCondition() != FieldPatchCondition.GROWING || crop == FieldCrop.UNKNOWN
                || state.getStage() < 0 || state.getStage() >= crop.getStages() - 1) { return Optional.empty(); }
        int minutes = crop.getGrowthCycleMinutes();
        if (observation.getGrowthRate() == FarmingGrowthRate.LEAGUES) { minutes /= 5; }
        Duration cycle = Duration.ofMinutes(minutes);
        Instant earliest = observation.getObservedAt().plus(cycle.multipliedBy(crop.getStages() - 2L - state.getStage()));
        return Optional.of(new FieldGrowthWindow(earliest, earliest.plus(cycle)));
    }
}
