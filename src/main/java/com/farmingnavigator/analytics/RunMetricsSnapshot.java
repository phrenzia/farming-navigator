package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalDouble;

/** Recorded account XP during a checklist, not crop attribution or a future-yield prediction. */
public final class RunMetricsSnapshot
{
    private final String runName;
    private final RunHistoryContext context;
    private final RunStopOutcomes stopOutcomes;
    private final boolean recording;
    private final boolean completed;
    private final boolean xpKnown;
    private final long xpGained;
    private final Duration elapsed;
    private final Instant startedAt, finishedAt;
    RunMetricsSnapshot(String runName, boolean recording, boolean completed, boolean xpKnown, long xpGained, Duration elapsed)
    {
        this(runName, recording, completed, xpKnown, xpGained, elapsed, null, null);
    }
    RunMetricsSnapshot(String runName, boolean recording, boolean completed, boolean xpKnown, long xpGained, Duration elapsed,
            Instant startedAt, Instant finishedAt)
    { this(runName,recording,completed,xpKnown,xpGained,elapsed,startedAt,finishedAt,RunHistoryContext.UNKNOWN); }
    RunMetricsSnapshot(String runName,boolean recording,boolean completed,boolean xpKnown,long xpGained,Duration elapsed,
            Instant startedAt,Instant finishedAt,RunHistoryContext context)
    { this(runName,recording,completed,xpKnown,xpGained,elapsed,startedAt,finishedAt,context,RunStopOutcomes.UNKNOWN); }
    RunMetricsSnapshot(String runName,boolean recording,boolean completed,boolean xpKnown,long xpGained,Duration elapsed,
            Instant startedAt,Instant finishedAt,RunHistoryContext context,RunStopOutcomes stopOutcomes)
    {
        this.stopOutcomes=stopOutcomes==null?RunStopOutcomes.UNKNOWN:stopOutcomes;
        this.context=context==null?RunHistoryContext.UNKNOWN:context;
        this.startedAt = startedAt; this.finishedAt = finishedAt;
        this.runName = runName; this.recording = recording; this.completed = completed;
        this.xpKnown = xpKnown; this.xpGained = xpGained; this.elapsed = elapsed;
    }
    public RunHistoryContext getContext(){return context;}
    public RunStopOutcomes getStopOutcomes(){return stopOutcomes;}
    public String getRunName() { return runName; }
    public boolean isRecording() { return recording; }
    public boolean isCompleted() { return completed; }
    public boolean isXpKnown() { return xpKnown; }
    public long getXpGained() { return xpGained; }
    public Duration getElapsed() { return elapsed; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public OptionalDouble getXpPerHour()
    {
        double seconds = elapsed.getSeconds() + elapsed.getNano() / 1_000_000_000d;
        return xpKnown && seconds > 0 ? OptionalDouble.of(xpGained * 3600d / seconds) : OptionalDouble.empty();
    }
}
