package com.farmingnavigator.analytics;

import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistoryOutcomeTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RunMetricsSnapshot entry(String kind,int ago,Duration elapsed,boolean complete,boolean known,long xp)
    {Instant end=now.minusSeconds(ago);return new RunMetricsSnapshot(kind,false,complete,known,xp,elapsed,end.minus(elapsed),end);}
    @Test public void completedAndStoppedFiltersRecomputeCountsRatesAndUnknownXpFromExactlyTheirSample()
    {
        List<RunMetricsSnapshot> history=List.of(entry("Herb run",1,Duration.ofMinutes(10),true,true,100),entry("Tree run",2,Duration.ofMinutes(1),false,true,900),entry("Field run",3,Duration.ofMinutes(1),true,false,0));
        RunHistorySummary completed=RunHistorySummary.create(history,RunHistoryPeriod.ALL,null,now,RunHistoryOutcome.COMPLETED);
        assertEquals(2,completed.getCompletedCount());assertEquals(0,completed.getStoppedCount());assertEquals(100,completed.getKnownXp());assertEquals(1,completed.getUnknownXpCount());
        assertEquals(600d,completed.getXpPerHour().getAsDouble(),0.001);assertEquals(Duration.ofMinutes(11),completed.getElapsed());
        RunHistorySummary stopped=RunHistorySummary.create(history,RunHistoryPeriod.ALL,null,now,RunHistoryOutcome.STOPPED);
        assertEquals(0,stopped.getCompletedCount());assertEquals(1,stopped.getStoppedCount());assertEquals(900,stopped.getKnownXp());assertEquals(54000d,stopped.getXpPerHour().getAsDouble(),0.001);
        assertEquals(3,RunHistorySummary.create(history,RunHistoryPeriod.ALL,null,now).getEntries().size());
        assertEquals(3,RunHistorySummary.create(history,RunHistoryPeriod.ALL,null,now,null).getEntries().size());
    }
    @Test public void outcomesCannotRestoreRecordsOutsideTheGlobalLastTwentyOrInflateDuplicates()
    {
        List<RunMetricsSnapshot> history=new ArrayList<>();for(int i=1;i<=20;i++){history.add(entry("Herb run",i,Duration.ofSeconds(1),false,true,1));}
        history.add(history.get(0));history.add(entry("Tree run",21,Duration.ofSeconds(1),true,true,100));
        assertTrue(RunHistorySummary.create(history,RunHistoryPeriod.ALL,null,now,RunHistoryOutcome.COMPLETED).getEntries().isEmpty());
        assertEquals(20,RunHistorySummary.create(history,RunHistoryPeriod.ALL,null,now,RunHistoryOutcome.STOPPED).getEntries().size());
    }
    @Test public void periodAndKindAreIntersectedWithOutcomeAfterValidation()
    {
        List<RunMetricsSnapshot> history=Arrays.asList(entry("Tree run",86400,Duration.ofMinutes(1),true,true,10),entry("Tree run",60,Duration.ofMinutes(1),false,true,20),entry("Herb run",60,Duration.ofMinutes(1),true,true,30),entry("Tree run",86401,Duration.ofMinutes(1),true,true,40),entry("Tree run",-1,Duration.ofMinutes(1),true,true,99),null);
        RunHistorySummary summary=RunHistorySummary.create(history,RunHistoryPeriod.LAST_24_HOURS,RunHistoryStore.Kind.TREE,now,RunHistoryOutcome.COMPLETED);
        assertEquals(1,summary.getEntries().size());assertEquals(10,summary.getKnownXp());
        assertTrue(RunHistorySummary.create(history,RunHistoryPeriod.LAST_24_HOURS,RunHistoryStore.Kind.TREE,now.plusNanos(1),RunHistoryOutcome.COMPLETED).getEntries().isEmpty());
    }
    @Test public void zeroDurationCompletedRecordsStayVisibleWithoutInventingARateOrCompletionTime()
    {
        List<RunMetricsSnapshot> history=List.of(entry("Field run",1,Duration.ZERO,true,true,100),entry("Tree run",2,Duration.ofMinutes(1),false,true,200));
        RunHistorySummary summary=RunHistorySummary.create(history,RunHistoryPeriod.ALL,null,now,RunHistoryOutcome.COMPLETED);
        assertEquals(1,summary.getCompletedCount());assertFalse(summary.getXpPerHour().isPresent());assertEquals(100,summary.getKnownXp());
        RunHistoryComparison comparison=RunHistoryComparison.create(summary.getEntries(),RunHistoryPeriod.ALL,now);
        assertEquals(1,comparison.getRows().size());assertEquals(RunHistoryStore.Kind.FIELD,comparison.getRows().get(0).getKind());assertFalse(comparison.getRows().get(0).getMeanCompletedTime().isPresent());
    }
}
