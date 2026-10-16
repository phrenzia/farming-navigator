package com.farmingnavigator.guided;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.field.*;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.guided.tree.*;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.*;
import java.util.function.*;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChecklistCommandGuardTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
    private void exercise(LongSupplier revision,LongSupplier snapshotRevision,Runnable confirm,Runnable skip,Runnable restart,IntSupplier skipped)
    {
        long preparation=revision.getAsLong(); assertEquals(preparation,snapshotRevision.getAsLong());
        assertTrue(ChecklistCommandGuard.dispatch(preparation,revision.getAsLong(),confirm));
        long firstTravel=revision.getAsLong(); assertNotEquals(preparation,firstTravel); assertEquals(firstTravel,snapshotRevision.getAsLong());
        assertTrue(ChecklistCommandGuard.dispatch(firstTravel,revision.getAsLong(),skip));
        assertNotEquals(firstTravel,revision.getAsLong()); assertEquals(1,skipped.getAsInt());
        assertFalse(ChecklistCommandGuard.dispatch(firstTravel,revision.getAsLong(),skip)); assertEquals(1,skipped.getAsInt());
        restart.run(); assertEquals(0,skipped.getAsInt());
        assertFalse(ChecklistCommandGuard.dispatch(preparation,revision.getAsLong(),confirm));
        assertEquals(revision.getAsLong(),snapshotRevision.getAsLong());
        assertTrue(ChecklistCommandGuard.dispatch(revision.getAsLong(),revision.getAsLong(),confirm));
    }
    @Test public void herbRevisionsProtectNewStopsAndRestartedRuns()
    {
        GuidedRunSession session=new GuidedRunSession();
        HerbRunPlan plan=new HerbRunPlanner().plan(account,List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of()),new HerbPatchStatus(HerbPatchDefinition.CATHERBY,List.of())),
                Set.of(HerbPatchDefinition.FALADOR,HerbPatchDefinition.CATHERBY),HerbRunCrop.GUAM,HerbRunCompost.NONE,Map.of(),Map.of(),null,now,HerbTransportPreference.CATALOGUE);
        Runnable restart=() -> { session.cancel(); assertTrue(session.start(plan,HerbRunCompost.NONE,now)); }; restart.run();
        exercise(session::getRevision,() -> session.snapshot(now).getRevision(),() -> session.confirm(now.plusSeconds(1)),() -> session.skip(now.plusSeconds(2)),restart,() -> session.snapshot(now).getSkipped());
    }
    @Test public void treeRevisionsProtectNewStopsAndRestartedRuns()
    {
        TreeRunSession session=new TreeRunSession(); TreeRunPlan plan=new TreeRunPlanner().plan(account,Set.of(TreePatchDefinition.VARROCK_TREE,TreePatchDefinition.FALADOR_TREE),TreeRunCrop.OAK,true,Map.of(),null,now);
        Runnable restart=() -> { session.cancel(); assertTrue(session.start(plan,true,now)); }; restart.run();
        exercise(session::getRevision,() -> session.snapshot(now).getRevision(),() -> session.command(TreeRunCommand.CONFIRM,now.plusSeconds(1)),() -> session.command(TreeRunCommand.SKIP,now.plusSeconds(2)),restart,() -> session.snapshot(now).getSkipped());
    }
    @Test public void gardenRevisionsProtectNewStopsAndRestartedRuns()
    {
        GardenRunSession session=new GardenRunSession(); GardenRunPlan plan=new GardenRunPlanner().plan(account,Set.of(GardenPatchDefinition.RIMMINGTON_BUSH,GardenPatchDefinition.CHAMPIONS_GUILD_BUSH),GardenRunCrop.REDBERRIES,GardenRunMode.KEEP_MATURE,true,Map.of(),null,now);
        Runnable restart=() -> { session.cancel(); assertTrue(session.start(plan,true,now)); }; restart.run();
        exercise(session::getRevision,() -> session.snapshot(now).getRevision(),() -> session.command(GardenRunCommand.CONFIRM,now.plusSeconds(1)),() -> session.command(GardenRunCommand.SKIP,now.plusSeconds(2)),restart,() -> session.snapshot(now).getSkipped());
    }
    private FieldRunPlan fieldPlan()
    {
        return new FieldRunPlanner().plan(account,Set.of(FieldPatchDefinition.FALADOR_NORTH,FieldPatchDefinition.FALADOR_SOUTH),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,true,Map.of(),null,now);
    }
    @Test public void fieldRevisionsProtectNewStopsAndRestartedRuns()
    {
        FieldRunSession session=new FieldRunSession(); FieldRunPlan plan=fieldPlan();
        Runnable restart=() -> { session.cancel(); assertTrue(session.start(plan,true,now)); }; restart.run();
        exercise(session::getRevision,() -> session.snapshot(now).getRevision(),() -> session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1)),() -> session.command(FieldRunCommand.SKIP,now.plusSeconds(2)),restart,() -> session.snapshot(now).getSkipped());
    }
    @Test public void queuedSidebarClickCannotSkipTheStepChosenByANewerObservation()
    {
        FieldRunSession session=new FieldRunSession(); session.start(fieldPlan(),true,now); session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1));
        List<Runnable> clientQueue=new ArrayList<>(); long displayed=session.snapshot(now).getRevision();
        Consumer<FieldRunCommand> button=ChecklistCommandGuard.bind(displayed,(command,expected) -> clientQueue.add(() -> ChecklistCommandGuard.dispatch(expected,session.getRevision(),() -> session.command(command,now.plusSeconds(4)))));
        button.accept(FieldRunCommand.SKIP);
        FieldPatchDefinition patch=FieldPatchDefinition.FALADOR_NORTH;
        session.observe(patch.getLocation(),Map.of(patch,new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT,10),now.plusSeconds(2),FarmingGrowthRate.NORMAL)),now.plusSeconds(2));
        assertEquals(FieldRunStep.HARVEST,session.snapshot(now).getStep()); clientQueue.get(0).run();
        assertEquals(FieldRunStep.HARVEST,session.snapshot(now).getStep()); assertEquals(0,session.snapshot(now).getSkipped());
    }
    @Test public void elapsedTimeAndRepeatedSameStepObservationsDoNotInvalidateVisibleCommands()
    {
        FieldRunSession session=new FieldRunSession(); session.start(fieldPlan(),true,now); session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1));
        FieldPatchDefinition patch=FieldPatchDefinition.FALADOR_NORTH;
        for(int second:new int[]{2,3})
        {
            session.observe(patch.getLocation(),Map.of(patch,new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT,-1),now.plusSeconds(second),FarmingGrowthRate.NORMAL)),now.plusSeconds(second));
            if(second==2) { long revision=session.getRevision(); assertEquals(revision,session.snapshot(now.plusSeconds(999)).getRevision()); }
        }
        long stable=session.getRevision();
        session.observe(patch.getLocation(),Map.of(patch,new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT,-1),now.plusSeconds(4),FarmingGrowthRate.NORMAL)),now.plusSeconds(4));
        assertEquals(stable,session.getRevision());
        assertFalse(ChecklistCommandGuard.dispatch(-1,session.getRevision(),() -> fail("Missing displayed snapshot")));
    }
}
