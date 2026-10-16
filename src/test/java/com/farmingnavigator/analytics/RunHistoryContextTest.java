package com.farmingnavigator.analytics;

import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import com.farmingnavigator.routing.run.*;
import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistoryContextTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final Map<String,String> records=new HashMap<>();
    private int writes;
    private final ObservationPersistenceBackend backend=new ObservationPersistenceBackend()
    {
        public String read(String profile,String key){assertEquals("runHistoryV1",key);return records.get(profile);}
        public void write(String profile,String key,String value){assertEquals("runHistoryV1",key);records.put(profile,value);writes++;}
    };
    private RunMetricsSnapshot entry(RunHistoryStore.Kind kind,String crop,int count,int ago)
    {
        RunMetricsTracker tracker=new RunMetricsTracker();tracker.start(kind.toString(),100,now.minusSeconds(ago).plusNanos(123),RunHistoryContext.of(kind,crop,count));
        tracker.finish(250,now.minusSeconds(ago-60).plusNanos(123),true);return tracker.snapshot(now);
    }
    @Test public void everyCurrentCropHasAValidatedContextButInvalidKeysAndStopCountsDoNot()
    {
        for(HerbRunCrop crop:HerbRunCrop.values()){assertEquals(crop.getDisplayName(),RunHistoryContext.of(RunHistoryStore.Kind.HERB,crop.name(),1).getCropLabel());}
        for(TreeRunCrop crop:TreeRunCrop.values()){assertEquals(crop.toString(),RunHistoryContext.of(RunHistoryStore.Kind.TREE,crop.name(),1).getCropLabel());}
        for(GardenRunCrop crop:GardenRunCrop.values()){assertEquals(crop.toString(),RunHistoryContext.of(RunHistoryStore.Kind.GARDEN,crop.name(),1).getCropLabel());}
        for(FieldRunCrop crop:FieldRunCrop.values()){assertEquals(crop.toString(),RunHistoryContext.of(RunHistoryStore.Kind.FIELD,crop.name(),1).getCropLabel());}
        for(int count:new int[]{Integer.MIN_VALUE,-1,0,9999,Integer.MAX_VALUE})
        {try{RunHistoryContext.of(RunHistoryStore.Kind.HERB,"RANARR",count);fail("Bad count");}catch(IllegalArgumentException expected){}}
        for(String key:Arrays.asList(null,"", "POTATO","RANARR|x","<html>"))
        {try{RunHistoryContext.of(RunHistoryStore.Kind.HERB,key,1);fail("Bad crop key");}catch(IllegalArgumentException expected){}}
    }
    @Test public void allFourContextsRoundTripWithActualXpAndNanosecondTimestamps()
    {
        RunHistoryStore store=new RunHistoryStore(backend);store.loadProfile("A",now);
        RunHistoryStore.Kind[] kinds=RunHistoryStore.Kind.values();String[] crops={"RANARR","OAK","REDBERRIES","POTATO"};
        for(int i=0;i<kinds.length;i++){assertTrue(store.observe(entry(kinds[i],crops[i],i+1,300-i*60),true,now));}
        assertTrue(records.get("A").startsWith("2\n"));store.detach();store.loadProfile("A",now);assertEquals(4,store.snapshot().size());
        for(RunMetricsSnapshot saved:store.snapshot())
        {
            int index=RunHistoryStore.Kind.fromLabel(saved.getRunName()).ordinal();assertTrue(saved.getContext().isKnown());assertEquals(crops[index],saved.getContext().getCropKey());
            assertEquals(index+1,saved.getContext().getPlannedStops());assertEquals(123,saved.getStartedAt().getNano());assertEquals(150,saved.getXpGained());assertEquals(Duration.ofMinutes(1),saved.getElapsed());
        }
    }
    @Test public void versionOneLoadsWithoutInventingSetupAndUpgradesOnlyOnTheNextWrite()
    {
        String legacy="1\nHERB|"+now.minusSeconds(300)+"|"+now.minusSeconds(240)+"|1|1|150";records.put("A",legacy);
        RunHistoryStore store=new RunHistoryStore(backend);store.loadProfile("A",now);assertEquals(1,store.snapshot().size());assertFalse(store.snapshot().get(0).getContext().isKnown());assertEquals(0,writes);assertEquals(legacy,records.get("A"));
        assertTrue(store.observe(entry(RunHistoryStore.Kind.FIELD,"POTATO",2,120),true,now));assertEquals(1,writes);assertTrue(records.get("A").startsWith("2\n"));
        store.detach();store.loadProfile("A",now);assertEquals(2,store.snapshot().size());assertTrue(store.snapshot().get(0).getContext().isKnown());assertFalse(store.snapshot().get(1).getContext().isKnown());assertEquals(300,store.snapshot().stream().mapToLong(RunMetricsSnapshot::getXpGained).sum());
        store.clear();assertEquals("2",records.get("A"));store.detach();store.loadProfile("A",now);assertTrue(store.snapshot().isEmpty());
    }
    @Test public void malformedVersionTwoContextsCannotCorruptOtherRecordsOrCrossProfiles()
    {
        String prefix="FIELD|"+now.minusSeconds(120)+"|"+now.minusSeconds(60)+"|1|1|100|";
        records.put("A",String.join("\n","2",prefix+"POTATO|2",prefix+"POTATO|0",prefix+"RANARR|1",prefix+"POTATO|9999",prefix+"POTATO|-1",prefix+"-|1",prefix+"|0",prefix+"POTATO|1|extra"));
        RunHistoryStore store=new RunHistoryStore(backend);store.loadProfile("A",now);assertEquals(1,store.snapshot().size());assertEquals(2,store.snapshot().get(0).getContext().getPlannedStops());
        store.loadProfile("B",now);assertTrue(store.snapshot().isEmpty());assertEquals(0,writes);
        records.put("C","3\n"+prefix+"POTATO|2");store.loadProfile("C",now);assertTrue(store.snapshot().isEmpty());
        records.put("D","2\n"+prefix+"-|0");store.loadProfile("D",now);assertEquals(1,store.snapshot().size());assertFalse(store.snapshot().get(0).getContext().isKnown());
    }
    @Test public void startContextIsFrozenAcrossSwitchingRunsAndLegacyStartNeverBorrowsOldSetup()
    {
        RunMetricsTracker tracker=new RunMetricsTracker();RunHistoryContext herb=RunHistoryContext.of(RunHistoryStore.Kind.HERB,"RANARR",4);
        tracker.start("Herb run",100,now.minusSeconds(180),herb);tracker.finish(200,now.minusSeconds(120),false);RunMetricsSnapshot first=tracker.snapshot(now);
        tracker.start("Field run",200,now.minusSeconds(120),RunHistoryContext.of(RunHistoryStore.Kind.FIELD,"POTATO",2));tracker.finish(300,now.minusSeconds(60),true);
        assertEquals("RANARR",first.getContext().getCropKey());assertEquals(4,first.getContext().getPlannedStops());assertEquals("POTATO",tracker.snapshot(now).getContext().getCropKey());
        tracker.start("Tree run",300,now.minusSeconds(60));assertFalse(tracker.snapshot(now).getContext().isKnown());tracker.clear();assertNull(tracker.snapshot(now));
    }
    @Test public void mismatchedKindCannotBeRecordedAndANewSetupDoesNotChangeDeduplicationIdentity()
    {
        RunHistoryContext wrong=RunHistoryContext.of(RunHistoryStore.Kind.GARDEN,"REDBERRIES",1);
        try{new RunMetricsTracker().start("Herb run",100,now,wrong);fail("Mismatched tracker context");}catch(IllegalArgumentException expected){}
        RunHistoryStore store=new RunHistoryStore(backend);store.loadProfile("A",now);
        RunMetricsSnapshot invalid=new RunMetricsSnapshot("Herb run",false,true,true,100,Duration.ofSeconds(60),now.minusSeconds(120),now.minusSeconds(60),wrong);
        assertFalse(store.observe(invalid,true,now));assertTrue(store.snapshot().isEmpty());
        assertTrue(store.observe(entry(RunHistoryStore.Kind.HERB,"RANARR",1,120),true,now));
        assertFalse(store.observe(entry(RunHistoryStore.Kind.HERB,"SNAPDRAGON",2,120),true,now));assertEquals(1,store.snapshot().size());
    }
}
