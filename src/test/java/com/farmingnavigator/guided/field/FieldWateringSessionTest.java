package com.farmingnavigator.guided.field;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldWateringSessionTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final FieldRunSession session=new FieldRunSession();
    private final FieldPatchDefinition north=FieldPatchDefinition.FALADOR_NORTH,south=FieldPatchDefinition.FALADOR_SOUTH;
    private FieldRunPlan start(boolean protect) { return start(protect, FieldRunCrop.POTATO); }
    private FieldRunPlan start(boolean protect, FieldRunCrop crop)
    {
        AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
        StorageSnapshot storage=new StorageSnapshot(Map.of(ItemID.WATERING_CAN_1,1L),Map.of(),Map.of(ItemID.WATERING_CAN_1,1L),true,now);
        RequirementContext context=new RequirementContext(account,new FarmingNavigatorConfig(){},null,storage);
        FieldRunPlan plan=new FieldRunPlanner().plan(account,Set.of(north,south),crop,FieldRunMode.HARVEST_ALL,protect,FieldRunCompost.NONE,HerbRunCure.PLANT_CURE,
                FieldRunWatering.AFTER_PLANTING,Map.of(),storage,now).withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        assertEquals(2,plan.getWateringApplications()); assertTrue(plan.getWateringBudget().getCheck().requiresBankWithdrawal());
        session.start(plan,protect,now); session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1)); return plan;
    }
    private void observe(FieldPatchDefinition patch,int raw,int second)
    {
        session.observe(patch.getLocation(),Map.of(patch,new FieldPatchObservation(new FieldPatchStateDecoder().decode(patch.getFamily(),raw),now.plusSeconds(second),FarmingGrowthRate.NORMAL)),now.plusSeconds(second));
    }
    @Test public void plantedCropNeedsManualWaterBeforeProtectionAndNextPlotResetsTheFlags()
    {
        FieldRunPlan plan=start(true); observe(north,0,2); observe(north,6,3); assertEquals(FieldRunStep.WATER,session.snapshot(now).getStep());
        assertTrue(session.snapshot(now).getInstruction().contains("only covers this growth cycle"));
        assertEquals(WateringCanBudget.getFilledCanIds(),GuidedRunTargets.fromField(session.snapshot(now),plan,null).getInventoryItems());
        observe(north,70,4); assertEquals(FieldRunStep.PROTECT,session.snapshot(now).getStep());
        assertFalse(session.command(FieldRunCommand.CONFIRM_WATERING,now.plusSeconds(5))); assertEquals(0,session.snapshot(now).getProtectionConfirmed());
        session.command(FieldRunCommand.CONFIRM,now.plusSeconds(6)); observe(south,0,7); observe(south,6,8); assertEquals(FieldRunStep.WATER,session.snapshot(now).getStep());
    }
    @Test public void unknownEvidenceDoesNotErasePlantingOrManualWaterRequirement()
    {
        start(false); observe(north,0,2); observe(north,6,3); observe(north,-1,4); assertEquals(FieldRunStep.INSPECT,session.snapshot(now).getStep());
        observe(north,6,5); assertEquals(FieldRunStep.WATER,session.snapshot(now).getStep());
        session.command(FieldRunCommand.CONFIRM,now.plusSeconds(6)); assertEquals(1,session.snapshot(now).getCompleted());
    }
    @Test public void emptyPlotAfterPlantingRequiresNewPlanInsteadOfASecondUnbudgetedSeed()
    {
        FieldRunPlan plan=start(false); observe(north,0,2); observe(north,6,3); observe(north,0,4);
        assertEquals(FieldRunStep.REPREPARE,session.snapshot(now).getStep()); assertTrue(session.snapshot(now).getInstruction().contains("No second seed"));
        assertTrue(GuidedRunTargets.fromField(session.snapshot(now),plan,null).getInventoryItems().isEmpty());
        assertNull(GuidedRunTargets.fromField(session.snapshot(now),plan,null).getFieldScenePatch());
        assertFalse(session.command(FieldRunCommand.CONFIRM,now.plusSeconds(5))); observe(north,0,6); assertEquals(FieldRunStep.REPREPARE,session.snapshot(now).getStep());
    }
    @Test public void existingGrowingCropsDoNotSkipDirectlyToPostPlantingWater()
    {
        start(false); observe(north,6,2); assertEquals(FieldRunStep.WAIT,session.snapshot(now).getStep());
        assertEquals(0,session.snapshot(now).getCompleted());
    }
    @Test public void keptFlowersDiseaseOnlyAndSpecialFamiliesReserveNoWaterApplications()
    {
        AccountState account=new AccountState(true,99,99,EnumSet.of(Quest.PRIEST_IN_PERIL),false);
        FieldRunCrop[] crops={FieldRunCrop.MARIGOLD,FieldRunCrop.POTATO,FieldRunCrop.MUSHROOM};
        FieldPatchDefinition[] patches={FieldPatchDefinition.FALADOR_FLOWER,north,FieldPatchDefinition.MORYTANIA_MUSHROOM}; int[] raw={12,135,0};
        for(int i=0;i<crops.length;i++)
        {
            FieldPatchDefinition patch=patches[i]; FieldPatchObservation observed=new FieldPatchObservation(new FieldPatchStateDecoder().decode(patch.getFamily(),raw[i]),now,FarmingGrowthRate.NORMAL);
            FieldRunPlan plan=new FieldRunPlanner().plan(account,Set.of(patch),crops[i],FieldRunMode.KEEP_MATURE_FLOWERS,true,FieldRunCompost.NONE,HerbRunCure.PLANT_CURE,FieldRunWatering.AFTER_PLANTING,Map.of(patch,observed),null,now);
            assertEquals(0,plan.getWateringApplications());
        }
    }
    @Test public void diseaseAfterPlantingKeepsThePendingWaterAndProtectionSteps()
    {
        start(true); observe(north,0,2); observe(north,6,3); observe(north,135,4); assertEquals(FieldRunStep.CURE,session.snapshot(now).getStep());
        observe(north,6,5); assertEquals(FieldRunStep.WATER,session.snapshot(now).getStep());
        session.command(FieldRunCommand.CONFIRM,now.plusSeconds(6)); assertEquals(FieldRunStep.PROTECT,session.snapshot(now).getStep());
        assertEquals(0,session.snapshot(now).getCompleted());
    }

    @Test public void staleWaterConfirmationCannotConfirmProtectionInstead()
    {
        start(true); observe(north,0,2); observe(north,6,3);
        assertTrue(session.command(FieldRunCommand.CONFIRM_WATERING,now.plusSeconds(4))); assertEquals(FieldRunStep.PROTECT,session.snapshot(now).getStep());
        assertFalse(session.command(FieldRunCommand.CONFIRM_WATERING,now.plusSeconds(5))); assertEquals(0,session.snapshot(now).getProtectionConfirmed());
    }

    @Test public void snapeGrassWateredAndDryRangesAreReversedAndCannotBeConfused()
    {
        start(true,FieldRunCrop.SNAPE_GRASS); observe(north,0,2); observe(north,128,3); assertEquals(FieldRunStep.WATER,session.snapshot(now).getStep());
        observe(north,63,4); assertEquals(FieldRunStep.PROTECT,session.snapshot(now).getStep());
    }
    @Test public void wateredPlantingStateCanProvePlantingAndWateringTogether()
    {
        start(false); observe(north,0,2); observe(north,70,3); assertEquals(1,session.snapshot(now).getCompleted());
    }
    @Test public void staleFutureAndDifferentCropWateredStatesCannotProveWatering()
    {
        start(true); observe(north,0,2); observe(north,6,3);
        FieldPatchObservation stale=new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT,70),now.plusSeconds(4),FarmingGrowthRate.NORMAL);
        assertFalse(session.observe(north.getLocation(),Map.of(north,stale),now.plusSeconds(35))); assertEquals(FieldRunStep.WATER,session.snapshot(now).getStep());
        FieldPatchObservation future=new FieldPatchObservation(stale.getState(),now.plusSeconds(40),FarmingGrowthRate.NORMAL);
        assertFalse(session.observe(north.getLocation(),Map.of(north,future),now.plusSeconds(36)));
        observe(north,77,37); assertEquals(FieldRunStep.WAIT,session.snapshot(now).getStep()); assertEquals(0,session.snapshot(now).getProtectionConfirmed());
    }

}
