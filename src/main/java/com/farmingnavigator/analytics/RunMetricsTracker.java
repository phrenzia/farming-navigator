package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Client-thread session statistics; history persistence is separate. Reset at account/world-mode boundaries. */
public final class RunMetricsTracker
{
    private String runName;
    private RunHistoryContext context=RunHistoryContext.UNKNOWN;
    private RunStopOutcomes stopOutcomes=RunStopOutcomes.UNKNOWN;
    private Instant startedAt;
    private Instant lastSampleAt;
    private Instant finishedAt;
    private int baselineXp;
    private int lastXp;
    private boolean xpKnown;
    private boolean completed;

    public void start(String name, int currentXp, Instant now)
    { start(name,currentXp,now,RunHistoryContext.UNKNOWN); }
    public void start(String name,int currentXp,Instant now,RunHistoryContext context)
    {
        RunHistoryContext captured=context==null?RunHistoryContext.UNKNOWN:context;
        if(!captured.isValidFor(RunHistoryStore.Kind.fromLabel(name))){throw new IllegalArgumentException("Checklist context must match its run kind.");}
        clear(); this.context=captured; runName = Objects.requireNonNull(name); startedAt = Objects.requireNonNull(now); lastSampleAt = now;
        baselineXp = lastXp = currentXp; xpKnown = currentXp >= 0;
    }
    public void sample(int currentXp, Instant now)
    {
        if (!isRecording() || now.isBefore(lastSampleAt)) { return; }
        lastSampleAt = now;
        if (currentXp < 0 || currentXp < lastXp) { xpKnown = false; }
        lastXp = currentXp;
    }
    public void finish(int currentXp, Instant now, boolean completed)
    {
        if (!isRecording()) { return; }
        sample(currentXp, now);
        finishedAt = now.isBefore(lastSampleAt) ? lastSampleAt : now;
        this.completed = completed;
    }
    public boolean isRecording() { return startedAt != null && finishedAt == null; }
    public RunHistoryStore.Kind getRunKind() { return RunHistoryStore.Kind.fromLabel(runName); }
    public boolean sampleStops(int completed, int manual, int skipped)
    {
        if (!isRecording() || !context.isKnown()) { return false; }
        RunStopOutcomes next;
        try { next=RunStopOutcomes.of(completed,manual,skipped); }
        catch (IllegalArgumentException ex) { return false; }
        if (!next.isValidFor(context,false) || stopOutcomes.isKnown() && (completed<stopOutcomes.getCompleted()
                || manual<stopOutcomes.getManual() || skipped<stopOutcomes.getSkipped())) { return false; }
        stopOutcomes=next; return true;
    }
    public RunMetricsSnapshot snapshot(Instant now)
    {
        if (startedAt == null) { return null; }
        Instant end = finishedAt == null ? now : finishedAt;
        if (finishedAt == null && end.isBefore(lastSampleAt)) { end = lastSampleAt; }
        if (end.isBefore(startedAt)) { end = startedAt; }
        return new RunMetricsSnapshot(runName, isRecording(), completed, xpKnown,
                xpKnown ? Math.max(0L, (long) lastXp - baselineXp) : 0, Duration.between(startedAt, end), startedAt, finishedAt,context,stopOutcomes);
    }
    public void clear()
    {
        context=RunHistoryContext.UNKNOWN;
        stopOutcomes=RunStopOutcomes.UNKNOWN;
        runName = null; startedAt = lastSampleAt = finishedAt = null;
        baselineXp = lastXp = -1; xpKnown = completed = false;
    }
}
