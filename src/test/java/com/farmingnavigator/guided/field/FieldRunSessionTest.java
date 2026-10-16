package com.farmingnavigator.guided.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
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

public class FieldRunSessionTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final FieldRunSession session = new FieldRunSession();
    private FieldPatchDefinition patch = FieldPatchDefinition.FALADOR_NORTH;
    private FieldRunPlan plan(FieldRunCrop crop, FieldRunMode mode, Map<FieldPatchDefinition, FieldPatchObservation> observations)
    {
        return new FieldRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.of(Quest.THE_GREAT_BRAIN_ROBBERY), false), EnumSet.of(patch), crop, mode, true, observations, null, now);
    }
    private void start(FieldRunCrop crop, FieldRunMode mode)
    {
        assertTrue(session.start(plan(crop, mode, Map.of()), true, now)); session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1));
    }
    private FieldRunStep step() { return session.snapshot(now.plusSeconds(100)).getStep(); }
    private FieldPatchObservation observation(int raw, int second)
    {
        return new FieldPatchObservation(new FieldPatchStateDecoder().decode(patch.getFamily(), raw), now.plusSeconds(second), FarmingGrowthRate.NORMAL);
    }
    private boolean observe(int raw, int second)
    {
        return session.observe(patch.getLocation(), Map.of(patch, observation(raw, second)), now.plusSeconds(second));
    }
    @Test public void refusesInvalidPlansAndAdvancementDuringPreparation()
    {
        assertFalse(session.start(null, true, now)); assertFalse(session.command(FieldRunCommand.CONFIRM, now));
        assertTrue(session.start(plan(FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, Map.of()), true, now));
        assertFalse(session.command(FieldRunCommand.MARK_COMPLETE, now)); assertFalse(observe(10, 1)); assertEquals(FieldRunStep.PREPARE, step());
    }
    @Test public void harvestNeedsEmptyThenMatchingPlantAndManualProtection()
    {
        start(FieldRunCrop.POTATO, FieldRunMode.KEEP_MATURE_FLOWERS);
        observe(10, 2); assertEquals(FieldRunStep.HARVEST, step());
        observe(12, 3); assertEquals(FieldRunStep.HARVEST, step()); assertFalse(session.command(FieldRunCommand.CONFIRM, now.plusSeconds(3)));
        observe(0, 4); assertEquals(FieldRunStep.PLANT, step());
        assertTrue(session.snapshot(now).getInstruction().contains("3 Potato seed"));
        observe(6, 5); assertEquals(FieldRunStep.PROTECT, step()); assertEquals(0, session.snapshot(now).getProtectionConfirmed());
        assertFalse(observe(6, 6)); session.command(FieldRunCommand.CONFIRM, now.plusSeconds(7));
        assertEquals(FieldRunStep.COMPLETE, step()); assertEquals(1, session.snapshot(now).getCompleted());
        assertEquals(1, session.snapshot(now).getProtectionConfirmed()); assertEquals(Duration.ofSeconds(7), session.snapshot(now.plusSeconds(500)).getElapsed());
    }
    @Test public void keepFlowerRequiresExplicitConfirmationWithoutHarvestOrReplacement()
    {
        patch = FieldPatchDefinition.FALADOR_FLOWER;
        assertTrue(session.start(plan(FieldRunCrop.LIMPWURT, FieldRunMode.KEEP_MATURE_FLOWERS, Map.of(patch, observation(41, 0))), true, now));
        session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1)); observe(41, 2);
        assertEquals(FieldRunStep.KEEP_FLOWER, step()); assertTrue(session.snapshot(now).getInstruction().contains("keep this mature flower"));
        assertFalse(observe(41, 3)); session.command(FieldRunCommand.CONFIRM, now.plusSeconds(4));
        assertEquals(FieldRunStep.COMPLETE, step()); assertEquals(1, session.snapshot(now).getManual()); assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void explicitFlowerHarvestWaitsForEmptyAndNeverRequestsFarmer()
    {
        patch = FieldPatchDefinition.FALADOR_FLOWER; start(FieldRunCrop.WHITE_LILY, FieldRunMode.HARVEST_ALL);
        observe(41, 2); assertEquals(FieldRunStep.HARVEST, step()); observe(0, 3); assertEquals(FieldRunStep.PLANT, step());
        assertTrue(session.snapshot(now).getInstruction().contains("1 White lily seed")); observe(37, 4); assertEquals(FieldRunStep.COMPLETE, step());
        assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void harmonyNeverRequestsFarmerPayment()
    {
        patch = FieldPatchDefinition.HARMONY_ISLAND_ALLOTMENT; start(FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL);
        observe(0, 2); observe(6, 3); assertEquals(FieldRunStep.COMPLETE, step()); assertEquals(0, session.snapshot(now).getProtectionConfirmed());
    }
    @Test public void cureRequiresSameCropAndCannotClaimReplacement()
    {
        assertTrue(session.start(plan(FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, Map.of(patch, observation(135, 0))), true, now));
        session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1)); observe(135, 2); assertEquals(FieldRunStep.CURE, step());
        observe(13, 3); assertEquals(FieldRunStep.WAIT, step()); assertEquals(0, session.snapshot(now).getCompleted());
        session.cancel(); start(FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL); observe(135, 2); observe(6, 3); assertEquals(FieldRunStep.COMPLETE, step());
    }
    @Test public void unreservedFlowerOrDiseaseThatBecomesEmptyDoesNotRequestSeeds()
    {
        patch = FieldPatchDefinition.FALADOR_FLOWER;
        assertTrue(session.start(plan(FieldRunCrop.MARIGOLD, FieldRunMode.KEEP_MATURE_FLOWERS, Map.of(patch, observation(12, 0))), true, now));
        session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1)); observe(0, 2); assertEquals(FieldRunStep.COMPLETE, step());
    }
    @Test public void oldFutureFarWrongPlaneWrongFamilyAndStaleEvidenceCannotChooseActions()
    {
        start(FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL); WorldPoint anchor = patch.getLocation();
        assertFalse(session.observe(anchor, Map.of(patch, observation(10, 0)), now.plusSeconds(2)));
        assertFalse(session.observe(anchor, Map.of(patch, observation(10, 3)), now.plusSeconds(2)));
        assertFalse(session.observe(anchor, Map.of(patch, observation(10, 2)), now.plusSeconds(33)));
        assertFalse(session.observe(new WorldPoint(anchor.getX()+13, anchor.getY(), 0), Map.of(patch, observation(10, 2)), now.plusSeconds(2)));
        assertFalse(session.observe(new WorldPoint(anchor.getX(), anchor.getY(), 1), Map.of(patch, observation(10, 2)), now.plusSeconds(2)));
        FieldPatchObservation wrong = new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.FLOWER, 12), now.plusSeconds(2), FarmingGrowthRate.NORMAL);
        assertFalse(session.observe(anchor, Map.of(patch, wrong), now.plusSeconds(2))); assertEquals(FieldRunStep.TRAVEL, step());
    }
    @Test public void unknownWrongCropAndPassingTimeNeverProvePlantingOrReadiness()
    {
        start(FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL); observe(0, 2); observe(-1, 3); assertEquals(FieldRunStep.INSPECT, step());
        observe(0, 4); observe(13, 5); assertEquals(FieldRunStep.WAIT, step()); observe(13, 20000); assertEquals(FieldRunStep.WAIT, step());
        assertEquals(0, session.snapshot(now).getCompleted());
    }
    @Test public void deadCropsClearAndWeedsRakeBeforePlanting()
    {
        start(FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL); observe(199, 2); assertEquals(FieldRunStep.CLEAR, step());
        observe(3, 3); assertEquals(FieldRunStep.RAKE, step()); observe(0, 4); assertEquals(FieldRunStep.PLANT, step());
    }
    @Test public void newScarecrowNeverClaimsFlowerPlanting()
    {
        patch = FieldPatchDefinition.FALADOR_FLOWER; start(FieldRunCrop.MARIGOLD, FieldRunMode.HARVEST_ALL);
        observe(36, 2); assertEquals(FieldRunStep.SCARECROW, step()); observe(35, 3); assertEquals(FieldRunStep.SCARECROW, step());
        assertEquals(0, session.snapshot(now).getCompleted());
    }
    @Test public void manualCompletionSkipAndCancellationAreDistinct()
    {
        FieldRunPlan two = new FieldRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false),
                EnumSet.of(FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_SOUTH), FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, false, Map.of(), null, now);
        session.start(two, false, now); session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1));
        session.command(FieldRunCommand.MARK_COMPLETE, now.plusSeconds(2)); session.command(FieldRunCommand.SKIP, now.plusSeconds(3));
        FieldRunSnapshot result = session.snapshot(now.plusSeconds(100)); assertEquals(1, result.getCompleted()); assertEquals(1, result.getManual()); assertEquals(1, result.getSkipped());
        assertEquals(Duration.ofSeconds(3), result.getElapsed()); session.cancel(); assertEquals(Duration.ZERO, session.snapshot(now).getElapsed()); assertNull(session.snapshot(now).getCrop());
    }
    @Test public void compostApplicationRequiresManualConfirmationBeforePlanting()
    {
        FieldRunPlan plan = new FieldRunPlanner().plan(new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false), EnumSet.of(patch),
                FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, true, FieldRunCompost.ULTRACOMPOST, Map.of(), null, now);
        session.start(plan, true, now); session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1));
        observe(0,2); assertEquals(FieldRunStep.COMPOST, step()); assertTrue(session.snapshot(now).getInstruction().contains("Ultracompost"));
        assertEquals(FieldRunCompost.ULTRACOMPOST, session.snapshot(now).getCompost());
        observe(0,3); assertEquals(FieldRunStep.COMPOST, step());
        session.command(FieldRunCommand.CONFIRM, now.plusSeconds(4)); assertEquals(FieldRunStep.PLANT, step());
        observe(-1,5); assertEquals(FieldRunStep.INSPECT, step());
        observe(0,6); assertEquals(FieldRunStep.PLANT, step());
        observe(6,7); assertEquals(FieldRunStep.PROTECT, step());
    }

    @Test public void hopsHarvestCountersDoNotCompleteBeforeEmptyAndFourSeedPlanting()
    {
        patch=FieldPatchDefinition.LUMBRIDGE_HOPS; start(FieldRunCrop.BARLEY,FieldRunMode.KEEP_MATURE_FLOWERS);
        observe(78,2); assertEquals(FieldRunStep.HARVEST,step()); observe(80,3); assertEquals(FieldRunStep.HARVEST,step());
        observe(0,4); assertEquals(FieldRunStep.PLANT,step()); assertTrue(session.snapshot(now).getInstruction().contains("4 Barley seed"));
        observe(74,5); assertEquals(FieldRunStep.PROTECT,step());
        session.command(FieldRunCommand.CONFIRM,now.plusSeconds(6)); assertEquals(FieldRunStep.COMPLETE,step());
    }
    @Test public void juteRequiresThreeSeedsAndWrongHopsCropNeverProvesPlanting()
    {
        patch=FieldPatchDefinition.YANILLE_HOPS; start(FieldRunCrop.JUTE,FieldRunMode.HARVEST_ALL); observe(0,2);
        assertTrue(session.snapshot(now).getInstruction().contains("3 Jute seed")); observe(74,3); assertEquals(FieldRunStep.WAIT,step());
        assertEquals(0,session.snapshot(now).getCompleted());
    }

}
