package com.farmingnavigator.farming.field;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldAlertTrackerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final FieldAlertTracker tracker = new FieldAlertTracker();
    private final FieldPatchDefinition patch = FieldPatchDefinition.FALADOR_NORTH;
    private Map<FieldPatchDefinition, FieldPatchObservation> observation(int raw, Instant at)
    {
        return Map.of(patch, new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT, raw), at, FarmingGrowthRate.NORMAL));
    }
    private List<String> poll(int raw, Instant observed, Instant time)
    {
        return tracker.poll(observation(raw, observed), EnumSet.of(patch), time, true, true);
    }
    @Test public void restoredHistoryNeverCreatesLoginBurst()
    {
        assertTrue(poll(10, now.minusSeconds(10), now).isEmpty());
        assertTrue(poll(10, now, now).isEmpty());
        tracker.reset(); assertTrue(poll(9, now.minusSeconds(7200), now).isEmpty());
        assertTrue(poll(9, now.minusSeconds(7200), now.plusSeconds(60)).isEmpty());
    }
    @Test public void freshDiseaseDeathAndHealthCheckReadyNotifyOncePerCondition()
    {
        tracker.poll(Map.of(), EnumSet.of(patch), now, true, true);
        assertTrue(poll(135, now, now).get(0).contains("observed diseased"));
        assertTrue(poll(136, now.plusSeconds(1), now.plusSeconds(1)).isEmpty());
        assertTrue(poll(199, now.plusSeconds(2), now.plusSeconds(2)).get(0).contains("observed dead"));
        assertTrue(poll(10, now.plusSeconds(3), now.plusSeconds(3)).get(0).contains("harvestable (quantity unknown)"));
        assertTrue(poll(12, now.plusSeconds(4), now.plusSeconds(4)).isEmpty());
    }
    @Test public void estimateFiresAtUpperWindowBoundaryOnceWithConditionalLanguage()
    {
        poll(9, now, now);
        assertTrue(poll(9, now, now.plusSeconds(599)).isEmpty());
        List<String> alerts = poll(9, now, now.plusSeconds(600));
        assertEquals(1, alerts.size()); assertTrue(alerts.get(0).contains("may be ready to harvest if growth stayed healthy"));
        assertTrue(alerts.get(0).contains("Visit to confirm"));
        assertTrue(poll(9, now, now.plusSeconds(700)).isEmpty());
    }
    @Test public void disabledUnselectedStaleAndFutureStatesNeverNotify()
    {
        tracker.poll(Map.of(), EnumSet.of(patch), now, true, true);
        assertTrue(tracker.poll(observation(10, now), EnumSet.of(patch), now, false, false).isEmpty());
        assertTrue(tracker.poll(observation(135, now), EnumSet.noneOf(FieldPatchDefinition.class), now, true, true).isEmpty());
        assertTrue(poll(199, now.minusSeconds(31), now).isEmpty());
        assertTrue(poll(10, now.plusSeconds(1), now).isEmpty());
    }
    @Test public void turningOnEstimateAlertsDoesNotReplayAlreadyElapsedWindows()
    {
        tracker.poll(observation(9, now), EnumSet.of(patch), now, false, false);
        tracker.poll(observation(9, now), EnumSet.of(patch), now.plusSeconds(600), false, false);
        assertTrue(poll(9, now, now.plusSeconds(601)).isEmpty());
    }
    @Test public void diseaseAndUnknownGrowthRatePreventEstimatedReadiness()
    {
        poll(9, now, now); poll(135, now.plusSeconds(1), now.plusSeconds(1));
        assertTrue(poll(135, now.plusSeconds(1), now.plusSeconds(10000)).isEmpty());
        FieldPatchObservation unknownRate = new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT, 9), now, FarmingGrowthRate.UNKNOWN);
        assertTrue(tracker.poll(Map.of(patch, unknownRate), EnumSet.of(patch), now.plusSeconds(10000), true, true).isEmpty());
    }
    @Test public void newPlantingCanProduceNewEstimateAndProfileResetSuppressesRestoredEstimate()
    {
        poll(9, now, now); poll(9, now, now.plusSeconds(600));
        poll(0, now.plusSeconds(700), now.plusSeconds(700));
        Instant planted = now.plusSeconds(800); poll(9, planted, planted);
        assertEquals(1, poll(9, planted, planted.plusSeconds(600)).size());
        tracker.reset(); assertTrue(poll(9, planted, planted.plusSeconds(601)).isEmpty());
    }
    @Test public void scarecrowsNeverProduceReadinessAlerts()
    {
        FieldPatchDefinition flower = FieldPatchDefinition.FALADOR_FLOWER;
        tracker.poll(Map.of(), EnumSet.of(flower), now, true, true);
        for (int raw = 33; raw <= 36; raw++)
        {
            FieldPatchObservation scarecrow = new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.FLOWER, raw), now, FarmingGrowthRate.NORMAL);
            assertTrue(tracker.poll(Map.of(flower, scarecrow), EnumSet.of(flower), now.plusSeconds(10000), true, true).isEmpty());
        }
    }
    @Test public void mismatchedFamilyCannotEmitObservedOrEstimatedAlerts()
    {
        tracker.poll(Map.of(), EnumSet.of(patch), now, true, true);
        for (int raw : new int[] {40, 41, 166, 230})
        {
            FieldPatchObservation cactus = new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.FLOWER, raw), now, FarmingGrowthRate.NORMAL);
            assertTrue(tracker.poll(Map.of(patch, cactus), EnumSet.of(patch), now.plusSeconds(10000), true, true).isEmpty());
        }
    }
    @Test public void unselectedElapsedEstimateDoesNotReplayWhenSelectedLater()
    {
        tracker.poll(observation(9, now), EnumSet.noneOf(FieldPatchDefinition.class), now, true, true);
        tracker.poll(observation(9, now), EnumSet.noneOf(FieldPatchDefinition.class), now.plusSeconds(600), true, true);
        assertTrue(poll(9, now, now.plusSeconds(601)).isEmpty());
    }
    @Test public void alertsDefaultToOff()
    {
        com.farmingnavigator.FarmingNavigatorConfig config = new com.farmingnavigator.FarmingNavigatorConfig() {};
        assertFalse(config.fieldObservedAlerts()); assertFalse(config.fieldEstimatedAlerts());
    }
    @Test public void observedFlowerAlertNamesCropAndKeepsQuantityUnknown()
    {
        FieldPatchDefinition flower = FieldPatchDefinition.FALADOR_FLOWER;
        tracker.poll(Map.of(), EnumSet.of(flower), now, true, true);
        FieldPatchObservation ready = new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.FLOWER, 41), now, FarmingGrowthRate.NORMAL);
        assertEquals("Falador - Flower - White lily observed harvestable (quantity unknown).",
                tracker.poll(Map.of(flower, ready), EnumSet.of(flower), now, true, true).get(0));
    }

}
