package com.farmingnavigator.guided.combined;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.guided.*;
import com.farmingnavigator.guided.tree.*;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.guided.field.*;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.farmingnavigator.routing.run.CombinedRunKind.*;

/** Exercises the coordinator with real checklist sessions, without a client or game input. */
public class CombinedGuidanceChecklistTest
{
    private final Instant now = Instant.parse("2026-10-17T14:00:00Z");
    private final GuidedRunSession herb = new GuidedRunSession();
    private final TreeRunSession tree = new TreeRunSession();
    private final GardenRunSession garden = new GardenRunSession();
    private final FieldRunSession field = new FieldRunSession();
    private final CombinedGuidanceSession combined = new CombinedGuidanceSession();
    private int starts;

    private boolean start(CombinedRunKind kind)
    {
        AccountState account = new AccountState(true, 99, 99, java.util.EnumSet.noneOf(Quest.class), false);
        herb.cancel(); tree.cancel(); garden.cancel(); field.cancel(); starts++;
        switch (kind)
        {
            case HERB: return herb.start(new HerbRunPlanner().plan(account,
                    List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR, List.of())), Set.of(HerbPatchDefinition.FALADOR),
                    HerbRunCrop.RANARR, HerbRunCompost.NONE, Map.of(), Map.of(), null, now), HerbRunCompost.NONE, now);
            case TREE: return tree.start(new TreeRunPlanner().plan(account, Set.of(TreePatchDefinition.VARROCK_TREE),
                    TreeRunCrop.OAK, false, Map.of(), null, now), false, now);
            case GARDEN: return garden.start(new GardenRunPlanner().plan(account, Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),
                    GardenRunCrop.REDBERRIES, GardenRunMode.REPLACE_MATURE, false, Map.of(), null, now), false, now);
            case FIELD: return field.start(new FieldRunPlanner().plan(account, Set.of(FieldPatchDefinition.FALADOR_NORTH),
                    FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, false, Map.of(), null, now), false, now);
            default: return false;
        }
    }
    private CombinedGuidanceSession.Progress progress(boolean active, boolean complete)
    { return active ? CombinedGuidanceSession.Progress.ACTIVE : complete ? CombinedGuidanceSession.Progress.COMPLETE : CombinedGuidanceSession.Progress.INACTIVE; }
    private void observe()
    {
        combined.observe("profile", Map.of(
                HERB, progress(herb.snapshot().isActive(), herb.snapshot().getStep() == GuidedRunStep.COMPLETE),
                TREE, progress(tree.snapshot(now).isActive(), tree.snapshot(now).getStep() == TreeRunStep.COMPLETE),
                GARDEN, progress(garden.snapshot(now).isActive(), garden.snapshot(now).getStep() == GardenRunStep.COMPLETE),
                FIELD, progress(field.snapshot(now).isActive(), field.snapshot(now).getStep() == FieldRunStep.COMPLETE)));
    }

    @Test public void allFourComponentsBeginAtPreparationAndOnlyExplicitNextStartsAnother()
    {
        assertTrue(combined.begin("profile", Set.of(FIELD, GARDEN, TREE, HERB), this::start));
        assertEquals(GuidedRunStep.PREPARE, herb.snapshot().getStep());
        assertFalse(herb.markComplete(now)); observe();
        assertEquals(CombinedGuidanceSession.State.RUNNING, combined.snapshot().getState());
        herb.confirm(now.plusSeconds(1)); herb.markComplete(now.plusSeconds(2)); observe();
        assertEquals(CombinedGuidanceSession.State.BETWEEN, combined.snapshot().getState());
        assertEquals(TreeRunStep.INACTIVE, tree.snapshot(now).getStep()); assertEquals(1, starts);
        assertTrue(combined.next("profile", this::start));
        assertEquals(TreeRunStep.PREPARE, tree.snapshot(now).getStep());
        tree.command(TreeRunCommand.CONFIRM, now.plusSeconds(3));
        tree.command(TreeRunCommand.SKIP, now.plusSeconds(4)); observe();
        assertEquals(1, tree.snapshot(now).getSkipped());
        assertEquals(CombinedGuidanceSession.State.BETWEEN, combined.snapshot().getState());
        assertEquals(GardenRunStep.INACTIVE, garden.snapshot(now).getStep()); assertEquals(2, starts);
        assertTrue(combined.next("profile", this::start));
        assertEquals(GardenRunStep.PREPARE, garden.snapshot(now).getStep());
        garden.command(GardenRunCommand.CONFIRM, now.plusSeconds(5));
        garden.command(GardenRunCommand.MARK_COMPLETE, now.plusSeconds(6)); observe();
        assertEquals(CombinedGuidanceSession.State.BETWEEN, combined.snapshot().getState());
        assertEquals(FieldRunStep.INACTIVE, field.snapshot(now).getStep()); assertEquals(3, starts);
        assertTrue(combined.next("profile", this::start));
        assertEquals(FieldRunStep.PREPARE, field.snapshot(now).getStep());
        field.command(FieldRunCommand.CONFIRM, now.plusSeconds(7));
        field.command(FieldRunCommand.MARK_COMPLETE, now.plusSeconds(8)); observe();
        assertEquals(CombinedGuidanceSession.State.COMPLETE, combined.snapshot().getState());
        assertEquals(4, starts); assertEquals(4, combined.snapshot().getFinished());
        assertTrue(GuidedRunTargets.fromField(field.snapshot(now), null, null).getInventoryItems().isEmpty());
    }

    @Test public void cancellingAComponentEndsTheQueueButEndingTheQueueKeepsTheComponent()
    {
        combined.begin("profile", Set.of(TREE, GARDEN), this::start); tree.cancel(); observe();
        assertEquals(CombinedGuidanceSession.State.INACTIVE, combined.snapshot().getState());
        assertEquals(GardenRunStep.INACTIVE, garden.snapshot(now).getStep());
        combined.begin("profile", Set.of(TREE, GARDEN), this::start); combined.clear();
        assertEquals(TreeRunStep.PREPARE, tree.snapshot(now).getStep());
        assertTrue(tree.snapshot(now).isActive());
    }
}
