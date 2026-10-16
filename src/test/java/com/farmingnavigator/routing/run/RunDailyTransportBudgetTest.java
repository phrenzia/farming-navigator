package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunDailyTransportBudgetTest
{
    private final RunTransportResourcePlanner planner=new RunTransportResourcePlanner();
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RequirementContext context(Map<DailyTransportUseKind,Integer> remaining)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of(),false);
        return new RequirementContext(account,null,new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,Map.of(),Map.of(),Map.of(),Map.of(),false,remaining),new StorageSnapshot(Map.of(),Map.of(),Map.of(),true,now));
    }
    private VerifiedTransportResult route(TransportVerificationKind kind)
    { return new VerifiedTransportResult(new TransportOption(1,"Synthetic " + kind,"Travel manually",TransportCategory.DIARY_ITEM,kind),VerifiedTransportState.READY_NOW,""); }
    private List<HerbRunStop> stops(TransportVerificationKind kind)
    {
        VerifiedTransportResult daily=route(kind);
        return List.of(new HerbRunStop(HerbPatchDefinition.FALADOR,"Inspect",daily,null,true),new HerbRunStop(HerbPatchDefinition.CATHERBY,"Inspect",daily,null,true));
    }
    @Test public void bothLimitedKindsShareTheirVerifiedRemainingCounterAcrossRecommendedLegs()
    {
        Map<TransportVerificationKind,DailyTransportUseKind> kinds=Map.of(TransportVerificationKind.EXPLORER_RING_2_DAILY,DailyTransportUseKind.EXPLORER_RING_CABBAGE_PORT,
                TransportVerificationKind.KANDARIN_HEADGEAR_3_DAILY,DailyTransportUseKind.KANDARIN_SHERLOCK_TELEPORT);
        kinds.forEach((kind,pool) -> {
            for(int remaining=0;remaining<=3;remaining++)
            {
                RunTransportResources resources=planner.evaluate(stops(kind),context(Map.of(pool,remaining)));
                assertEquals(1,resources.getChecks().size()); assertEquals(remaining>=2,resources.getChecks().get(0).isReadyNow());
                assertTrue(resources.getChecks().get(0).getRequirement().contains("2 run teleports"));
            }
        });
    }
    @Test public void unknownOrNegativeDailyDataDoesNotBecomeAvailableFromVerifiedBank()
    {
        for(Map<DailyTransportUseKind,Integer> remaining:List.of(Map.<DailyTransportUseKind,Integer>of(),Map.of(DailyTransportUseKind.EXPLORER_RING_CABBAGE_PORT,-1)))
        {
            assertEquals(RequirementState.DATA_REQUIRED,planner.evaluate(stops(TransportVerificationKind.EXPLORER_RING_2_DAILY),context(remaining)).getChecks().get(0).getState());
        }
        assertEquals(RequirementState.DATA_REQUIRED,planner.evaluate(stops(TransportVerificationKind.EXPLORER_RING_2_DAILY),null).getChecks().get(0).getState());
    }
    @Test public void unlimitedRoutesAndFallbacksDoNotSpendLimitedUses()
    {
        VerifiedTransportResult daily=route(TransportVerificationKind.EXPLORER_RING_2_DAILY),unlimited=route(TransportVerificationKind.EXPLORER_RING_UNLIMITED);
        HerbRunStop stop=new HerbRunStop(HerbPatchDefinition.FALADOR,"Inspect",unlimited,daily,true);
        RunTransportResources resources=planner.evaluate(List.of(stop),context(Map.of(DailyTransportUseKind.EXPLORER_RING_CABBAGE_PORT,0)));
        assertTrue(resources.getChecks().isEmpty());
    }
    @Test public void adjacentLocalWalkDoesNotSpendAnotherDailyUse()
    {
        VerifiedTransportResult daily=route(TransportVerificationKind.EXPLORER_RING_2_DAILY);
        FieldRunStop first=new FieldRunStop(FieldPatchDefinition.FALADOR_NORTH,"Inspect",true).withTransports(daily,null);
        FieldRunStop next=new FieldRunStop(FieldPatchDefinition.FALADOR_SOUTH,"Inspect",true).withLocalWalk();
        RunTransportResources resources=planner.evaluate(List.of(first,next),context(Map.of(DailyTransportUseKind.EXPLORER_RING_CABBAGE_PORT,1)));
        assertTrue(resources.getChecks().get(0).isReadyNow()); assertTrue(resources.getChecks().get(0).getRequirement().contains("1 run teleport"));
    }
    @Test public void differentDailyPoolsAreIndependentAndUnusedPoolsDoNotBlock()
    {
        List<HerbRunStop> mixed=List.of(stops(TransportVerificationKind.EXPLORER_RING_2_DAILY).get(0),stops(TransportVerificationKind.KANDARIN_HEADGEAR_3_DAILY).get(1));
        RunTransportResources resources=planner.evaluate(mixed,context(Map.of(DailyTransportUseKind.EXPLORER_RING_CABBAGE_PORT,1,DailyTransportUseKind.KANDARIN_SHERLOCK_TELEPORT,1)));
        assertEquals(2,resources.getChecks().size()); assertTrue(resources.getChecks().stream().allMatch(RequirementResult::isReadyNow));
    }
    @Test public void repairCanReplaceOneArrivalInsteadOfReusingLastDailyTeleport()
    {
        VerifiedTransportResult daily=route(TransportVerificationKind.EXPLORER_RING_2_DAILY),unlimited=route(TransportVerificationKind.EXPLORER_RING_UNLIMITED);
        SharedTransportRepair.Result<HerbRunStop> result=new SharedTransportRepair().repair(stops(TransportVerificationKind.EXPLORER_RING_2_DAILY),
                stop -> List.of(daily,unlimited),(stop,chosen)->stop.withTransports(chosen,HerbTransportSelector.fallback(List.of(daily,unlimited),chosen)),
                context(Map.of(DailyTransportUseKind.EXPLORER_RING_CABBAGE_PORT,1)),RuneCost.builder().build());
        assertSame(unlimited,result.getStops().get(0).getRecommended()); assertTrue(result.getResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        assertTrue(result.getResources().getChecks().stream().noneMatch(check -> check.getState()==RequirementState.DATA_REQUIRED));
    }
    @Test public void individuallyCoveredHerbAndFieldRunsCannotReuseOneDailyUseInCombinedPreparation()
    {
        RequirementContext old=context(Map.of(DailyTransportUseKind.EXPLORER_RING_CABBAGE_PORT,1));
        RequirementContext context=new RequirementContext(old.getAccountState(),new com.farmingnavigator.FarmingNavigatorConfig(){},old.getTravelCapabilitySnapshot(),old.getStorageSnapshot());
        VerifiedTransportResult daily=route(TransportVerificationKind.EXPLORER_RING_2_DAILY);
        Map<HerbPatchDefinition,List<VerifiedTransportResult>> herbRoutes=Map.of(HerbPatchDefinition.FALADOR,List.of(daily));
        HerbRunPlan herb=new HerbRunPlanner().plan(context.getAccountState(),List.of(new com.farmingnavigator.farming.HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of())),herbRoutes.keySet(),HerbRunCrop.RANARR,HerbRunCompost.NONE,Map.of(),herbRoutes,context.getStorageSnapshot(),now);
        herb=herb.withCareResources(HerbRunCareResources.evaluate(herb,HerbRunCure.PLANT_CURE,HerbRunCompost.NONE,context))
                .withTransports(herbRoutes,HerbTransportPreference.SHARED_BUDGET,context);
        FieldRunPlan field=new FieldRunPlanner().plan(context.getAccountState(),Set.of(FieldPatchDefinition.FALADOR_NORTH),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,false,Map.of(),context.getStorageSnapshot(),now)
                .withTransports(Map.of(FieldPatchDefinition.FALADOR_NORTH,List.of(daily)),HerbTransportPreference.CATALOGUE,context);
        assertTrue(herb.getTransportResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        assertTrue(field.getTransportResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        CombinedRunPlan combined=new CombinedRunPlanner().plan(EnumSet.of(CombinedRunKind.HERB,CombinedRunKind.FIELD),herb,null,null,field,context);
        assertTrue(combined.getBlockers().isEmpty()); assertEquals(2,combined.getStops().size());
        assertEquals(1,combined.getResources().getChecks().size());
        assertEquals(RequirementState.BLOCKED,combined.getResources().getChecks().get(0).getState());
        assertTrue(combined.getResources().getChecks().get(0).getRequirement().contains("2 run teleports"));
    }
}
