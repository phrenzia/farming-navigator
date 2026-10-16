package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.guided.*;
import com.farmingnavigator.guided.tree.*;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.guided.field.*;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

@org.junit.runner.RunWith(org.junit.runners.Parameterized.class)
public class SharedTransportPlanTest
{
    @org.junit.runners.Parameterized.Parameters(name="{0}") public static Object[] modes()
    { return new Object[]{HerbTransportPreference.SHARED_BUDGET,HerbTransportPreference.SHARED_BUDGET_PAIR}; }
    private final HerbTransportPreference mode;
    public SharedTransportPlanTest(HerbTransportPreference mode) { this.mode=mode; }
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of(),false);
    private final VerifiedTransportResult spell=new VerifiedTransportResult(new TransportOption(1,"Synthetic spell arrival","Cast manually",TransportCategory.SPELL,TransportVerificationKind.TELEPORT_SPELL,TeleportSpellDefinition.FALADOR_TELEPORT),VerifiedTransportState.READY_NOW,"");
    private final VerifiedTransportResult reusable=new VerifiedTransportResult(new TransportOption(2,"Synthetic reusable arrival","Use manually",TransportCategory.ITEM,TransportVerificationKind.ECTOPHIAL),VerifiedTransportState.READY_NOW,"");
    private final List<VerifiedTransportResult> alternatives=List.of(spell,reusable);
    private RequirementContext context(int laws)
    {
        Map<Integer,Integer> inv=Map.of(ItemID.AIRRUNE,6,ItemID.WATERRUNE,2,ItemID.LAWRUNE,laws);
        Map<Integer,Long> stock=new HashMap<>(); inv.forEach((id,count)->stock.put(id,count.longValue()));
        return new RequirementContext(account,null,new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,inv,Map.of(),Map.of()),new StorageSnapshot(stock,Map.of(),Map.of(),true,now));
    }
    private void covered(List<? extends RunTravelStop> stops,RunTransportResources resources)
    {
        assertSame(reusable,stops.get(0).getRecommended());
        assertTrue(resources.getChecks().stream().allMatch(RequirementResult::isSatisfied));
        assertTrue(resources.getNotes().stream().anyMatch(note -> note.contains("Alternate arrival selected")));
    }
    @Test public void herbPlanAndActiveRefreshPreserveRepairedChoiceWithoutChangingRevision()
    {
        Map<HerbPatchDefinition,List<VerifiedTransportResult>> routes=Map.of(HerbPatchDefinition.FALADOR,alternatives,HerbPatchDefinition.CATHERBY,alternatives);
        HerbRunPlan original=new HerbRunPlanner().plan(account,List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of()),new HerbPatchStatus(HerbPatchDefinition.CATHERBY,List.of())),routes.keySet(),HerbRunCrop.RANARR,HerbRunCompost.NONE,Map.of(),routes,null,now);
        HerbRunPlan plan=original.withTransports(routes,mode,context(1)); covered(plan.getStops(),plan.getTransportResources());
        assertEquals(original.getSupplies(),plan.getSupplies()); assertSame(spell,original.getStops().get(0).getRecommended());
        GuidedRunSession session=new GuidedRunSession(); assertTrue(session.start(plan,HerbRunCompost.NONE,now)); long revision=session.getRevision();
        session.refreshTransports(routes,mode); assertSame(reusable,session.snapshot(now).getStop().getRecommended()); assertEquals(revision,session.getRevision());
    }
    @Test public void treePlanAndActiveRefreshPreserveRepairedChoice()
    {
        Map<TreePatchDefinition,List<VerifiedTransportResult>> routes=Map.of(TreePatchDefinition.VARROCK_TREE,alternatives,TreePatchDefinition.FALADOR_TREE,alternatives);
        TreeRunPlan original=new TreeRunPlanner().plan(account,routes.keySet(),TreeRunCrop.OAK,false,Map.of(),null,now);
        TreeRunPlan plan=original.withTransports(routes,mode,context(1)); covered(plan.getStops(),plan.getTransportResources());
        assertEquals(original.getSupplies(),plan.getSupplies());
        TreeRunSession session=new TreeRunSession(); assertTrue(session.start(plan,false,now)); long revision=session.getRevision();
        session.refreshTransports(routes,mode,context(0)); assertSame(reusable,session.snapshot(now).getStop().getRecommended()); assertEquals(revision,session.getRevision());
    }
    @Test public void gardenPlanAndActiveRefreshPreserveRepairedChoice()
    {
        Map<GardenPatchDefinition,List<VerifiedTransportResult>> routes=Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,alternatives,GardenPatchDefinition.CHAMPIONS_GUILD_BUSH,alternatives);
        GardenRunPlan original=new GardenRunPlanner().plan(account,routes.keySet(),GardenRunCrop.REDBERRIES,GardenRunMode.KEEP_MATURE,false,Map.of(),null,now);
        GardenRunPlan plan=original.withTransports(routes,mode,context(1)); covered(plan.getStops(),plan.getTransportResources());
        assertEquals(original.getSupplies(),plan.getSupplies());
        GardenRunSession session=new GardenRunSession(); assertTrue(session.start(plan,false,now)); long revision=session.getRevision();
        session.refreshTransports(routes,mode,context(0)); assertSame(reusable,session.snapshot(now).getStop().getRecommended()); assertEquals(revision,session.getRevision());
    }
    @Test public void adjacentFieldPlotStaysLocalAndConsumesNoAdditionalArrival()
    {
        Map<FieldPatchDefinition,List<VerifiedTransportResult>> routes=Map.of(FieldPatchDefinition.FALADOR_NORTH,alternatives,FieldPatchDefinition.FALADOR_SOUTH,alternatives);
        FieldRunPlan original=new FieldRunPlanner().plan(account,routes.keySet(),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,false,Map.of(),null,now);
        FieldRunPlan plan=original.withTransports(routes,mode,context(0)); covered(plan.getStops(),plan.getTransportResources());
        assertTrue(plan.getStops().get(1).isLocalWalk()); assertNull(plan.getStops().get(1).getRecommended());
        FieldRunSession session=new FieldRunSession(); assertTrue(session.start(plan,false,now)); long revision=session.getRevision();
        session.refreshTransports(routes,mode,context(0)); assertSame(reusable,session.snapshot(now).getStop().getRecommended()); assertEquals(revision,session.getRevision());
        session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1)); session.command(FieldRunCommand.SKIP,now.plusSeconds(2));
        assertTrue(session.snapshot(now).getStop().isLocalWalk());
    }
    @Test public void combinedWorksheetRetainsStrictPoolVerificationAcrossAllComponents()
    {
        RequirementContext original=context(0);
        RequirementContext context=new RequirementContext(account,new com.farmingnavigator.FarmingNavigatorConfig(){},original.getTravelCapabilitySnapshot(),original.getStorageSnapshot());
        GardenRunPlan garden=new GardenRunPlanner().plan(account,Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),GardenRunCrop.REDBERRIES,GardenRunMode.KEEP_MATURE,false,Map.of(),null,now)
                .withTransports(Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,List.of(reusable)),mode,context);
        VerifiedTransportResult unknownPool=new VerifiedTransportResult(new TransportOption(1,"Synthetic charged ring","Use manually",TransportCategory.ITEM,TransportVerificationKind.RING_OF_ELEMENTS),VerifiedTransportState.READY_NOW,"");
        FieldRunPlan field=new FieldRunPlanner().plan(account,Set.of(FieldPatchDefinition.FALADOR_NORTH),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,false,Map.of(),null,now)
                .withTransports(Map.of(FieldPatchDefinition.FALADOR_NORTH,List.of(unknownPool)),HerbTransportPreference.CATALOGUE,context);
        assertTrue(garden.getTransportResources().isKnownPoolsRequired());
        assertFalse(field.getTransportResources().isKnownPoolsRequired());
        CombinedRunPlan combined=new CombinedRunPlanner().plan(EnumSet.of(CombinedRunKind.GARDEN,CombinedRunKind.FIELD),null,null,garden,field,context);
        assertTrue(combined.getResources().isKnownPoolsRequired());
        assertTrue(combined.getResources().getChecks().stream().anyMatch(check -> check.getState()==RequirementState.DATA_REQUIRED));
    }
    @Test public void allFourRootPlansOnlyPublishTheTwoChangesWhenPairModeIsExplicitlySelected()
    {
        Map<HerbPatchDefinition,List<VerifiedTransportResult>> herbs=Map.of(HerbPatchDefinition.FALADOR,alternatives,HerbPatchDefinition.CATHERBY,alternatives);
        HerbRunPlan herb=new HerbRunPlanner().plan(account,List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of()),new HerbPatchStatus(HerbPatchDefinition.CATHERBY,List.of())),herbs.keySet(),HerbRunCrop.RANARR,HerbRunCompost.NONE,Map.of(),herbs,null,now).withTransports(herbs,mode,context(0));
        pairOutcome(herb.getStops(),herb.getTransportResources(),0);
        Map<TreePatchDefinition,List<VerifiedTransportResult>> trees=Map.of(TreePatchDefinition.VARROCK_TREE,alternatives,TreePatchDefinition.FALADOR_TREE,alternatives);
        TreeRunPlan tree=new TreeRunPlanner().plan(account,trees.keySet(),TreeRunCrop.OAK,false,Map.of(),null,now).withTransports(trees,mode,context(0));
        pairOutcome(tree.getStops(),tree.getTransportResources(),0);
        Map<GardenPatchDefinition,List<VerifiedTransportResult>> gardens=Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,alternatives,GardenPatchDefinition.CHAMPIONS_GUILD_BUSH,alternatives);
        GardenRunPlan garden=new GardenRunPlanner().plan(account,gardens.keySet(),GardenRunCrop.REDBERRIES,GardenRunMode.KEEP_MATURE,false,Map.of(),null,now).withTransports(gardens,mode,context(0));
        pairOutcome(garden.getStops(),garden.getTransportResources(),0);
        Map<FieldPatchDefinition,List<VerifiedTransportResult>> fields=Map.of(FieldPatchDefinition.FALADOR_NORTH,alternatives,FieldPatchDefinition.FALADOR_SOUTH,alternatives,FieldPatchDefinition.CATHERBY_NORTH,alternatives);
        FieldRunPlan field=new FieldRunPlanner().plan(account,fields.keySet(),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,false,Map.of(),null,now).withTransports(fields,mode,context(0));
        pairOutcome(field.getStops(),field.getTransportResources(),1);
    }
    private void pairOutcome(List<? extends RunTravelStop> stops,RunTransportResources resources,int localCount)
    {
        assertTrue(resources.isKnownPoolsRequired()); assertEquals(localCount,stops.stream().filter(RunTravelStop::isLocalWalk).count());
        boolean paired=mode==HerbTransportPreference.SHARED_BUDGET_PAIR;
        assertTrue(stops.stream().filter(stop -> !stop.isLocalWalk()).allMatch(stop -> stop.getRecommended()==(paired?reusable:spell)));
        assertEquals(paired,resources.getChecks().stream().allMatch(RequirementResult::isSatisfied));
    }

}
