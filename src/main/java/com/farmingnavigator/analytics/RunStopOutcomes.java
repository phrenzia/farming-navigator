package com.farmingnavigator.analytics;

/** Checklist outcomes, not harvested crops or verified farming actions. Completed includes manual confirmations. */
public final class RunStopOutcomes
{
    public static final RunStopOutcomes UNKNOWN = new RunStopOutcomes(-1, -1, -1);
    private final int completed, manual, skipped;
    private RunStopOutcomes(int completed, int manual, int skipped)
    { this.completed = completed; this.manual = manual; this.skipped = skipped; }
    public static RunStopOutcomes of(int completed, int manual, int skipped)
    {
        if (completed < 0 || manual < 0 || skipped < 0 || manual > completed)
        { throw new IllegalArgumentException("Checklist counts must be nonnegative; manual completions are a subset of completed stops."); }
        return new RunStopOutcomes(completed, manual, skipped);
    }
    public boolean isKnown() { return completed >= 0; }
    public int getCompleted() { return completed; }
    public int getManual() { return manual; }
    public int getSkipped() { return skipped; }
    public long getFinished() { return (long) completed + skipped; }
    public boolean isValidFor(RunHistoryContext context, boolean complete)
    {
        return !isKnown() || context != null && context.isKnown() && getFinished() <= context.getPlannedStops()
                && (!complete || getFinished() == context.getPlannedStops());
    }
    public String describe(RunHistoryContext context)
    {
        return !isKnown() || !isValidFor(context, false) ? "Checklist stop outcomes: unavailable."
                : "Checklist stops: " + completed + " completed (" + manual + " completed manually), " + skipped + " skipped, "
                        + (context.getPlannedStops() - getFinished()) + " unfinished.";
    }
}
