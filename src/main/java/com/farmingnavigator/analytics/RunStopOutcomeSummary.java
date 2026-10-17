package com.farmingnavigator.analytics;

import java.util.List;

/** Subtotals for a validated history sample. Legacy records never contribute invented zero counts. */
public final class RunStopOutcomeSummary
{
    private final int knownCount, unknownCount;
    private final long completed, manual, skipped, unfinished;

    RunStopOutcomeSummary(List<RunMetricsSnapshot> entries)
    {
        int known = 0;
        long done = 0, confirmed = 0, skip = 0, remaining = 0;
        for (RunMetricsSnapshot entry : entries)
        {
            RunStopOutcomes stops = entry.getStopOutcomes();
            if (!stops.isKnown()) { continue; }
            known++;
            done += stops.getCompleted();
            confirmed += stops.getManual();
            skip += stops.getSkipped();
            remaining += entry.getContext().getPlannedStops() - stops.getFinished();
        }
        knownCount = known;
        unknownCount = entries.size() - known;
        completed = done; manual = confirmed; skipped = skip; unfinished = remaining;
    }

    public int getKnownCount() { return knownCount; }
    public int getUnknownCount() { return unknownCount; }
    public long getCompleted() { return completed; }
    public long getManual() { return manual; }
    public long getSkipped() { return skipped; }
    public long getUnfinished() { return unfinished; }

    public String describe()
    {
        return knownCount == 0 ? "Checklist stop subtotals: unavailable (no saved stop counts)."
                : "Checklist stop subtotals: " + completed + " completed (" + manual + " completed manually), "
                        + skipped + " skipped, " + unfinished + " unfinished.";
    }

    public String describeCoverage()
    {
        return "Stop counts known for " + knownCount + " / " + (knownCount + unknownCount)
                + " saved checklists; entries without counts are excluded from stop subtotals.";
    }
}
