package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.Objects;

public final class GardenPatchObservation
{
    private final GardenPatchState state;
    private final Instant observedAt;
    private final FarmingGrowthRate growthRate;
    public GardenPatchObservation(GardenPatchState state, Instant at, FarmingGrowthRate rate)
    {
        this.state = Objects.requireNonNull(state); this.observedAt = Objects.requireNonNull(at); this.growthRate = Objects.requireNonNull(rate);
    }
    public GardenPatchState getState() { return state; }
    public Instant getObservedAt() { return observedAt; }
    public FarmingGrowthRate getGrowthRate() { return growthRate; }
}
