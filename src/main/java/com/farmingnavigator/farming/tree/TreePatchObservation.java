package com.farmingnavigator.farming.tree;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.Objects;

public final class TreePatchObservation
{
    private final TreePatchState state;
    private final Instant observedAt;
    private final FarmingGrowthRate growthRate;

    public TreePatchObservation(TreePatchState state, Instant observedAt, FarmingGrowthRate growthRate)
    {
        this.state = Objects.requireNonNull(state);
        this.observedAt = Objects.requireNonNull(observedAt);
        this.growthRate = Objects.requireNonNull(growthRate);
    }
    public TreePatchState getState() { return state; }
    public Instant getObservedAt() { return observedAt; }
    public FarmingGrowthRate getGrowthRate() { return growthRate; }
}
