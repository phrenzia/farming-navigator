package com.farmingnavigator.farming.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.transport.FieldPatchTransportService;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoralObservationTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    @Test public void eastAndWestChannelsCannotReadTheNearbyCalquatTransmit()
    {
        FieldPatchObservationStore store=new FieldPatchObservationStore(); FieldPatchObservationSampler sampler=new FieldPatchObservationSampler(store);
        Set<Integer> reads=new HashSet<>();
        for(int tick=1;tick<=3;tick++)
        {
            assertEquals(tick==3,sampler.sample(FieldPatchDefinition.GREAT_CONCH_CORAL_EAST.getLocation(),true,false,false,tick,id -> {
                reads.add(id); assertTrue(id==VarbitID.FARMING_TRANSMIT_A || id==VarbitID.FARMING_TRANSMIT_B); return id==VarbitID.FARMING_TRANSMIT_A?8:20;
            },now.plusSeconds(tick),FarmingGrowthRate.NORMAL));
        }
        assertEquals(2,reads.size()); assertEquals(2,store.snapshot().size());
        assertEquals(FieldPatchCondition.COLLECTABLE,store.snapshot().get(FieldPatchDefinition.GREAT_CONCH_CORAL_EAST).getState().getCondition());
        assertEquals(FieldCrop.PILLAR_CORAL,store.snapshot().get(FieldPatchDefinition.GREAT_CONCH_CORAL_WEST).getState().getCrop());
        assertEquals(FieldPatchCondition.DISEASED,store.snapshot().get(FieldPatchDefinition.GREAT_CONCH_CORAL_WEST).getState().getCondition());
    }
    @Test public void collectionStagesNeverBecomeAnotherGrowthWindowOrOrdinaryHarvestStage()
    {
        FieldPatchStateDecoder decoder=new FieldPatchStateDecoder(); FieldGrowthTimer timer=new FieldGrowthTimer();
        for(int start:new int[]{4,15,26})
        {
            FieldGrowthWindow window=timer.estimate(new FieldPatchObservation(decoder.decode(FieldFamily.CORAL,start),now,FarmingGrowthRate.NORMAL),now).get();
            assertEquals(now.plusSeconds(120*60),window.getEarliest()); assertEquals(now.plusSeconds(160*60),window.getLatest());
            FieldPatchState collect=decoder.decode(FieldFamily.CORAL,start+4); assertEquals(FieldPatchCondition.COLLECTABLE,collect.getCondition());
            assertFalse(collect.isWatered()); assertFalse(timer.estimate(new FieldPatchObservation(collect,now,FarmingGrowthRate.NORMAL),now).isPresent());
            for(int offset=5;offset<=10;offset++) { assertFalse(timer.estimate(new FieldPatchObservation(decoder.decode(FieldFamily.CORAL,start+offset),now,FarmingGrowthRate.NORMAL),now).isPresent()); }
        }
        for(int raw:new int[]{0,1,2,3,37,255}) { assertEquals(FieldPatchCondition.EMPTY,decoder.decode(FieldFamily.CORAL,raw).getCondition()); }
    }
    @Test public void persistedCollectionAndDeadStatesSurviveRestartWithoutYieldClaims()
    {
        Map<String,String> records=new HashMap<>(); FieldObservationPersistence persistence=new FieldObservationPersistence(new ObservationPersistenceBackend() {
            public String read(String profile,String key) { return records.get(profile+key); }
            public void write(String profile,String key,String value) { records.put(profile+key,value); }
        });
        FieldPatchObservationStore store=new FieldPatchObservationStore(); persistence.loadProfile("A",store,now);
        store.observe(FieldPatchDefinition.GREAT_CONCH_CORAL_EAST,30,now,FarmingGrowthRate.NORMAL);
        store.observe(FieldPatchDefinition.GREAT_CONCH_CORAL_WEST,36,now,FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); persistence.detach(); persistence.loadProfile("A",store,now.plusSeconds(1));
        assertEquals(FieldPatchCondition.COLLECTABLE,store.snapshot().get(FieldPatchDefinition.GREAT_CONCH_CORAL_EAST).getState().getCondition());
        assertEquals(FieldPatchCondition.DEAD,store.snapshot().get(FieldPatchDefinition.GREAT_CONCH_CORAL_WEST).getState().getCondition());
        persistence.loadProfile("B",store,now.plusSeconds(1)); assertTrue(store.isEmpty());
    }
    @Test public void unverifiedAccessAndPreparationCannotBecomeAUsableCoralRun()
    {
        AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
        for(FieldPatchDefinition patch:FieldPatchRegion.GREAT_CONCH.getPatches())
        {
            assertTrue(new FieldPatchAccess().missingRequirement(patch,account).contains("Data required"));
            assertFalse(new FieldPatchTransportService().getAllOptions().containsKey(patch));
        }
        for(FieldRunCrop crop:FieldRunCrop.values()) { assertNotEquals(FieldFamily.CORAL,crop.getCrop().getFamily()); }
        ReadinessDashboard dashboard=ReadinessDashboard.at(now).fields(account,EnumSet.copyOf(FieldPatchRegion.GREAT_CONCH.getPatches()),Map.of()).build();
        assertEquals(2,dashboard.getExcludedCount()); assertTrue(dashboard.getEntries().isEmpty());
    }
}
