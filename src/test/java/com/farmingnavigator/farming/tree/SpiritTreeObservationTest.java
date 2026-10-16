package com.farmingnavigator.farming.tree;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.*;
import com.farmingnavigator.guided.tree.TreePatchDestination;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.transport.TreePatchTransportService;
import com.farmingnavigator.overlay.TreeSceneTargetPolicy;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpiritTreeObservationTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private List<TreePatchDefinition> patches()
    { return List.of(TreePatchDefinition.PORT_SARIM_SPIRIT,TreePatchDefinition.HOSIDIUS_SPIRIT,TreePatchDefinition.ETCETERIA_SPIRIT,TreePatchDefinition.BRIMHAVEN_SPIRIT,TreePatchDefinition.FARMING_GUILD_SPIRIT); }
    @Test public void fiveAreaAnchorsMapToExactRegionsAndPortSarimRejectsTheFaladorFarmSide()
    {
        for(TreePatchDefinition patch:patches())
        {
            WorldPoint point=TreePatchDestination.valueOf(patch.name()).getLocation();
            assertTrue(TreePatchRegion.forLocation(point).getPatches().contains(patch));
            assertNull(TreePatchRegion.forLocation(new WorldPoint(point.getX(),point.getY(),1)));
        }
        WorldPoint etceteria=TreePatchDestination.ETCETERIA_SPIRIT.getLocation(); assertEquals(2614,etceteria.getX()); assertEquals(10300,etceteria.getRegionID());
        assertEquals(TreePatchRegion.PORT_SARIM,TreePatchRegion.forLocation(new WorldPoint(3052,3271,0)));
        assertNull(TreePatchRegion.forLocation(new WorldPoint(3052,3272,0)));
    }
    @Test public void brimhavenAndEtceteriaCannotConfuseSpiritBWithFruitOrBushA()
    {
        TreePatchObservationStore store=new TreePatchObservationStore(); TreePatchObservationSampler sampler=new TreePatchObservationSampler(store);
        for(int tick=1;tick<=3;tick++)
        {
            assertEquals(tick==3,sampler.sample(TreePatchDestination.BRIMHAVEN_SPIRIT.getLocation(),true,false,false,tick,
                    id -> { assertTrue(id==VarbitID.FARMING_TRANSMIT_A || id==VarbitID.FARMING_TRANSMIT_B); return id==VarbitID.FARMING_TRANSMIT_A?226:44; },now.plusSeconds(tick),FarmingGrowthRate.NORMAL));
        }
        assertEquals(TreeCrop.PALM,store.snapshot().get(TreePatchDefinition.BRIMHAVEN_FRUIT).getState().getCrop());
        assertEquals(TreePatchCondition.CHECK_HEALTH,store.snapshot().get(TreePatchDefinition.BRIMHAVEN_SPIRIT).getState().getCondition());
        for(int tick=4;tick<=6;tick++)
        {
            assertEquals(tick==6,sampler.sample(TreePatchDestination.ETCETERIA_SPIRIT.getLocation(),true,false,false,tick,
                    id -> { assertEquals(VarbitID.FARMING_TRANSMIT_B,id); return 20; },now.plusSeconds(tick),FarmingGrowthRate.NORMAL));
        }
        assertEquals(TreePatchCondition.HEALTH_CHECKED,store.snapshot().get(TreePatchDefinition.ETCETERIA_SPIRIT).getState().getCondition());
    }
    @Test public void matureTravelAndCheckHealthStayDistinctAndNeverGenerateAnotherGrowthWindow()
    {
        TreePatchStateDecoder decoder=new TreePatchStateDecoder(); TreeGrowthTimer timer=new TreeGrowthTimer();
        assertEquals(TreePatchCondition.HEALTH_CHECKED,decoder.decode(TreeFamily.SPIRIT_TREE,20).getCondition());
        assertEquals(TreePatchCondition.CHECK_HEALTH,decoder.decode(TreeFamily.SPIRIT_TREE,44).getCondition());
        for(int raw:new int[]{20,44,64,255}) { assertFalse(timer.estimate(new TreePatchObservation(decoder.decode(TreeFamily.SPIRIT_TREE,raw),now,FarmingGrowthRate.NORMAL),now).isPresent()); }
        for(FarmingGrowthRate rate:List.of(FarmingGrowthRate.NORMAL,FarmingGrowthRate.LEAGUES))
        {
            TreeGrowthWindow window=timer.estimate(new TreePatchObservation(decoder.decode(TreeFamily.SPIRIT_TREE,8),now,rate),now).get(); int divisor=rate==FarmingGrowthRate.LEAGUES?5:1;
            assertEquals(now.plusSeconds(3520L*60/divisor),window.getEarliest()); assertEquals(now.plusSeconds(3840L*60/divisor),window.getLatest());
        }
    }
    @Test public void allFivePatchRecordsSurviveRestartAndRemainProfileSeparated()
    {
        Map<String,String> records=new HashMap<>(); TreeObservationPersistence persistence=new TreeObservationPersistence(new ObservationPersistenceBackend() {
            public String read(String profile,String key) { return records.get(profile+key); }
            public void write(String profile,String key,String value) { records.put(profile+key,value); }
        });
        TreePatchObservationStore store=new TreePatchObservationStore(); persistence.loadProfile("A",store,now);
        for(TreePatchDefinition patch:patches()) { store.observe(patch,20,now,FarmingGrowthRate.NORMAL); }
        persistence.save(store.snapshot()); persistence.detach(); persistence.loadProfile("A",store,now.plusSeconds(1)); assertEquals(5,store.snapshot().size());
        for(TreePatchDefinition patch:patches()) { assertEquals(TreeCrop.SPIRIT_TREE,store.snapshot().get(patch).getState().getCrop()); }
        persistence.loadProfile("B",store,now.plusSeconds(1)); assertTrue(store.isEmpty());
    }
    @Test public void matureHistoryDoesNotEnablePlantingProtectionTravelOrSceneGuidance()
    {
        AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
        for(TreeRunCrop crop:TreeRunCrop.values()) { assertNotEquals(TreeFamily.SPIRIT_TREE,crop.getCrop().getFamily()); }
        Map<?,?> routes=new TreePatchTransportService().getAllOptions();
        for(TreePatchDefinition patch:patches()) { assertTrue(new TreePatchAccess().missingRequirement(patch,account).contains("Data required")); assertFalse(routes.containsKey(patch)); }
        assertFalse(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.BRIMHAVEN_SPIRIT,NpcID.GARTH,TreePatchDestination.BRIMHAVEN_SPIRIT.getLocation()));
        ReadinessDashboard overview=ReadinessDashboard.at(now).trees(account,EnumSet.copyOf(patches()),Map.of()).build();
        assertEquals(5,overview.getExcludedCount()); assertTrue(overview.getEntries().isEmpty());
    }
}
