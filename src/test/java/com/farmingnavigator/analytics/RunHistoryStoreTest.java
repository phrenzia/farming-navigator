package com.farmingnavigator.analytics;

import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistoryStoreTest
{
    private final Instant now=Instant.parse("2026-10-15T12:00:00Z");
    private final Map<String,String> records=new HashMap<>();
    private int writes;
    private final ObservationPersistenceBackend backend=new ObservationPersistenceBackend()
    {
        @Override public String read(String profile,String key) { assertEquals(RunHistoryStore.KEY,key); return records.get(profile); }
        @Override public void write(String profile,String key,String value) { assertEquals(RunHistoryStore.KEY,key); records.put(profile,value); writes++; }
    };
    private final RunHistoryStore store=new RunHistoryStore(backend);
    private RunMetricsSnapshot ended(String name,Instant start,boolean known,boolean completed)
    {
        RunMetricsTracker tracker=new RunMetricsTracker(); tracker.start(name,known?100:-1,start);
        tracker.finish(known?250:-1,start.plusSeconds(60),completed); return tracker.snapshot(now);
    }
    @Test public void allFourFamiliesRoundTripCompletionTimeAndActualXpWithoutDuplicates()
    {
        store.loadProfile("A",now);
        String[] names={"Herb run","Tree run","Bush/cactus run","Field run"};
        for(int i=0;i<names.length;i++)
        {
            RunMetricsSnapshot entry=ended(names[i],now.minusSeconds(300-i*60),true,i%2==0);
            assertTrue(store.observe(entry,true,now)); assertFalse(store.observe(entry,true,now));
        }
        assertEquals(4,writes); RunHistoryStore restarted=new RunHistoryStore(backend); restarted.loadProfile("A",now);
        assertEquals(4,restarted.snapshot().size()); RunMetricsSnapshot latest=restarted.snapshot().get(0);
        assertEquals("Field run",latest.getRunName()); assertFalse(latest.isCompleted()); assertEquals(150,latest.getXpGained());
        assertEquals(Duration.ofSeconds(60),latest.getElapsed()); assertEquals(9000,latest.getXpPerHour().getAsDouble(),0.001);
        assertFalse(restarted.observe(latest,true,now));
    }
    @Test public void unknownXpRemainsUnknownAfterRestart()
    {
        store.loadProfile("A",now); store.observe(ended("Tree run",now.minusSeconds(120),false,true),true,now);
        store.detach(); store.loadProfile("A",now);
        RunMetricsSnapshot entry=store.snapshot().get(0); assertFalse(entry.isXpKnown()); assertFalse(entry.getXpPerHour().isPresent()); assertEquals(0,entry.getXpGained());
    }
    @Test public void activeDisabledAndLateEnabledRunsDoNotCreateHistory()
    {
        store.loadProfile("A",now); RunMetricsTracker active=new RunMetricsTracker(); active.start("Herb run",100,now.minusSeconds(10));
        assertFalse(store.observe(active.snapshot(now),true,now));
        RunMetricsSnapshot ended=ended("Herb run",now.minusSeconds(120),true,true);
        assertFalse(store.observe(ended,false,now)); assertFalse(store.observe(ended,true,now.plusSeconds(1)));
        assertTrue(store.snapshot().isEmpty()); assertEquals(0,writes);
    }
    @Test public void missingProfileNeverWritesAndCharactersAndModesStaySeparate()
    {
        RunMetricsSnapshot entry=ended("Herb run",now.minusSeconds(120),true,true);
        assertFalse(store.observe(entry,true,now)); assertEquals(0,writes);
        store.loadProfile("A-normal",now); store.observe(entry,true,now);
        store.loadProfile("A-leagues",now); assertTrue(store.snapshot().isEmpty());
        store.observe(ended("Field run",now.minusSeconds(180),true,false),true,now);
        store.loadProfile("B-normal",now); assertTrue(store.snapshot().isEmpty());
        store.loadProfile("A-normal",now); assertEquals("Herb run",store.snapshot().get(0).getRunName());
        store.loadProfile(null,now); assertTrue(store.snapshot().isEmpty()); assertEquals(2,writes);
    }
    @Test public void retainsOnlyNewestTwentyEntriesAndSnapshotsAreImmutable()
    {
        store.loadProfile("A",now);
        for(int i=0;i<25;i++) { store.observe(ended("Herb run",now.minusSeconds(1000-i*2),true,true),true,now); }
        assertEquals(20,store.snapshot().size()); assertEquals(now.minusSeconds(952),store.snapshot().get(0).getStartedAt());
        assertEquals(now.minusSeconds(990),store.snapshot().get(19).getStartedAt());
        store.detach(); store.loadProfile("A",now); assertEquals(20,store.snapshot().size());
        try { store.snapshot().clear(); fail("Mutable history"); } catch(UnsupportedOperationException expected) { }
    }
    @Test public void explicitClearPersistsAndRefreshingLastRunDoesNotRestoreIt()
    {
        store.loadProfile("A",now); RunMetricsSnapshot entry=ended("Herb run",now.minusSeconds(120),true,true);
        store.observe(entry,true,now); store.clear(); assertTrue(store.snapshot().isEmpty());
        assertFalse(store.observe(entry,true,now)); assertEquals("1",records.get("A"));
        store.detach(); store.loadProfile("A",now); assertTrue(store.snapshot().isEmpty());
    }
    @Test public void invalidCorruptFutureUnknownAndNegativeRecordsAreIgnored()
    {
        String good="FIELD|"+now.minusSeconds(120)+"|"+now.minusSeconds(60)+"|1|1|150";
        records.put("A",String.join("\n","1",good,good,"broken","OTHER|a|b|1|1|2",
                "FIELD|not-a-time|"+now+"|1|1|0","FIELD|"+now+"|"+now.plusSeconds(1)+"|1|1|0",
                "FIELD|"+now+"|"+now.minusSeconds(1)+"|1|1|0",
                "FIELD|"+now.minusSeconds(1)+"|"+now+"|2|1|0",
                "FIELD|"+now.minusSeconds(1)+"|"+now+"|1|0|10",
                "FIELD|"+now.minusSeconds(1)+"|"+now+"|1|1|-1",
                "FIELD|"+now.minus(Duration.ofDays(31))+"|"+now+"|1|1|0"));
        store.loadProfile("A",now); assertEquals(1,store.snapshot().size()); assertEquals(0,writes);
        records.put("B","2\n"+good); store.loadProfile("B",now); assertTrue(store.snapshot().isEmpty());
        records.put("C","1\n"+"x".repeat(8193)); store.loadProfile("C",now); assertTrue(store.snapshot().isEmpty());
    }
    @Test public void nanosecondTimestampsSurviveSerializationWithoutChangingRates()
    {
        Instant start=now.minusSeconds(120).plusNanos(123); RunMetricsSnapshot entry=ended("Herb run",start,true,true);
        store.loadProfile("A",now); store.observe(entry,true,now); store.detach(); store.loadProfile("A",now);
        assertEquals(start,store.snapshot().get(0).getStartedAt()); assertEquals(entry.getFinishedAt(),store.snapshot().get(0).getFinishedAt());
        assertEquals(entry.getElapsed(),store.snapshot().get(0).getElapsed());
    }
    @Test public void unknownRunNamesAndFutureFinishTimesCannotBeSaved()
    {
        store.loadProfile("A",now); assertFalse(store.observe(ended("Unknown run",now.minusSeconds(120),true,true),true,now));
        assertFalse(store.observe(ended("Herb run",now,true,true),true,now)); assertEquals(0,writes);
    }
    @Test public void switchingRunTypesKeepsTheStoppedBoundaryWithoutDoubleCountingXp()
    {
        store.loadProfile("A",now); RunMetricsTracker tracker=new RunMetricsTracker();
        tracker.start("Herb run",100,now.minusSeconds(120)); tracker.finish(200,now.minusSeconds(60),false);
        assertTrue(store.observe(tracker.snapshot(now),true,now));
        tracker.start("Field run",200,now.minusSeconds(60)); tracker.finish(300,now,true);
        assertTrue(store.observe(tracker.snapshot(now),true,now)); assertEquals(2,store.snapshot().size());
        assertEquals(200,store.snapshot().stream().mapToLong(RunMetricsSnapshot::getXpGained).sum());
        assertTrue(store.snapshot().get(0).isCompleted()); assertFalse(store.snapshot().get(1).isCompleted());
    }

}
