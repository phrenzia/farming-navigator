package com.farmingnavigator.analytics;

import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistoryCropComparisonTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RunMetricsSnapshot entry(String crop,int stops,int ago,int seconds,boolean completed,boolean known,long xp)
    {Instant end=now.minusSeconds(ago);return new RunMetricsSnapshot("Herb run",false,completed,known,xp,Duration.ofSeconds(seconds),end.minusSeconds(seconds),end,crop==null?RunHistoryContext.UNKNOWN:RunHistoryContext.of(RunHistoryStore.Kind.HERB,crop,stops));}
    @Test public void knownCropsAndLegacyUnknownSetupStaySeparateWithWeightedRatesAndStopRanges()
    {
        List<RunMetricsSnapshot> entries=List.of(entry("RANARR",1,600,60,true,true,100),entry("RANARR",5,400,120,false,true,300),entry("SNAPDRAGON",4,200,120,true,true,600),entry(null,0,100,60,true,true,50));
        RunHistoryComparison.Row herb=RunHistoryComparison.create(entries,RunHistoryPeriod.ALL,now).getRows().get(0);assertEquals(3,herb.getCrops().size());
        RunHistoryComparison.CropRow ranarr=herb.getCrops().get(0);assertEquals("RANARR",ranarr.getCropKey());assertEquals("Ranarr",ranarr.getCropLabel());assertTrue(ranarr.isKnown());
        assertEquals(1,ranarr.getMinimumPlannedStops());assertEquals(5,ranarr.getMaximumPlannedStops());assertEquals(400,ranarr.getStatistics().getSummary().getKnownXp());assertEquals(8000d,ranarr.getStatistics().getSummary().getXpPerHour().getAsDouble(),0.001);
        assertEquals(Duration.ofSeconds(60),ranarr.getStatistics().getMeanCompletedTime().get());assertEquals(1,ranarr.getStatistics().getTimedCompletedCount());assertTrue(ranarr.getStatistics().getCrops().isEmpty());
        RunHistoryComparison.CropRow unknown=herb.getCrops().get(2);assertFalse(unknown.isKnown());assertEquals("Setup unavailable",unknown.getCropLabel());assertEquals(0,unknown.getMaximumPlannedStops());
        assertEquals(herb.getSummary().getEntries().size(),herb.getCrops().stream().mapToInt(c -> c.getStatistics().getSummary().getEntries().size()).sum());
        assertEquals(herb.getSummary().getKnownXp(),herb.getCrops().stream().mapToLong(c -> c.getStatistics().getSummary().getKnownXp()).sum());
    }
    @Test public void groupingNeverRestoresCropsOutsideTheGlobalLimitOrCurrentOutcomeSample()
    {
        List<RunMetricsSnapshot> entries=new ArrayList<>();for(int i=1;i<=20;i++){entries.add(entry("RANARR",1,i*60,1,false,true,1));}
        entries.add(entries.get(0));entries.add(entry("SNAPDRAGON",1,1260,1,true,true,99));
        RunHistoryComparison comparison=RunHistoryComparison.create(entries,RunHistoryPeriod.ALL,now);assertEquals(1,comparison.getRows().get(0).getCrops().size());assertEquals(20,comparison.getRows().get(0).getSummary().getKnownXp());
        RunHistorySummary completed=RunHistorySummary.create(entries,RunHistoryPeriod.ALL,null,now,RunHistoryOutcome.COMPLETED);
        assertTrue(RunHistoryComparison.create(completed.getEntries(),RunHistoryPeriod.ALL,now).getRows().isEmpty());
    }
    @Test public void zeroDurationAndUnknownXpDoNotInventCropRatesOrLoseValidCompletionTimes()
    {
        RunHistoryComparison.CropRow crop=RunHistoryComparison.create(List.of(entry("RANARR",1,10,0,true,true,100),entry("RANARR",2,20,60,true,false,0)),RunHistoryPeriod.ALL,now).getRows().get(0).getCrops().get(0);
        assertEquals(100,crop.getStatistics().getSummary().getKnownXp());assertFalse(crop.getStatistics().getSummary().getXpPerHour().isPresent());
        assertEquals(Duration.ofMinutes(1),crop.getStatistics().getMeanCompletedTime().get());assertEquals(1,crop.getStatistics().getTimedCompletedCount());
    }
    @Test public void immutableGroupsKeepKindsAndRollingPeriodBoundariesSeparate()
    {
        RunMetricsSnapshot herb=entry(null,0,86400,60,true,true,100);
        RunMetricsSnapshot tree=new RunMetricsSnapshot("Tree run",false,true,true,200,Duration.ofSeconds(60),now.minusSeconds(120),now.minusSeconds(60));
        RunHistoryComparison comparison=RunHistoryComparison.create(List.of(herb,tree),RunHistoryPeriod.LAST_24_HOURS,now);assertEquals(2,comparison.getRows().size());
        assertFalse(comparison.getRows().get(0).getCrops().get(0).isKnown());assertFalse(comparison.getRows().get(1).getCrops().get(0).isKnown());
        assertEquals(100,comparison.getRows().get(0).getCrops().get(0).getStatistics().getSummary().getKnownXp());assertEquals(200,comparison.getRows().get(1).getCrops().get(0).getStatistics().getSummary().getKnownXp());
        assertEquals(1,RunHistoryComparison.create(List.of(herb,tree),RunHistoryPeriod.LAST_24_HOURS,now.plusNanos(1)).getRows().size());
        try{comparison.getRows().get(0).getCrops().clear();fail("Mutable crop groups");}catch(UnsupportedOperationException expected){}
    }
}
