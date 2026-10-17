package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Aggregates only bounded saved checklist records, not complete account history or patch-attributed XP. */
public final class RunHistorySummary
{
    private final List<RunMetricsSnapshot> entries;
    private final int completed,knownXpCount;
    private final long knownXp;
    private final Duration elapsed;
    private final OptionalDouble xpPerHour;
    private final RunStopOutcomeSummary stopOutcomes;
    private RunHistorySummary(List<RunMetricsSnapshot> selected)
    {
        entries = List.copyOf(selected); int complete=0,known=0; long xp=0,timedXp=0; Duration total=Duration.ZERO,timed=Duration.ZERO;
        for(RunMetricsSnapshot entry:entries)
        {
            if(entry.isCompleted()) { complete++; } total=total.plus(entry.getElapsed());
            if(entry.isXpKnown())
            {
                known++; xp+=entry.getXpGained();
                if(!entry.getElapsed().isZero()) { timed=timed.plus(entry.getElapsed()); timedXp+=entry.getXpGained(); }
            }
        }
        completed=complete; knownXpCount=known; knownXp=xp; elapsed=total;
        stopOutcomes = new RunStopOutcomeSummary(entries);
        double seconds=timed.getSeconds()+timed.getNano()/1_000_000_000d;
        xpPerHour=seconds>0?OptionalDouble.of(timedXp*3600d/seconds):OptionalDouble.empty();
    }
    public static RunHistorySummary create(List<RunMetricsSnapshot> history,RunHistoryPeriod period,RunHistoryStore.Kind kind,Instant now)
    { return create(history,period,kind,now,RunHistoryOutcome.ALL); }
    public static RunHistorySummary create(List<RunMetricsSnapshot> history,RunHistoryPeriod period,RunHistoryStore.Kind kind,Instant now,RunHistoryOutcome outcome)
    {
        Map<String,RunMetricsSnapshot> unique=new LinkedHashMap<>();
        for(RunMetricsSnapshot entry:history)
        {
            if(!RunHistoryStore.valid(entry,now) || entry.getElapsed()==null || entry.getElapsed().isNegative()
                    || !entry.getElapsed().equals(Duration.between(entry.getStartedAt(),entry.getFinishedAt()))) { continue; }
            unique.putIfAbsent(entry.getRunName()+"|"+entry.getStartedAt(),entry);
        }
        List<RunMetricsSnapshot> bounded=new ArrayList<>(unique.values());
        bounded.sort(Comparator.comparing(RunMetricsSnapshot::getFinishedAt).reversed().thenComparing(RunMetricsSnapshot::getRunName));
        if(bounded.size()>RunHistoryStore.LIMIT) { bounded.subList(RunHistoryStore.LIMIT,bounded.size()).clear(); }
        bounded.removeIf(entry -> !period.includes(entry.getFinishedAt(),now) || kind!=null && RunHistoryStore.Kind.fromLabel(entry.getRunName())!=kind
                || outcome!=null && !outcome.includes(entry));
        return new RunHistorySummary(bounded);
    }
    public List<RunMetricsSnapshot> getEntries() { return entries; }
    public RunStopOutcomeSummary getStopOutcomes() { return stopOutcomes; }
    public int getCompletedCount() { return completed; }
    public int getStoppedCount() { return entries.size()-completed; }
    public int getKnownXpCount() { return knownXpCount; }
    public int getUnknownXpCount() { return entries.size()-knownXpCount; }
    public long getKnownXp() { return knownXp; }
    public Duration getElapsed() { return elapsed; }
    public OptionalDouble getXpPerHour() { return xpPerHour; }
}
