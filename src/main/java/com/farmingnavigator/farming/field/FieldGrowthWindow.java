package com.farmingnavigator.farming.field;

import java.time.Instant;

/** Conditional first-harvest window; it does not prove survival or predict yield. */
public final class FieldGrowthWindow
{
    private final Instant earliest;
    private final Instant latest;
    FieldGrowthWindow(Instant earliest, Instant latest) { this.earliest = earliest; this.latest = latest; }
    public Instant getEarliest() { return earliest; }
    public Instant getLatest() { return latest; }
}
