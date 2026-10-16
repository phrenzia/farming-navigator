package com.farmingnavigator.farming.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.overlay.FieldSceneTargetPolicy;
import com.farmingnavigator.transport.FieldPatchTransportService;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnimaObservationTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final FieldPatchDefinition patch=FieldPatchDefinition.FARMING_GUILD_ANIMA;
    private FieldPatchObservation observation(int raw,Instant at)
    { return new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ANIMA,raw),at,FarmingGrowthRate.NORMAL); }
    @Test public void guildAnimaUsesItsOwnTransmitWithoutReplacingAllotmentOrFlowerEvidence()
    {
        FieldPatchObservationStore store=new FieldPatchObservationStore(); FieldPatchObservationSampler sampler=new FieldPatchObservationSampler(store);
        for(int tick=1;tick<=3;tick++)
        {
            assertEquals(tick==3,sampler.sample(patch.getLocation(),true,false,false,tick,
                    id -> id==VarbitID.FARMING_TRANSMIT_M?15:id==VarbitID.FARMING_TRANSMIT_H?41:10,now.plusSeconds(tick),FarmingGrowthRate.NORMAL));
        }
        assertEquals(4,store.snapshot().size()); assertEquals(FieldPatchCondition.WITHERING_ANIMA,store.snapshot().get(patch).getState().getCondition());
        assertEquals(FieldCrop.WHITE_LILY,store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_FLOWER).getState().getCrop());
        assertEquals(FieldCrop.POTATO,store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_NORTH).getState().getCrop());
    }
    @Test public void allAnimaLifecycleStagesAvoidHarvestEstimatesEvenAfterArbitraryElapsedTime()
    {
        for(int raw=8;raw<=34;raw++)
        {
            FieldPatchObservation observed=observation(raw,now); FieldPatchState state=observed.getState(); int stage=(raw-8)%9;
            assertEquals(stage==8?FieldPatchCondition.DEAD:stage==7?FieldPatchCondition.WITHERING_ANIMA:FieldPatchCondition.LIVING_ANIMA,state.getCondition());
            assertNotEquals(FieldPatchCondition.DISEASED,state.getCondition()); assertFalse(state.isWatered());
            assertFalse(new FieldGrowthTimer().estimate(observed,now.plusSeconds(365L*86400)).isPresent());
        }
    }
    @Test public void livingAndWitheringNeverTriggerHarvestOrDiseaseNotifications()
    {
        FieldAlertTracker tracker=new FieldAlertTracker(); tracker.poll(Map.of(),Set.of(patch),now,true,true);
        assertTrue(tracker.poll(Map.of(patch,observation(8,now)),Set.of(patch),now,true,true).isEmpty());
        assertTrue(tracker.poll(Map.of(patch,observation(15,now.plusSeconds(1))),Set.of(patch),now.plusSeconds(1),true,true).isEmpty());
        assertTrue(tracker.poll(Map.of(patch,observation(15,now.plusSeconds(1))),Set.of(patch),now.plusSeconds(86400L*10),true,true).isEmpty());
    }
    @Test public void witheringAndDeadFormsPersistAndProfilesStaySeparate()
    {
        Map<String,String> records=new HashMap<>(); FieldObservationPersistence persistence=new FieldObservationPersistence(new ObservationPersistenceBackend() {
            public String read(String profile,String key) { return records.get(profile+key); }
            public void write(String profile,String key,String value) { records.put(profile+key,value); }
        });
        FieldPatchObservationStore store=new FieldPatchObservationStore(); persistence.loadProfile("A",store,now); store.observe(patch,33,now,FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); persistence.detach(); persistence.loadProfile("A",store,now.plusSeconds(1));
        assertEquals(FieldCrop.KRONOS,store.snapshot().get(patch).getState().getCrop()); assertEquals(FieldPatchCondition.WITHERING_ANIMA,store.snapshot().get(patch).getState().getCondition());
        store.observe(patch,34,now.plusSeconds(2),FarmingGrowthRate.NORMAL); persistence.save(store.snapshot()); persistence.detach(); persistence.loadProfile("A",store,now.plusSeconds(3));
        assertEquals(FieldPatchCondition.DEAD,store.snapshot().get(patch).getState().getCondition());
        persistence.loadProfile("B",store,now.plusSeconds(3)); assertTrue(store.isEmpty());
    }
    @Test public void unknownPreparationAndGuildTierDoNotBecomeBeginnerAreaGuidance()
    {
        AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
        assertTrue(new FieldPatchAccess().missingRequirement(patch,account).contains("Data required"));
        assertFalse(new FieldPatchTransportService().getAllOptions().containsKey(patch));
        assertFalse(FieldSceneTargetPolicy.isFarmerFor(patch,NpcID.FARMING_GARDENER_FARMGUILD_T1,patch.getLocation()));
        for(FieldRunCrop crop:FieldRunCrop.values()) { assertNotEquals(FieldFamily.ANIMA,crop.getCrop().getFamily()); }
        ReadinessDashboard overview=ReadinessDashboard.at(now).fields(account,Set.of(patch),Map.of(patch,observation(8,now))).build();
        assertEquals(1,overview.getExcludedCount()); assertTrue(overview.getEntries().isEmpty());
    }
}
