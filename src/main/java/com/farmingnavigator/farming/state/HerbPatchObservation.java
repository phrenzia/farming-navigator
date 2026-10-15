package com.farmingnavigator.farming.state;

import java.time.Instant;
import java.util.Objects;

/** What was observed at a particular time, without claiming it is still current. */
public final class HerbPatchObservation
{
    private final HerbPatchState state;
    private final Instant observedAt;

    public HerbPatchObservation(HerbPatchState state, Instant observedAt)
    {
        this.state = Objects.requireNonNull(state);
        this.observedAt = Objects.requireNonNull(observedAt);
    }

    public HerbPatchState getState()
    {
        return state;
    }

    public Instant getObservedAt()
    {
        return observedAt;
    }
}
