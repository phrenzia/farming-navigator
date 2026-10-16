package com.farmingnavigator.analytics;

import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistoryComparisonTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RunMetricsSnapshot entry(String name,int ago,Duration elapsed,boolean complete,boolean known,long xp)
    { Instant end=now.minusSeconds(ago); return new RunMetricsSnapshot(name,false,complete,known,xp,elapsed,end.minus(elapsed),end); }
    @Test public void completedTimeExcludesStoppedAndZeroDurationWhileUnknownXpStillCountsTime()
    {
        RunHistoryComparison comparison=RunHistoryComparison.create(List.of(
                entry("Herb run",1,Duration.ofMinutes(2),true,true,120),
                entry("Herb run",2,Duration.ofMinutes(4),true,false,0),
                entry("Herb run",3,Duration.ofMinutes(30),false,true,1800),
                entry("Herb run",4,Duration.ZERO,true,true,100),
                entry("Tree run",5,Duration.ofMinutes(1),false,false,0)),RunHistoryPeriod.ALL,now);
        assertEquals(2,comparison.getRows().size()); RunHistoryComparison.Row row=comparison.getRows().get(0);
        assertEquals(RunHistoryStore.Kind.HERB,row.getKind()); assertEquals(3,row.getSummary().getCompletedCount()); assertEquals(1,row.getSummary().getStoppedCount());
        assertEquals(2,row.getTimedCompletedCount()); assertEquals(Duration.ofMinutes(3),row.getMeanCompletedTime().get());
        assertEquals(Duration.ofMinutes(2),row.getMinimumCompletedTime().get()); assertEquals(Duration.ofMinutes(4),row.getMaximumCompletedTime().get());
        assertEquals(2020,row.getSummary().getKnownXp()); assertEquals(3600d*1920/1920,row.getSummary().getXpPerHour().getAsDouble(),0.001);
        RunHistoryComparison.Row tree=comparison.getRows().get(1); assertFalse(tree.getMeanCompletedTime().isPresent()); assertFalse(tree.getSummary().getXpPerHour().isPresent());
    }
    @Test public void globalTwentyRecordLimitAppliesBeforeTypeComparisonAndDuplicatesCannotResurrectOldTypes()
    {
        List<RunMetricsSnapshot> history=new ArrayList<>();
        for(int i=1;i<=20;i++) { history.add(entry("Herb run",i,Duration.ofSeconds(1),true,true,1)); }
        history.add(history.get(0)); history.add(entry("Tree run",21,Duration.ofMinutes(1),true,true,100));
        RunHistoryComparison comparison=RunHistoryComparison.create(history,RunHistoryPeriod.ALL,now); assertEquals(1,comparison.getRows().size());
        assertEquals(20,comparison.getRows().get(0).getSummary().getKnownXp());
        try { comparison.getRows().clear(); fail("Mutable comparison"); } catch(UnsupportedOperationException expected) { }
        try { comparison.getRows().get(0).getSummary().getEntries().clear(); fail("Mutable entries"); } catch(UnsupportedOperationException expected) { }
        history.clear(); assertEquals(20,comparison.getRows().get(0).getTimedCompletedCount());
    }
    @Test public void rollingPeriodsUseEndTimeAndDoNotCreateEmptyRows()
    {
        List<RunMetricsSnapshot> entries=List.of(entry("Tree run",86400,Duration.ofHours(1),true,true,1),entry("Field run",86401,Duration.ofMinutes(1),true,true,1));
        RunHistoryComparison comparison=RunHistoryComparison.create(entries,RunHistoryPeriod.LAST_24_HOURS,now);
        assertEquals(1,comparison.getRows().size()); assertEquals(RunHistoryStore.Kind.TREE,comparison.getRows().get(0).getKind());
        assertTrue(RunHistoryComparison.create(entries,RunHistoryPeriod.LAST_24_HOURS,now.plusNanos(1)).getRows().isEmpty());
    }
    @Test public void invalidSnapshotsAndInconsistentElapsedNeverDistortComparisons()
    {
        RunMetricsSnapshot future=entry("Herb run",-1,Duration.ofMinutes(1),true,true,10);
        RunMetricsSnapshot mismatch=new RunMetricsSnapshot("Tree run",false,true,true,1,Duration.ofMinutes(1),now.minusSeconds(5),now);
        RunMetricsSnapshot active=new RunMetricsSnapshot("Field run",true,false,true,1,Duration.ofMinutes(1),now.minusSeconds(60),now);
        assertTrue(RunHistoryComparison.create(Arrays.asList(future,mismatch,active,null),RunHistoryPeriod.ALL,now).getRows().isEmpty());
    }
    @Test public void fractionalTimeAndLargeValidSamplesRemainAccurateAndBounded()
    {
        RunHistoryComparison.Row row=RunHistoryComparison.create(List.of(entry("Herb run",1,Duration.ofMillis(1500),true,true,1),
                entry("Herb run",2,Duration.ofMillis(2500),true,true,1)),RunHistoryPeriod.ALL,now).getRows().get(0);
        assertEquals(Duration.ofSeconds(2),row.getMeanCompletedTime().get()); assertEquals(Duration.ofMillis(1500),row.getMinimumCompletedTime().get());
        List<RunMetricsSnapshot> max=new ArrayList<>(); for(int i=1;i<=20;i++) {max.add(entry("Field run",i,Duration.ofDays(30),true,true,Integer.MAX_VALUE));}
        row=RunHistoryComparison.create(max,RunHistoryPeriod.ALL,now).getRows().get(0);
        assertEquals(Duration.ofDays(30),row.getMeanCompletedTime().get()); assertEquals(20L*Integer.MAX_VALUE,row.getSummary().getKnownXp());
    }
}
