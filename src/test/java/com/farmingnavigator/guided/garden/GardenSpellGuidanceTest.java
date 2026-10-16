package com.farmingnavigator.guided.garden;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.*;
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

public class GardenSpellGuidanceTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final GardenPatchDefinition patch=GardenPatchDefinition.RIMMINGTON_BUSH;
    private final GardenRunSession session=new GardenRunSession();
    private final TravelCapabilitySnapshot travel=new TravelCapabilitySnapshot(true,SpellbookType.LUNAR,null,Map.of(ItemID.ASTRALRUNE,10,ItemID.NATURERUNE,4),Map.of(),Map.of());
    private GardenRunPlan start(boolean sick)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(Quest.LUNAR_DIPLOMACY,QuestState.FINISHED),Map.of(),Map.of()).withRealAttackLevel(99);
        StorageSnapshot storage=new StorageSnapshot(Map.of(ItemID.ASTRALRUNE,10L,ItemID.NATURERUNE,4L),Map.of(),Map.of(ItemID.STAFF_OF_EARTH,1L),true,now);
        RequirementContext context=new RequirementContext(account,new FarmingNavigatorConfig(){ @Override public boolean gardenAshTomeRead() { return true; } },travel,storage);
        Map<GardenPatchDefinition,GardenPatchObservation> observed=sick?Map.of(patch,observation(70,0)):Map.of();
        GardenRunPlan plan=new GardenRunPlanner().plan(account,Set.of(patch),GardenRunCrop.REDBERRIES,GardenRunMode.REPLACE_MATURE,true,GardenRunCompost.FERTILE_SOIL_ULTRA,HerbRunCure.CURE_PLANT,observed,storage,now)
                .withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        session.start(plan,true,now); return plan;
    }
    private GardenPatchObservation observation(int raw,int second) { return new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH,raw),now.plusSeconds(second),FarmingGrowthRate.NORMAL); }
    private void observe(int raw,int second) { session.observe(patch.getLocation(),Map.of(patch,observation(raw,second)),now.plusSeconds(second)); }
    @Test public void fertileSoilHighlightsSelectedRuneSourceAndAshButRequiresManualConfirmation()
    {
        GardenRunPlan plan=start(false);
        GuidedRunTargets preparation=GuidedRunTargets.fromGarden(session.snapshot(now),plan,travel);
        assertTrue(preparation.getBankItems().contains(ItemID.STAFF_OF_EARTH)); assertTrue(preparation.getInventoryItems().contains(ItemID.ASTRALRUNE));
        session.command(GardenRunCommand.CONFIRM,now.plusSeconds(1)); observe(0,2);
        GardenRunSnapshot snapshot=session.snapshot(now); assertEquals(GardenRunStep.COMPOST,snapshot.getStep()); assertTrue(snapshot.getInstruction().contains("Cast Fertile Soil"));
        GuidedRunTargets targets=GuidedRunTargets.fromGarden(snapshot,plan,travel);
        assertTrue(targets.getInventoryItems().containsAll(Set.of(ItemID.STAFF_OF_EARTH,ItemID.ASTRALRUNE,ItemID.NATURERUNE,ItemID.FOSSIL_VOLCANIC_ASH)));
        assertFalse(targets.getInventoryItems().contains(ItemID.BUCKET_ULTRACOMPOST));
        observe(5,3); assertEquals(GardenRunStep.COMPOST,session.snapshot(now).getStep());
        session.command(GardenRunCommand.CONFIRM,now.plusSeconds(4)); observe(5,5); assertEquals(GardenRunStep.PROTECT,session.snapshot(now).getStep());
    }
    @Test public void curePlantHighlightsRunesAndSourceAndRequiresSameCropHealthyEvidence()
    {
        GardenRunPlan plan=start(true); session.command(GardenRunCommand.CONFIRM,now.plusSeconds(1)); observe(70,2);
        GardenRunSnapshot snapshot=session.snapshot(now); assertEquals(GardenRunStep.CURE,snapshot.getStep()); assertTrue(snapshot.getInstruction().contains("Lunar Cure Plant"));
        GuidedRunTargets targets=GuidedRunTargets.fromGarden(snapshot,plan,travel);
        assertTrue(targets.getInventoryItems().containsAll(Set.of(ItemID.STAFF_OF_EARTH,ItemID.ASTRALRUNE)));
        assertFalse(targets.getInventoryItems().contains(ItemID.PLANT_CURE)); assertFalse(targets.getInventoryItems().contains(ItemID.FOSSIL_VOLCANIC_ASH));
        assertFalse(session.command(GardenRunCommand.CONFIRM,now.plusSeconds(3)));
        observe(5,4); assertEquals(GardenRunStep.COMPLETE,session.snapshot(now).getStep());
    }
    @Test public void cactusSpellCureDoesNotAskForPotionOrReplacementSeed()
    {
        GardenPatchDefinition cactus=GardenPatchDefinition.AL_KHARID_CACTUS;
        GardenPatchObservation diseased=new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.CACTUS,19),now,FarmingGrowthRate.NORMAL);
        GardenRunPlan plan=new GardenRunPlanner().plan(new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false),Set.of(cactus),GardenRunCrop.CACTUS,
                GardenRunMode.KEEP_MATURE,true,GardenRunCompost.NONE,HerbRunCure.CURE_PLANT,Map.of(cactus,diseased),null,now);
        assertTrue(plan.getSupplies().stream().noneMatch(c -> c.getRequirement().getItemId()==ItemID.PLANT_CURE || c.getRequirement().getItemId()==ItemID.CACTUS_SEED));
        session.start(plan,true,now); session.command(GardenRunCommand.CONFIRM,now.plusSeconds(1));
        session.observe(cactus.getLocation(),Map.of(cactus,new GardenPatchObservation(diseased.getState(),now.plusSeconds(2),FarmingGrowthRate.NORMAL)),now.plusSeconds(2));
        assertEquals(GardenRunStep.CURE,session.snapshot(now).getStep()); assertTrue(session.snapshot(now).getInstruction().contains("Cure Plant"));
        session.observe(cactus.getLocation(),Map.of(cactus,new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.CACTUS,8),now.plusSeconds(3),FarmingGrowthRate.NORMAL)),now.plusSeconds(3));
        assertEquals(GardenRunStep.COMPLETE,session.snapshot(now).getStep());
    }

}
