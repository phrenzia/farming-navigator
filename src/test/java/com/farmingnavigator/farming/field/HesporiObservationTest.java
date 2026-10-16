package com.farmingnavigator.farming.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.overlay.FieldSceneTargetPolicy;
import com.farmingnavigator.transport.FieldPatchTransportService;
import com.farmingnavigator.ui.FieldObservationText;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import static org.junit.Assert.*;

public class HesporiObservationTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final FieldPatchDefinition patch=FieldPatchDefinition.FARMING_GUILD_HESPORI;
    private FieldPatchObservation observation(int raw,Instant at,FarmingGrowthRate rate)
    { return new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.HESPORI,raw),at,rate); }
    @Test public void caveSamplesOnlyItsOwnChannelAfterThreeSettledTicks()
    {
        FieldPatchObservationStore store=new FieldPatchObservationStore(); FieldPatchObservationSampler sampler=new FieldPatchObservationSampler(store);
        assertEquals(5021,patch.getLocation().getRegionID());
        for(int tick=1;tick<=3;tick++)
        {
            assertEquals(tick==3,sampler.sample(patch.getLocation(),true,false,false,tick,
                    id -> { assertEquals(VarbitID.FARMING_TRANSMIT_J,id); return 7; },now.plusSeconds(tick),FarmingGrowthRate.NORMAL));
        }
        assertEquals(Set.of(patch),store.snapshot().keySet()); assertEquals(FieldPatchCondition.MATURE_HESPORI,store.snapshot().get(patch).getState().getCondition());
        assertFalse(sampler.sample(patch.getLocation(),true,false,true,4,id -> { fail("No instanced sampling"); return 8; },now.plusSeconds(4),FarmingGrowthRate.NORMAL));
        assertFalse(sampler.sample(patch.getLocation(),true,true,false,5,id -> { fail("No modal sampling"); return 8; },now.plusSeconds(5),FarmingGrowthRate.NORMAL));
        assertFalse(sampler.sample(new WorldPoint(1182,10068,0),true,false,false,6,id -> { fail("Map anchor outside transmit region"); return 8; },now.plusSeconds(6),FarmingGrowthRate.NORMAL));
        assertEquals(7,store.snapshot().get(patch).getState().getRawValue());
        for(int tick=7;tick<=9;tick++) { assertEquals(tick==9,sampler.sample(patch.getLocation(),true,false,false,tick,id -> 8,now.plusSeconds(tick),FarmingGrowthRate.NORMAL)); }
        assertEquals(8,store.snapshot().get(patch).getState().getRawValue());
    }
    @Test public void growingWindowsRemainConditionalAndMatureBytesNeverPredictRewards()
    {
        for(FarmingGrowthRate rate:List.of(FarmingGrowthRate.NORMAL,FarmingGrowthRate.LEAGUES))
        {
            long cycle=640L*60/(rate==FarmingGrowthRate.LEAGUES?5:1);
            for(int raw=4;raw<=6;raw++)
            {
                FieldPatchObservation observed=observation(raw,now,rate); FieldGrowthWindow window=new FieldGrowthTimer().estimate(observed,now).get();
                assertEquals(now.plusSeconds(cycle*(6-raw)),window.getEarliest()); assertEquals(now.plusSeconds(cycle*(7-raw)),window.getLatest());
                String text=FieldObservationText.timer(observed,now); assertFalse(text.contains("harvest")); assertTrue(text.contains("confirm"));
                assertFalse(FieldObservationText.timer(observed,now.plusSeconds(1000000)).contains("harvest"));
                assertEquals(FieldPatchCondition.GROWING,observed.getState().getCondition());
            }
        }
        for(int raw:new int[]{0,3,7,8,9,10,255})
        { assertFalse(new FieldGrowthTimer().estimate(observation(raw,now,FarmingGrowthRate.NORMAL),now).isPresent()); }
        for(int raw:new int[]{7,8})
        {
            FieldPatchObservation observed=observation(raw,now,FarmingGrowthRate.NORMAL); assertFalse(observed.getState().isWatered());
            assertTrue(FieldObservationText.describe(observed,ZoneId.of("UTC")).contains("Mature Hespori patch; inspect manually"));
            assertNotEquals(FieldPatchCondition.HARVESTABLE,observed.getState().getCondition());
        }
        assertFalse(new FieldGrowthTimer().estimate(observation(4,now,FarmingGrowthRate.UNKNOWN),now).isPresent());
        assertFalse(new FieldGrowthTimer().estimate(observation(4,now.plusSeconds(1),FarmingGrowthRate.NORMAL),now).isPresent());
        assertEquals(FieldPatchCondition.UNKNOWN,observation(10,now,FarmingGrowthRate.NORMAL).getState().getCondition());
    }
    @Test public void noOrdinaryCropAlertsPreparationTravelOrFarmerTargetsAreCreated()
    {
        FieldAlertTracker tracker=new FieldAlertTracker(); tracker.poll(Map.of(),Set.of(patch),now,true,true);
        assertTrue(tracker.poll(Map.of(patch,observation(4,now,FarmingGrowthRate.NORMAL)),Set.of(patch),now.plusSeconds(1000000),true,true).isEmpty());
        assertTrue(tracker.poll(Map.of(patch,observation(7,now.plusSeconds(1),FarmingGrowthRate.NORMAL)),Set.of(patch),now.plusSeconds(1),true,true).isEmpty());
        AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
        assertTrue(new FieldPatchAccess().missingRequirement(patch,account).contains("Data required"));
        assertFalse(new FieldPatchTransportService().getAllOptions().containsKey(patch));
        assertFalse(FieldSceneTargetPolicy.isFarmerFor(patch,NpcID.FARMING_GARDENER_FARMGUILD_T1,patch.getLocation()));
        for(FieldRunCrop crop:FieldRunCrop.values()) { assertNotEquals(FieldFamily.HESPORI,crop.getCrop().getFamily()); }
        ReadinessDashboard overview=ReadinessDashboard.at(now).fields(account,Set.of(patch),Map.of(patch,observation(7,now,FarmingGrowthRate.NORMAL))).build();
        assertEquals(1,overview.getExcludedCount()); assertTrue(overview.getEntries().isEmpty());
    }
    @Test public void matureRawCodesPersistWithoutInferringAFightOrRewardAcrossProfiles()
    {
        Map<String,String> records=new HashMap<>(); FieldObservationPersistence persistence=new FieldObservationPersistence(new ObservationPersistenceBackend() {
            public String read(String profile,String key) { return records.get(profile+key); }
            public void write(String profile,String key,String value) { records.put(profile+key,value); }
        });
        FieldPatchObservationStore store=new FieldPatchObservationStore(); persistence.loadProfile("A",store,now);
        for(int raw:new int[]{7,8,9,10})
        {
            store.observe(patch,raw,now,FarmingGrowthRate.NORMAL); persistence.save(store.snapshot()); persistence.detach(); persistence.loadProfile("A",store,now.plusSeconds(1));
            assertEquals(raw,store.snapshot().get(patch).getState().getRawValue());
            assertEquals(new FieldPatchStateDecoder().decode(FieldFamily.HESPORI,raw).getCondition(),store.snapshot().get(patch).getState().getCondition());
        }
        persistence.loadProfile("B",store,now.plusSeconds(2)); assertTrue(store.isEmpty());
    }
}
