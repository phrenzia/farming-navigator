package com.farmingnavigator.farming.field;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.Objects;

public final class FieldPatchObservation
{
    private final FieldPatchState state;
    private final Instant observedAt;
    private final FarmingGrowthRate growthRate;
    public FieldPatchObservation(FieldPatchState state, Instant at, FarmingGrowthRate rate)
    {
        this.state = Objects.requireNonNull(state); this.observedAt = Objects.requireNonNull(at); this.growthRate = Objects.requireNonNull(rate);
    }
    public FieldPatchState getState() { return state; }
    public Instant getObservedAt() { return observedAt; }
    public FarmingGrowthRate getGrowthRate() { return growthRate; }
}
