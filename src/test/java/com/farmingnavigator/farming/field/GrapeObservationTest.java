package com.farmingnavigator.farming.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.transport.FieldPatchTransportService;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class GrapeObservationTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    @Test public void allTwelveNibbleChannelsAreIndependentAndSampleOnlyAfterSettledArrival()
    {
        FieldPatchObservationStore store=new FieldPatchObservationStore(); FieldPatchObservationSampler sampler=new FieldPatchObservationSampler(store);
        List<FieldPatchDefinition> patches=FieldPatchRegion.VINERY.getPatches(); assertEquals(12,patches.size());
        Map<Integer,Integer> raw=new HashMap<>(); int[] values={0,1,2,3,4,5,6,7,8,9,10,15};
        for(int i=0;i<patches.size();i++) { assertNull(raw.put(patches.get(i).getVarbit(),values[i])); }
        AtomicInteger reads=new AtomicInteger();
        for(int tick=1;tick<=3;tick++)
        {
            assertEquals(tick==3,sampler.sample(patches.get(0).getLocation(),true,false,false,tick,id -> {
                reads.incrementAndGet(); assertTrue(raw.containsKey(id)); return raw.get(id);
            },now.plusSeconds(tick),FarmingGrowthRate.NORMAL));
            assertEquals(tick==3?12:0,reads.get());
        }
        assertEquals(12,store.snapshot().size());
        for(FieldPatchDefinition patch:patches) { assertEquals((int)raw.get(patch.getVarbit()),store.snapshot().get(patch).getState().getRawValue()); }
        assertEquals(FieldPatchCondition.HARVESTABLE,store.snapshot().get(FieldPatchDefinition.VINERY_WEST_6).getState().getCondition());
        assertEquals(FieldPatchRegion.HOSIDIUS,FieldPatchRegion.forLocation(FieldPatchDefinition.HOSIDIUS_NORTH.getLocation()));
    }
    @Test public void fertiliserEmptyAndTerminalGrowingCodesDoNotInventWateringOrHarvestEvidence()
    {
        FieldPatchStateDecoder decoder=new FieldPatchStateDecoder();
        for(int raw:new int[]{0,1}) { assertEquals(FieldPatchCondition.EMPTY,decoder.decode(FieldFamily.GRAPES,raw).getCondition()); assertFalse(decoder.decode(FieldFamily.GRAPES,raw).isWatered()); }
        for(int raw:new int[]{9,10})
        {
            FieldPatchState state=decoder.decode(FieldFamily.GRAPES,raw); assertEquals(7,state.getStage()); assertEquals(FieldPatchCondition.GROWING,state.getCondition());
            assertFalse(state.isWatered()); assertFalse(new FieldGrowthTimer().estimate(new FieldPatchObservation(state,now,FarmingGrowthRate.NORMAL),now).isPresent());
        }
        assertEquals(FieldPatchCondition.UNKNOWN,decoder.decode(FieldFamily.GRAPES,16).getCondition());
        for(int raw=11;raw<=15;raw++) { assertEquals(raw-11,decoder.decode(FieldFamily.GRAPES,raw).getStage()); assertEquals(FieldPatchCondition.HARVESTABLE,decoder.decode(FieldFamily.GRAPES,raw).getCondition()); }
    }
    @Test public void normalAndLeaguesEstimatesFollowPinnedMetadataAndRemainConditional()
    {
        for(FarmingGrowthRate rate:List.of(FarmingGrowthRate.NORMAL,FarmingGrowthRate.LEAGUES))
        {
            FieldPatchObservation observation=new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.GRAPES,2),now,rate);
            FieldGrowthWindow window=new FieldGrowthTimer().estimate(observation,now).get(); int divisor=rate==FarmingGrowthRate.LEAGUES?5:1;
            assertEquals(now.plusSeconds(30*60/divisor),window.getEarliest()); assertEquals(now.plusSeconds(35*60/divisor),window.getLatest());
            assertEquals(FieldPatchCondition.GROWING,observation.getState().getCondition());
        }
    }
    @Test public void vineyardRecordsRemainSeparatedAcrossRestartAndProfiles()
    {
        Map<String,String> records=new HashMap<>(); FieldObservationPersistence persistence=new FieldObservationPersistence(new ObservationPersistenceBackend() {
            public String read(String profile,String key) { return records.get(profile+key); }
            public void write(String profile,String key,String value) { records.put(profile+key,value); }
        });
        FieldPatchObservationStore store=new FieldPatchObservationStore(); persistence.loadProfile("A",store,now);
        int raw=0; for(FieldPatchDefinition patch:FieldPatchRegion.VINERY.getPatches()) { store.observe(patch,raw++,now,FarmingGrowthRate.NORMAL); }
        persistence.save(store.snapshot()); persistence.detach(); persistence.loadProfile("A",store,now.plusSeconds(1));
        assertEquals(12,store.snapshot().size()); raw=0;
        for(FieldPatchDefinition patch:FieldPatchRegion.VINERY.getPatches()) { assertEquals(raw++,store.snapshot().get(patch).getState().getRawValue()); }
        persistence.loadProfile("B",store,now.plusSeconds(1)); assertTrue(store.isEmpty());
    }
    @Test public void observationFoundationCannotEnableUnverifiedGrapePreparationOrRoutes()
    {
        for(FieldRunCrop crop:FieldRunCrop.values()) { assertNotEquals(FieldCrop.GRAPE,crop.getCrop()); }
        AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
        Map<?,?> routes=new FieldPatchTransportService().getAllOptions();
        for(FieldPatchDefinition patch:FieldPatchRegion.VINERY.getPatches())
        {
            assertTrue(new FieldPatchAccess().missingRequirement(patch,account).contains("Data required")); assertFalse(routes.containsKey(patch));
        }
        FieldRunPlan plan=new FieldRunPlanner().plan(account,EnumSet.copyOf(FieldPatchRegion.VINERY.getPatches()),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,false,Map.of(),null,now);
        assertTrue(plan.getStops().isEmpty()); assertTrue(plan.getSupplies().isEmpty());
    }
}
