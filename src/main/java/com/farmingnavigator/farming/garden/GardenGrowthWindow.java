package com.farmingnavigator.farming.garden;

import java.time.Instant;

/** Conditional first health-check window; it does not model produce regrowth or prove survival. */
public final class GardenGrowthWindow
{
    private final Instant earliest;
    private final Instant latest;
    GardenGrowthWindow(Instant earliest, Instant latest) { this.earliest = earliest; this.latest = latest; }
    public Instant getEarliest() { return earliest; }
    public Instant getLatest() { return latest; }
}
