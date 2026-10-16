package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Client-thread session statistics; history persistence is separate. Reset at account/world-mode boundaries. */
public final class RunMetricsTracker
{
    private String runName;
    private Instant startedAt;
    private Instant lastSampleAt;
    private Instant finishedAt;
    private int baselineXp;
    private int lastXp;
    private boolean xpKnown;
    private boolean completed;

    public void start(String name, int currentXp, Instant now)
    {
        clear(); runName = Objects.requireNonNull(name); startedAt = Objects.requireNonNull(now); lastSampleAt = now;
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
    public RunMetricsSnapshot snapshot(Instant now)
    {
        if (startedAt == null) { return null; }
        Instant end = finishedAt == null ? now : finishedAt;
        if (finishedAt == null && end.isBefore(lastSampleAt)) { end = lastSampleAt; }
        if (end.isBefore(startedAt)) { end = startedAt; }
        return new RunMetricsSnapshot(runName, isRecording(), completed, xpKnown,
                xpKnown ? Math.max(0L, (long) lastXp - baselineXp) : 0, Duration.between(startedAt, end), startedAt, finishedAt);
    }
    public void clear()
    {
        runName = null; startedAt = lastSampleAt = finishedAt = null;
        baselineXp = lastXp = -1; xpKnown = completed = false;
    }
}
