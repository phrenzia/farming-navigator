package com.farmingnavigator.guided.garden;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.routing.run.*;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenRunSessionTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final GardenRunSession session = new GardenRunSession();
    private GardenPatchDefinition patch = GardenPatchDefinition.RIMMINGTON_BUSH;
    private GardenRunPlan plan(GardenRunCrop crop, GardenRunMode mode, Map<GardenPatchDefinition, GardenPatchObservation> observations)
    {
        return new GardenRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false),
                EnumSet.of(patch), crop, mode, true, observations, null, now);
    }
    private void start(GardenRunCrop crop, GardenRunMode mode)
    {
        assertTrue(session.start(plan(crop, mode, Map.of()), true, now));
        session.command(GardenRunCommand.CONFIRM, now.plusSeconds(1));
    }
    private GardenRunStep step() { return session.snapshot(now.plusSeconds(100)).getStep(); }
    private GardenPatchObservation observation(int raw, int second)
    {
        return new GardenPatchObservation(new GardenPatchStateDecoder().decode(patch.getFamily(), raw), now.plusSeconds(second), FarmingGrowthRate.NORMAL);
    }
    private boolean observe(int raw, int second)
    {
        return session.observe(patch.getLocation(), Map.of(patch, observation(raw, second)), now.plusSeconds(second));
    }
    @Test public void refusesInvalidPlansAndCompletionBeforePreparation()
    {
        assertFalse(session.start(null, true, now)); assertFalse(session.command(GardenRunCommand.CONFIRM, now));
        assertTrue(session.start(plan(GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE, Map.of()), true, now));
        assertFalse(session.command(GardenRunCommand.MARK_COMPLETE, now)); assertFalse(observe(250, 1));
        assertEquals(GardenRunStep.PREPARE, step());
    }
    @Test public void replacementNeedsHealthCheckManualProduceChoiceEmptyPlantAndProtectionEvidence()
    {
        start(GardenRunCrop.REDBERRIES, GardenRunMode.REPLACE_MATURE);
        observe(250, 2); assertEquals(GardenRunStep.CHECK_HEALTH, step());
        observe(14, 3); assertEquals(GardenRunStep.HARVEST, step());
        assertFalse(session.command(GardenRunCommand.REPLACE_PLANT, now.plusSeconds(4)));
        observe(10, 5); assertEquals(GardenRunStep.HARVEST, step()); assertEquals(0, session.snapshot(now).getCompleted());
        assertTrue(session.command(GardenRunCommand.CONFIRM, now.plusSeconds(6))); assertEquals(GardenRunStep.KEEP_OR_REPLACE, step());
        assertTrue(session.command(GardenRunCommand.REPLACE_PLANT, now.plusSeconds(7))); assertEquals(GardenRunStep.CLEAR, step());
        observe(0, 8); assertEquals(GardenRunStep.PLANT, step());
        observe(5, 9); assertEquals(GardenRunStep.PROTECT, step());
        assertFalse(observe(5, 10)); assertEquals(0, session.snapshot(now).getCompleted());
        session.command(GardenRunCommand.CONFIRM, now.plusSeconds(11));
        GardenRunSnapshot result = session.snapshot(now.plusSeconds(100));
        assertEquals(GardenRunStep.COMPLETE, result.getStep()); assertEquals(1, result.getCompleted());
        assertEquals(1, result.getProtectionConfirmed()); assertEquals(Duration.ofSeconds(11), result.getElapsed());
    }
    @Test public void keepModeFinishesOnlyAfterManualProduceConfirmationWithoutPaymentClaim()
    {
        assertTrue(session.start(plan(GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE, Map.of(patch, observation(250, 0))), true, now));
        session.command(GardenRunCommand.CONFIRM, now.plusSeconds(1)); observe(250, 2); observe(14, 3);
        assertEquals(GardenRunStep.HARVEST, step());
        assertTrue(session.snapshot(now).getInstruction().contains("not automatically verified"));
        assertFalse(session.command(GardenRunCommand.REPLACE_PLANT, now.plusSeconds(4)));
        session.command(GardenRunCommand.CONFIRM, now.plusSeconds(5));
        assertEquals(GardenRunStep.COMPLETE, step()); assertEquals(1, session.snapshot(now).getManual());
        assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void poisonIvyPlantingNeverRequestsFarmerPayment()
    {
        start(GardenRunCrop.POISON_IVY, GardenRunMode.KEEP_MATURE);
        observe(0, 2); assertEquals(GardenRunStep.PLANT, step());
        observe(197, 3); assertEquals(GardenRunStep.COMPLETE, step());
        assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void bushesPruneAndCactiCureOnlyAfterHealthySameCropEvidence()
    {
        start(GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE);
        observe(70, 2); assertEquals(GardenRunStep.PRUNE, step()); observe(5, 3); assertEquals(GardenRunStep.COMPLETE, step());
        patch = GardenPatchDefinition.AL_KHARID_CACTUS; start(GardenRunCrop.CACTUS, GardenRunMode.KEEP_MATURE);
        observe(19, 2); assertEquals(GardenRunStep.CURE, step());
        observe(32, 3); assertEquals(GardenRunStep.WAIT, step()); assertEquals(0, session.snapshot(now).getCompleted());
        session.cancel(); start(GardenRunCrop.CACTUS, GardenRunMode.KEEP_MATURE);
        observe(19, 2); observe(8, 3); assertEquals(GardenRunStep.COMPLETE, step());
    }
    @Test public void unreservedPlantClearedElsewhereCannotRequestUnbudgetedReplant()
    {
        assertTrue(session.start(plan(GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE, Map.of(patch, observation(250, 0))), true, now));
        session.command(GardenRunCommand.CONFIRM, now.plusSeconds(1)); observe(0, 2);
        assertEquals(GardenRunStep.COMPLETE, step()); assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void oldFutureFarAwayWrongPlaneAndWrongFamilyEvidenceCannotChooseActions()
    {
        start(GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE);
        WorldPoint anchor = patch.getLocation();
        assertFalse(session.observe(anchor, Map.of(patch, observation(250, 0)), now.plusSeconds(2)));
        assertFalse(session.observe(anchor, Map.of(patch, observation(250, 3)), now.plusSeconds(2)));
        assertFalse(session.observe(new WorldPoint(anchor.getX() + 13, anchor.getY(), 0), Map.of(patch, observation(250, 2)), now.plusSeconds(2)));
        assertFalse(session.observe(new WorldPoint(anchor.getX(), anchor.getY(), 1), Map.of(patch, observation(250, 2)), now.plusSeconds(2)));
        GardenPatchObservation wrong = new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.CACTUS, 31), now.plusSeconds(2), FarmingGrowthRate.NORMAL);
        assertFalse(session.observe(anchor, Map.of(patch, wrong), now.plusSeconds(2))); assertEquals(GardenRunStep.TRAVEL, step());
    }
    @Test public void unknownAndDifferentHealthyCropCannotClaimPlanting()
    {
        start(GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE); observe(0, 2); observe(-1, 3);
        assertEquals(GardenRunStep.INSPECT, step()); assertEquals(0, session.snapshot(now).getCompleted());
        observe(0, 4); observe(15, 5); assertEquals(GardenRunStep.WAIT, step()); assertEquals(0, session.snapshot(now).getCompleted());
    }
    @Test public void timerPassageDoesNotProveHealthReadinessAndCancelClearsElapsedHistory()
    {
        start(GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE); observe(5, 2); observe(5, 20000);
        assertEquals(GardenRunStep.WAIT, step()); assertEquals(0, session.snapshot(now).getCompleted());
        session.cancel(); GardenRunSnapshot result = session.snapshot(now.plusSeconds(100));
        assertEquals(GardenRunStep.INACTIVE, result.getStep()); assertEquals(Duration.ZERO, result.getElapsed());
        assertNull(result.getCrop()); assertNull(result.getStop()); assertEquals(0, result.getTotal());
    }
    @Test public void manualCompletionSkippingAndFinishedElapsedTimeStayDistinct()
    {
        GardenRunPlan two = new GardenRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false),
                EnumSet.of(GardenPatchDefinition.RIMMINGTON_BUSH, GardenPatchDefinition.CHAMPIONS_GUILD_BUSH), GardenRunCrop.REDBERRIES,
                GardenRunMode.KEEP_MATURE, false, Map.of(), null, now);
        assertTrue(session.start(two, false, now)); session.command(GardenRunCommand.CONFIRM, now.plusSeconds(1));
        session.command(GardenRunCommand.MARK_COMPLETE, now.plusSeconds(2)); session.command(GardenRunCommand.SKIP, now.plusSeconds(3));
        GardenRunSnapshot result = session.snapshot(now.plusSeconds(100));
        assertEquals(1, result.getCompleted()); assertEquals(1, result.getManual()); assertEquals(1, result.getSkipped());
        assertEquals(Duration.ofSeconds(3), result.getElapsed());
    }
}
