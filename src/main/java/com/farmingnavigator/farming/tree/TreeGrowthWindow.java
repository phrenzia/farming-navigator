package com.farmingnavigator.farming.tree;

import java.time.Instant;

/** Conditional health-check window, without claiming healthy survival. */
public final class TreeGrowthWindow
{
    private final Instant earliest;
    private final Instant latest;
    TreeGrowthWindow(Instant earliest, Instant latest) { this.earliest = earliest; this.latest = latest; }
    public Instant getEarliest() { return earliest; }
    public Instant getLatest() { return latest; }
}
