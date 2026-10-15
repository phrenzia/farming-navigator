package com.farmingnavigator.guided.tree;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.routing.run.*;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeRunSessionTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final TreeRunSession session = new TreeRunSession();
    private TreePatchDefinition patch = TreePatchDefinition.VARROCK_TREE;
    private TreeRunPlan plan(TreeRunCrop crop, TreePatchDefinition target)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Map.of(Quest.BONE_VOYAGE, QuestState.FINISHED, Quest.MOURNINGS_END_PART_I, QuestState.IN_PROGRESS), Map.of(), Map.of());
        return new TreeRunPlanner().plan(account, EnumSet.of(target), crop, true, Map.of(), null, now);
    }
    private void start(TreeRunCrop crop, TreePatchDefinition target, boolean protect)
    {
        patch = target;
        assertTrue(session.start(plan(crop, patch), protect, now));
        assertEquals(TreeRunStep.PREPARE, step());
        assertTrue(session.command(TreeRunCommand.CONFIRM, now.plusSeconds(1)));
    }
    private TreeRunStep step() { return session.snapshot(now.plusSeconds(100)).getStep(); }
    private TreePatchObservation observation(int raw, Instant at)
    {
        return new TreePatchObservation(new TreePatchStateDecoder().decode(patch.getFamily(), raw), at, FarmingGrowthRate.NORMAL);
    }
    private boolean observe(int raw, int seconds)
    {
        Instant at = now.plusSeconds(seconds);
        return session.observe(TreePatchDestination.valueOf(patch.name()).getLocation(), Map.of(patch, observation(raw, at)), at);
    }
    @Test public void refusesInvalidPlansAndActionsBeforePreparation()
    {
        assertFalse(session.start(null, true, now));
        assertFalse(session.command(TreeRunCommand.CONFIRM, now));
        assertTrue(session.start(plan(TreeRunCrop.OAK, patch), true, now));
        assertFalse(session.command(TreeRunCommand.MARK_COMPLETE, now));
        assertFalse(session.command(TreeRunCommand.SKIP, now));
        assertFalse(observe(8, 1));
        assertEquals(TreeRunStep.PREPARE, step());
    }
    @Test public void healthCheckRemovalPlantAndProtectionRequireSeparateEvidence()
    {
        start(TreeRunCrop.OAK, patch, true);
        assertTrue(observe(12, 2)); assertEquals(TreeRunStep.CHECK_HEALTH, step());
        assertFalse(session.command(TreeRunCommand.CONFIRM, now.plusSeconds(3)));
        assertTrue(observe(13, 4)); assertEquals(TreeRunStep.KEEP_OR_REMOVE, step());
        assertTrue(session.command(TreeRunCommand.REMOVE_TREE, now.plusSeconds(5)));
        assertEquals(TreeRunStep.REMOVE, step());
        assertTrue(observe(14, 6)); assertEquals(TreeRunStep.CLEAR, step());
        assertTrue(observe(0, 7)); assertEquals(TreeRunStep.PLANT, step());
        assertTrue(observe(8, 8)); assertEquals(TreeRunStep.PROTECT, step());
        assertFalse(observe(8, 9)); assertEquals(0, session.snapshot(now).getCompleted());
        assertTrue(session.command(TreeRunCommand.CONFIRM, now.plusSeconds(10)));
        TreeRunSnapshot result = session.snapshot(now.plusSeconds(100));
        assertEquals(TreeRunStep.COMPLETE, result.getStep()); assertEquals(1, result.getCompleted());
        assertEquals(0, result.getManual()); assertEquals(1, result.getProtectionConfirmed());
        assertEquals(Duration.ofSeconds(10), result.getElapsed());
    }
    @Test public void keepingHealthCheckedFruitDoesNotClaimHarvestOrReplacement()
    {
        start(TreeRunCrop.APPLE, TreePatchDefinition.CATHERBY_FRUIT, true);
        observe(14, 2); assertEquals(TreeRunStep.KEEP_OR_REMOVE, step());
        assertTrue(session.snapshot(now).getInstruction().contains("available fruit"));
        session.command(TreeRunCommand.KEEP_TREE, now.plusSeconds(3));
        assertEquals(1, session.snapshot(now).getManual());
        assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void matchingPlantCompletesWithoutProtectionOnlyWhenDisabled()
    {
        start(TreeRunCrop.TEAK, TreePatchDefinition.FOSSIL_ISLAND_EAST, false);
        observe(0, 2); assertEquals(TreeRunStep.PLANT, step());
        observe(8, 3); assertEquals(TreeRunStep.COMPLETE, step());
        assertEquals(0, session.snapshot(now).getManual());
        assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void weedsAndDeadTreesNeedFreshEmptyStateBeforePlanting()
    {
        start(TreeRunCrop.OAK, patch, true);
        observe(1, 2); assertEquals(TreeRunStep.RAKE, step());
        observe(0, 3); assertEquals(TreeRunStep.PLANT, step());
        session.cancel(); start(TreeRunCrop.OAK, patch, true);
        observe(137, 2); assertEquals(TreeRunStep.CLEAR, step());
        observe(0, 3); assertEquals(TreeRunStep.PLANT, step());
    }
    @Test public void diseaseMustBecomeHealthySameCropBeforeObservedCompletion()
    {
        start(TreeRunCrop.OAK, patch, true);
        observe(73, 2); assertEquals(TreeRunStep.PRUNE, step());
        assertFalse(observe(73, 3));
        observe(8, 4); assertEquals(TreeRunStep.COMPLETE, step());
        assertEquals(1, session.snapshot(now).getCompleted());
        assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void differentHealthyCropDoesNotProvePruningOrPlanting()
    {
        start(TreeRunCrop.OAK, patch, true);
        observe(73, 2); observe(15, 3);
        assertEquals(0, session.snapshot(now).getCompleted()); assertEquals(TreeRunStep.WAIT, step());
        session.cancel(); start(TreeRunCrop.OAK, patch, true);
        observe(0, 2); observe(15, 3);
        assertEquals(0, session.snapshot(now).getCompleted()); assertEquals(TreeRunStep.WAIT, step());
    }
    @Test public void oldFutureFarAwayAndWrongPlaneEvidenceCannotAdvance()
    {
        start(TreeRunCrop.OAK, patch, true);
        WorldPoint anchor = TreePatchDestination.VARROCK_TREE.getLocation();
        assertFalse(session.observe(anchor, Map.of(patch, observation(12, now)), now.plusSeconds(2)));
        assertFalse(session.observe(anchor, Map.of(patch, observation(12, now.plusSeconds(3))), now.plusSeconds(2)));
        assertFalse(session.observe(new WorldPoint(anchor.getX(), anchor.getY(), 1), Map.of(patch,
                observation(12, now.plusSeconds(2))), now.plusSeconds(2)));
        assertFalse(session.observe(new WorldPoint(anchor.getX() + 13, anchor.getY(), 0), Map.of(patch,
                observation(12, now.plusSeconds(2))), now.plusSeconds(2)));
        assertFalse(session.observe(null, Map.of(patch, observation(12, now.plusSeconds(2))), now.plusSeconds(2)));
        assertEquals(TreeRunStep.TRAVEL, step());
    }
    @Test public void unknownObservationReturnsToInspectionAndNeverClaimsPlanting()
    {
        start(TreeRunCrop.OAK, patch, true);
        observe(0, 2); assertEquals(TreeRunStep.PLANT, step());
        observe(76, 3); assertEquals(TreeRunStep.INSPECT, step());
        assertEquals(0, session.snapshot(now).getCompleted());
    }
    @Test public void staleEmptyOrGrowingObservationCannotCompleteLaterAction()
    {
        start(TreeRunCrop.OAK, patch, true);
        observe(0, 10);
        assertFalse(session.observe(TreePatchDestination.VARROCK_TREE.getLocation(), Map.of(patch,
                observation(8, now.plusSeconds(9))), now.plusSeconds(11)));
        assertEquals(TreeRunStep.PLANT, step());
    }
    @Test public void timerPassageDoesNotProveReadiness()
    {
        start(TreeRunCrop.OAK, patch, true);
        observe(8, 2); assertEquals(TreeRunStep.WAIT, step());
        observe(8, 20000); assertEquals(TreeRunStep.WAIT, step());
        assertEquals(0, session.snapshot(now).getCompleted());
        observe(12, 20001); assertEquals(TreeRunStep.CHECK_HEALTH, step());
    }
    @Test public void manualCompletionAndSkippingRemainDistinct()
    {
        AccountState account = new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false);
        TreeRunPlan two = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.VARROCK_TREE,
                TreePatchDefinition.TAVERLEY_TREE), TreeRunCrop.OAK, false, Map.of(), null, now);
        session.start(two, false, now); session.command(TreeRunCommand.CONFIRM, now.plusSeconds(1));
        session.command(TreeRunCommand.MARK_COMPLETE, now.plusSeconds(2));
        assertEquals(TreePatchDefinition.TAVERLEY_TREE, session.snapshot(now).getStop().getPatch());
        session.command(TreeRunCommand.SKIP, now.plusSeconds(3));
        TreeRunSnapshot result = session.snapshot(now.plusSeconds(100));
        assertEquals(1, result.getCompleted()); assertEquals(1, result.getManual()); assertEquals(1, result.getSkipped());
        assertEquals(Duration.ofSeconds(3), result.getElapsed());
    }
    @Test public void cancelClearsAllCharacterSpecificSessionState()
    {
        start(TreeRunCrop.OAK, patch, true); session.command(TreeRunCommand.MARK_COMPLETE, now.plusSeconds(2));
        session.cancel(); TreeRunSnapshot result = session.snapshot(now.plusSeconds(100));
        assertEquals(TreeRunStep.INACTIVE, result.getStep()); assertNull(result.getCrop()); assertNull(result.getStop());
        assertEquals(0, result.getTotal()); assertEquals(0, result.getCompleted()); assertEquals(Duration.ZERO, result.getElapsed());
    }
    @Test public void futureWrongFamilyRecordCannotChooseAnAction()
    {
        start(TreeRunCrop.OAK, patch, true);
        assertFalse(session.observe(TreePatchDestination.VARROCK_TREE.getLocation(), Map.of(patch,
                new TreePatchObservation(new TreePatchStateDecoder().decode(TreeFamily.FRUIT_TREE, 34),
                        now.plusSeconds(2), FarmingGrowthRate.NORMAL)), now.plusSeconds(2)));
        assertEquals(TreeRunStep.TRAVEL, step());
    }
}
