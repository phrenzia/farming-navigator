package com.farmingnavigator.guided.field;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldSpellGuidanceTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final FieldPatchDefinition patch=FieldPatchDefinition.FALADOR_NORTH;
    private final FieldRunSession session=new FieldRunSession();
    private final TravelCapabilitySnapshot travel=new TravelCapabilitySnapshot(true,SpellbookType.LUNAR,null,Map.of(ItemID.ASTRALRUNE,10,ItemID.NATURERUNE,4),Map.of(),Map.of());
    private FieldRunPlan start(boolean sick)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(Quest.LUNAR_DIPLOMACY,QuestState.FINISHED),Map.of(),Map.of()).withRealAttackLevel(99);
        StorageSnapshot storage=new StorageSnapshot(Map.of(ItemID.ASTRALRUNE,10L,ItemID.NATURERUNE,4L),Map.of(),Map.of(ItemID.STAFF_OF_EARTH,1L),true,now);
        RequirementContext context=new RequirementContext(account,new FarmingNavigatorConfig(){ @Override public boolean fieldAshTomeRead() { return true; } },travel,storage);
        Map<FieldPatchDefinition,FieldPatchObservation> observed=sick?Map.of(patch,observation(135,0)):Map.of();
        FieldRunPlan plan=new FieldRunPlanner().plan(account,Set.of(patch),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,true,FieldRunCompost.FERTILE_SOIL_ULTRA,HerbRunCure.CURE_PLANT,observed,storage,now)
                .withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        session.start(plan,true,now); return plan;
    }
    private FieldPatchObservation observation(int raw,int second) { return new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT,raw),now.plusSeconds(second),FarmingGrowthRate.NORMAL); }
    private void observe(int raw,int second) { session.observe(patch.getLocation(),Map.of(patch,observation(raw,second)),now.plusSeconds(second)); }
    @Test public void fertileSoilHighlightsSelectedRuneSourceAndAshButRequiresManualConfirmation()
    {
        FieldRunPlan plan=start(false);
        GuidedRunTargets preparation=GuidedRunTargets.fromField(session.snapshot(now),plan,travel);
        assertTrue(preparation.getBankItems().contains(ItemID.STAFF_OF_EARTH)); assertTrue(preparation.getInventoryItems().contains(ItemID.ASTRALRUNE));
        session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1)); observe(0,2);
        FieldRunSnapshot snapshot=session.snapshot(now); assertEquals(FieldRunStep.COMPOST,snapshot.getStep()); assertTrue(snapshot.getInstruction().contains("Cast Fertile Soil"));
        GuidedRunTargets targets=GuidedRunTargets.fromField(snapshot,plan,travel);
        assertTrue(targets.getInventoryItems().containsAll(Set.of(ItemID.STAFF_OF_EARTH,ItemID.ASTRALRUNE,ItemID.NATURERUNE,ItemID.FOSSIL_VOLCANIC_ASH)));
        assertFalse(targets.getInventoryItems().contains(ItemID.BUCKET_ULTRACOMPOST));
        observe(6,3); assertEquals(FieldRunStep.COMPOST,session.snapshot(now).getStep());
        session.command(FieldRunCommand.CONFIRM,now.plusSeconds(4)); observe(6,5); assertEquals(FieldRunStep.PROTECT,session.snapshot(now).getStep());
    }
    @Test public void curePlantHighlightsRunesAndSourceAndRequiresSameCropHealthyEvidence()
    {
        FieldRunPlan plan=start(true); session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1)); observe(135,2);
        FieldRunSnapshot snapshot=session.snapshot(now); assertEquals(FieldRunStep.CURE,snapshot.getStep()); assertTrue(snapshot.getInstruction().contains("Lunar Cure Plant"));
        GuidedRunTargets targets=GuidedRunTargets.fromField(snapshot,plan,travel);
        assertTrue(targets.getInventoryItems().containsAll(Set.of(ItemID.STAFF_OF_EARTH,ItemID.ASTRALRUNE)));
        assertFalse(targets.getInventoryItems().contains(ItemID.PLANT_CURE)); assertFalse(targets.getInventoryItems().contains(ItemID.FOSSIL_VOLCANIC_ASH));
        assertFalse(session.command(FieldRunCommand.CONFIRM,now.plusSeconds(3)));
        observe(6,4); assertEquals(FieldRunStep.COMPLETE,session.snapshot(now).getStep());
    }
}
