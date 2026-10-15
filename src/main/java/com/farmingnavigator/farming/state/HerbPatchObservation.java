package com.farmingnavigator.farming.state;

import java.time.Instant;
import java.util.Objects;

/** What was observed at a particular time, without claiming it is still current. */
public final class HerbPatchObservation
{
    private final HerbPatchState state;
    private final Instant observedAt;
    private final FarmingGrowthRate growthRate;

    public HerbPatchObservation(HerbPatchState state, Instant observedAt)
    {
        this(state, observedAt, FarmingGrowthRate.UNKNOWN);
    }

    public HerbPatchObservation(HerbPatchState state, Instant observedAt, FarmingGrowthRate growthRate)
    {
        this.state = Objects.requireNonNull(state);
        this.observedAt = Objects.requireNonNull(observedAt);
        this.growthRate = Objects.requireNonNull(growthRate);
    }

    public HerbPatchState getState()
    {
        return state;
    }

    public Instant getObservedAt()
    {
        return observedAt;
    }

    public FarmingGrowthRate getGrowthRate()
    {
        return growthRate;
    }
}
