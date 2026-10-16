package com.farmingnavigator.analytics;

import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistorySummaryTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RunMetricsSnapshot entry(String kind,Instant end,Duration elapsed,boolean known,long xp,boolean complete)
    { return new RunMetricsSnapshot(kind,false,complete,known,xp,elapsed,end.minus(elapsed),end); }
    @Test public void aggregateRateWeightsKnownTimedEntriesRatherThanAveragingTheirRates()
    {
        RunMetricsSnapshot hour=entry("Herb run",now.minusSeconds(1),Duration.ofHours(1),true,360,true);
        RunMetricsSnapshot minute=entry("Field run",now.minusSeconds(2),Duration.ofMinutes(1),true,60,false);
        RunMetricsSnapshot unknown=entry("Tree run",now.minusSeconds(3),Duration.ofHours(1),false,0,true);
        RunMetricsSnapshot zeroTime=entry("Bush/cactus run",now.minusSeconds(4),Duration.ZERO,true,100,false);
        RunHistorySummary summary=RunHistorySummary.create(List.of(hour,minute,unknown,zeroTime),RunHistoryPeriod.ALL,null,now);
        assertEquals(520,summary.getKnownXp()); assertEquals(3,summary.getKnownXpCount()); assertEquals(1,summary.getUnknownXpCount());
        assertEquals(2,summary.getCompletedCount()); assertEquals(2,summary.getStoppedCount()); assertEquals(Duration.ofMinutes(121),summary.getElapsed());
        assertEquals(420*3600d/3660,summary.getXpPerHour().getAsDouble(),0.00001);
    }
    @Test public void rollingWindowsUseInclusiveEndTimesWithoutProratingWholeChecklistXp()
    {
        RunMetricsSnapshot boundary=entry("Herb run",now.minus(Duration.ofHours(24)),Duration.ofHours(8),true,100,true);
        RunMetricsSnapshot outside=entry("Tree run",boundary.getFinishedAt().minusNanos(1),Duration.ofMinutes(1),true,200,true);
        RunMetricsSnapshot seven=entry("Field run",now.minus(Duration.ofDays(7)),Duration.ofMinutes(1),true,300,true);
        List<RunMetricsSnapshot> entries=List.of(boundary,outside,seven);
        RunHistorySummary day=RunHistorySummary.create(entries,RunHistoryPeriod.LAST_24_HOURS,null,now);
        assertEquals(1,day.getEntries().size()); assertEquals(100,day.getKnownXp());
        assertEquals(3,RunHistorySummary.create(entries,RunHistoryPeriod.LAST_7_DAYS,null,now).getEntries().size());
        assertTrue(RunHistorySummary.create(entries,RunHistoryPeriod.LAST_24_HOURS,null,now.plusNanos(1)).getEntries().isEmpty());
    }
    @Test public void boundedHistoryIsTrimmedBeforeFilteringAndDuplicateIdentitiesDoNotInflateTotals()
    {
        List<RunMetricsSnapshot> entries=new ArrayList<>();
        for(int i=1;i<=21;i++) { entries.add(entry(i>=20?"Tree run":"Herb run",now.minusSeconds(i*60),Duration.ofSeconds(1),true,1,true)); }
        entries.add(entries.get(0));
        RunHistorySummary all=RunHistorySummary.create(entries,RunHistoryPeriod.ALL,null,now); assertEquals(20,all.getEntries().size()); assertEquals(20,all.getKnownXp());
        RunHistorySummary trees=RunHistorySummary.create(entries,RunHistoryPeriod.ALL,RunHistoryStore.Kind.TREE,now);
        assertEquals(1,trees.getEntries().size()); assertEquals(now.minusSeconds(1200),trees.getEntries().get(0).getFinishedAt());
        try { all.getEntries().clear(); fail("Mutable summary"); } catch(UnsupportedOperationException expected) { }
    }
    @Test public void unknownAndZeroDurationXpDoNotInventAUsableRate()
    {
        RunHistorySummary unknown=RunHistorySummary.create(List.of(entry("Herb run",now,Duration.ofMinutes(1),false,0,true)),RunHistoryPeriod.ALL,null,now);
        assertEquals(0,unknown.getKnownXpCount()); assertFalse(unknown.getXpPerHour().isPresent());
        RunHistorySummary zero=RunHistorySummary.create(List.of(entry("Field run",now,Duration.ZERO,true,100,false)),RunHistoryPeriod.ALL,null,now);
        assertEquals(100,zero.getKnownXp()); assertFalse(zero.getXpPerHour().isPresent());
        RunHistorySummary knownZero=RunHistorySummary.create(List.of(entry("Field run",now,Duration.ofSeconds(1),true,0,false)),RunHistoryPeriod.ALL,null,now);
        assertEquals(0d,knownZero.getXpPerHour().getAsDouble(),0d);
    }
    @Test public void invalidOrActiveSnapshotsCannotAppearAsSavedHistory()
    {
        RunMetricsSnapshot future=entry("Herb run",now.plusSeconds(1),Duration.ofMinutes(1),true,100,true);
        RunMetricsSnapshot active=new RunMetricsSnapshot("Field run",true,false,true,100,Duration.ofSeconds(1),now.minusSeconds(1),now);
        RunMetricsSnapshot unknownKind=entry("Unrecognized run",now,Duration.ofMinutes(1),true,100,true);
        RunMetricsSnapshot oldShape=new RunMetricsSnapshot("Tree run",false,true,true,100,Duration.ofSeconds(1));
        assertTrue(RunHistorySummary.create(List.of(future,active,unknownKind,oldShape),RunHistoryPeriod.ALL,null,now).getEntries().isEmpty());
    }
    @Test public void inconsistentElapsedCannotDistortRecordedRate()
    {
        RunMetricsSnapshot inconsistent=new RunMetricsSnapshot("Herb run",false,true,true,100,Duration.ofSeconds(1),now.minusSeconds(60),now);
        assertTrue(RunHistorySummary.create(List.of(inconsistent),RunHistoryPeriod.ALL,null,now).getEntries().isEmpty());
    }
    @Test public void subtotalUsesLongAndRateRetainsFractionalSeconds()
    {
        RunMetricsSnapshot first=entry("Herb run",now,Duration.ofMillis(1500),true,Integer.MAX_VALUE,true);
        RunMetricsSnapshot second=entry("Tree run",now,Duration.ofMillis(1500),true,Integer.MAX_VALUE,true);
        RunHistorySummary summary=RunHistorySummary.create(List.of(first,second),RunHistoryPeriod.ALL,null,now);
        assertEquals(2L*Integer.MAX_VALUE,summary.getKnownXp());
        assertEquals(2d*Integer.MAX_VALUE*3600/3,summary.getXpPerHour().getAsDouble(),0.001);
    }
}
