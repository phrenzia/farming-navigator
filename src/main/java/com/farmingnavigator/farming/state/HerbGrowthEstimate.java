package com.farmingnavigator.farming.state;

import java.time.Instant;

/** A harvest window conditional on continued healthy growth, not proof of readiness. */
public final class HerbGrowthEstimate
{
    private final Instant earliest;
    private final Instant latest;

    HerbGrowthEstimate(Instant earliest, Instant latest)
    {
        this.earliest = earliest;
        this.latest = latest;
    }

    public Instant getEarliest()
    {
        return earliest;
    }

    public Instant getLatest()
    {
        return latest;
    }
}
