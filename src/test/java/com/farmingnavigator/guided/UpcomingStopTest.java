package com.farmingnavigator.guided;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.guided.tree.*;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.guided.field.*;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpcomingStopTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
    @Test public void herbPreviewAdvancesAfterSkipAndClearsAfterCompletionOrCancel()
    {
        HerbRunPlan plan=new HerbRunPlanner().plan(account,List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of()),new HerbPatchStatus(HerbPatchDefinition.CATHERBY,List.of())),EnumSet.of(HerbPatchDefinition.FALADOR,HerbPatchDefinition.CATHERBY),HerbRunCrop.RANARR,HerbRunCompost.NONE,Map.of(),Map.of(),null,now); assertEquals(2,plan.getStops().size());
        GuidedRunSession session=new GuidedRunSession(); assertTrue(session.start(plan,HerbRunCompost.NONE,now));
        GuidedRunSnapshot before=session.snapshot(now); assertSame(plan.getStops().get(1),before.getNextStop());
        assertTrue(session.confirm(now.plusSeconds(1))); assertTrue(session.skip(now.plusSeconds(2)));
        assertEquals(before.getNextStop().getPatch(),session.snapshot(now).getStop().getPatch());
        assertNull(session.snapshot(now).getNextStop()); assertSame(plan.getStops().get(1),before.getNextStop());
        assertTrue(session.markComplete(now.plusSeconds(3))); assertNull(session.snapshot(now).getNextStop());
        assertTrue(session.start(plan,HerbRunCompost.NONE,now)); assertNotNull(session.snapshot(now).getNextStop());
        session.cancel(); assertNull(session.snapshot(now).getNextStop());
    }
    @Test public void treePreviewAdvancesAfterSkipAndClearsAfterCompletionOrCancel()
    {
        TreeRunPlan plan=new TreeRunPlanner().plan(account,EnumSet.of(TreePatchDefinition.VARROCK_TREE,TreePatchDefinition.FALADOR_TREE),TreeRunCrop.OAK,false,Map.of(),null,now); assertEquals(2,plan.getStops().size());
        TreeRunSession session=new TreeRunSession(); assertTrue(session.start(plan,false,now));
        TreeRunSnapshot before=session.snapshot(now); assertSame(plan.getStops().get(1),before.getNextStop());
        assertTrue(session.command(TreeRunCommand.CONFIRM,now.plusSeconds(1))); assertTrue(session.command(TreeRunCommand.SKIP,now.plusSeconds(2)));
        assertEquals(before.getNextStop().getPatch(),session.snapshot(now).getStop().getPatch());
        assertNull(session.snapshot(now).getNextStop()); assertSame(plan.getStops().get(1),before.getNextStop());
        assertTrue(session.command(TreeRunCommand.MARK_COMPLETE,now.plusSeconds(3))); assertNull(session.snapshot(now).getNextStop());
        assertTrue(session.start(plan,false,now)); assertNotNull(session.snapshot(now).getNextStop());
        session.cancel(); assertNull(session.snapshot(now).getNextStop());
    }
    @Test public void gardenPreviewAdvancesAfterSkipAndClearsAfterCompletionOrCancel()
    {
        GardenRunPlan plan=new GardenRunPlanner().plan(account,EnumSet.of(GardenPatchDefinition.RIMMINGTON_BUSH,GardenPatchDefinition.CHAMPIONS_GUILD_BUSH),GardenRunCrop.REDBERRIES,GardenRunMode.KEEP_MATURE,false,Map.of(),null,now); assertEquals(2,plan.getStops().size());
        GardenRunSession session=new GardenRunSession(); assertTrue(session.start(plan,false,now));
        GardenRunSnapshot before=session.snapshot(now); assertSame(plan.getStops().get(1),before.getNextStop());
        assertTrue(session.command(GardenRunCommand.CONFIRM,now.plusSeconds(1))); assertTrue(session.command(GardenRunCommand.SKIP,now.plusSeconds(2)));
        assertEquals(before.getNextStop().getPatch(),session.snapshot(now).getStop().getPatch());
        assertNull(session.snapshot(now).getNextStop()); assertSame(plan.getStops().get(1),before.getNextStop());
        assertTrue(session.command(GardenRunCommand.MARK_COMPLETE,now.plusSeconds(3))); assertNull(session.snapshot(now).getNextStop());
        assertTrue(session.start(plan,false,now)); assertNotNull(session.snapshot(now).getNextStop());
        session.cancel(); assertNull(session.snapshot(now).getNextStop());
    }
    @Test public void fieldPreviewAdvancesAfterSkipAndClearsAfterCompletionOrCancel()
    {
        FieldRunPlan plan=new FieldRunPlanner().plan(account,EnumSet.of(FieldPatchDefinition.FALADOR_NORTH,FieldPatchDefinition.FALADOR_SOUTH),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,false,Map.of(),null,now); assertEquals(2,plan.getStops().size());
        FieldRunSession session=new FieldRunSession(); assertTrue(session.start(plan,false,now));
        FieldRunSnapshot before=session.snapshot(now); assertSame(plan.getStops().get(1),before.getNextStop());
        assertTrue(session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1))); assertTrue(session.command(FieldRunCommand.SKIP,now.plusSeconds(2)));
        assertEquals(before.getNextStop().getPatch(),session.snapshot(now).getStop().getPatch());
        assertNull(session.snapshot(now).getNextStop()); assertSame(plan.getStops().get(1),before.getNextStop());
        assertTrue(session.command(FieldRunCommand.MARK_COMPLETE,now.plusSeconds(3))); assertNull(session.snapshot(now).getNextStop());
        assertTrue(session.start(plan,false,now)); assertNotNull(session.snapshot(now).getNextStop());
        session.cancel(); assertNull(session.snapshot(now).getNextStop());
    }
}
