package com.farmingnavigator.farming.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.*;
import com.farmingnavigator.routing.run.FieldPatchAccess;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldSpecialObservationTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    @Test public void underwaterChannelsAreIndependentAndRequireSettledArrival()
    {
        FieldPatchObservationStore store=new FieldPatchObservationStore(); FieldPatchObservationSampler sampler=new FieldPatchObservationSampler(store);
        for(int tick=1;tick<=3;tick++)
        {
            assertEquals(tick==3,sampler.sample(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH.getLocation(),true,false,false,tick,
                    id -> { assertTrue(id==VarbitID.FARMING_TRANSMIT_A || id==VarbitID.FARMING_TRANSMIT_B); return id==VarbitID.FARMING_TRANSMIT_A?8:11; },now.plusSeconds(tick),FarmingGrowthRate.NORMAL));
        }
        assertEquals(2,store.snapshot().size());
        assertEquals(FieldPatchCondition.HARVESTABLE,store.snapshot().get(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH).getState().getCondition());
        assertEquals(FieldPatchCondition.DISEASED,store.snapshot().get(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_SOUTH).getState().getCondition());
        for(int tick=4;tick<=6;tick++)
        {
            assertEquals(tick==6,sampler.sample(FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA.getLocation(),true,false,false,tick,id -> 8,now.plusSeconds(tick),FarmingGrowthRate.NORMAL));
        }
        assertEquals(FieldCrop.BELLADONNA,store.snapshot().get(FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA).getState().getCrop());
        assertEquals(FieldCrop.SEAWEED,store.snapshot().get(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH).getState().getCrop());
    }
    @Test public void specialGrowthWindowsUsePinnedCyclesAndDoNotEstimateHarvestStages()
    {
        FieldFamily[] families={FieldFamily.SEAWEED,FieldFamily.MUSHROOM,FieldFamily.BELLADONNA};
        int[] earliest={30,200,240},latest={40,240,320};
        for(int i=0;i<families.length;i++)
        {
            for(FarmingGrowthRate rate:List.of(FarmingGrowthRate.NORMAL,FarmingGrowthRate.LEAGUES))
            {
                FieldPatchObservation observation=new FieldPatchObservation(new FieldPatchStateDecoder().decode(families[i],4),now,rate);
                FieldGrowthWindow window=new FieldGrowthTimer().estimate(observation,now).get(); int factor=rate==FarmingGrowthRate.LEAGUES?5:1;
                assertEquals(now.plusSeconds(earliest[i]*60/factor),window.getEarliest()); assertEquals(now.plusSeconds(latest[i]*60/factor),window.getLatest());
            }
        }
        for(int raw=10;raw<=15;raw++)
        {
            FieldPatchObservation observed=new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.MUSHROOM,raw),now,FarmingGrowthRate.NORMAL);
            assertEquals(FieldPatchCondition.HARVESTABLE,observed.getState().getCondition()); assertFalse(new FieldGrowthTimer().estimate(observed,now).isPresent());
        }
    }
    @Test public void persistenceKeepsSpecialFamilyAndUnderwaterChannelsAcrossRestart()
    {
        Map<String,String> records=new HashMap<>();
        FieldObservationPersistence persistence=new FieldObservationPersistence(new ObservationPersistenceBackend()
        {
            public String read(String profile,String key) { return records.get(profile+key); }
            public void write(String profile,String key,String value) { records.put(profile+key,value); }
        });
        FieldPatchObservationStore store=new FieldPatchObservationStore(); persistence.loadProfile("A",store,now);
        FieldPatchDefinition[] patches={FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH,FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_SOUTH,FieldPatchDefinition.MORYTANIA_MUSHROOM,FieldPatchDefinition.AUBURNVALE_BELLADONNA};
        for(FieldPatchDefinition patch:patches) { store.observe(patch,4,now,FarmingGrowthRate.LEAGUES); }
        persistence.save(store.snapshot()); persistence.detach(); persistence.loadProfile("A",store,now.plusSeconds(1));
        assertEquals(4,store.snapshot().size()); for(FieldPatchDefinition patch:patches) { assertEquals(patch.getFamily(),store.snapshot().get(patch).getState().getCrop().getFamily()); }
        persistence.loadProfile("B",store,now.plusSeconds(1)); assertTrue(store.isEmpty());
    }
    @Test public void specialAccessNeverInfersAuburnvaleAndRequiresKnownQuests()
    {
        FieldPatchAccess access=new FieldPatchAccess(); AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
        assertFalse(access.missingRequirement(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH,account).isEmpty());
        assertFalse(access.missingRequirement(FieldPatchDefinition.MORYTANIA_MUSHROOM,account).isEmpty());
        assertEquals("",access.missingRequirement(FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA,account));
        assertTrue(access.missingRequirement(FieldPatchDefinition.AUBURNVALE_BELLADONNA,account).contains("Data required"));
    }
}
