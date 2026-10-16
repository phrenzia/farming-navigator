package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenAlertTrackerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final GardenAlertTracker tracker = new GardenAlertTracker();
    private final GardenPatchDefinition patch = GardenPatchDefinition.RIMMINGTON_BUSH;
    private Map<GardenPatchDefinition, GardenPatchObservation> observation(int raw, Instant at)
    {
        return Map.of(patch, new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH, raw), at, FarmingGrowthRate.NORMAL));
    }
    private List<String> poll(int raw, Instant observed, Instant time)
    {
        return tracker.poll(observation(raw, observed), EnumSet.of(patch), time, true, true);
    }
    @Test public void restoredHistoryNeverCreatesLoginBurst()
    {
        assertTrue(poll(250, now.minusSeconds(10), now).isEmpty());
        assertTrue(poll(250, now, now).isEmpty());
        tracker.reset(); assertTrue(poll(9, now.minusSeconds(7200), now).isEmpty());
        assertTrue(poll(9, now.minusSeconds(7200), now.plusSeconds(60)).isEmpty());
    }
    @Test public void freshDiseaseDeathAndHealthCheckReadyNotifyOncePerCondition()
    {
        tracker.poll(Map.of(), EnumSet.of(patch), now, true, true);
        assertTrue(poll(70, now, now).get(0).contains("observed diseased"));
        assertTrue(poll(71, now.plusSeconds(1), now.plusSeconds(1)).isEmpty());
        assertTrue(poll(134, now.plusSeconds(2), now.plusSeconds(2)).get(0).contains("observed dead"));
        assertTrue(poll(250, now.plusSeconds(3), now.plusSeconds(3)).get(0).contains("ready to check health"));
        assertTrue(poll(10, now.plusSeconds(4), now.plusSeconds(4)).isEmpty());
    }
    @Test public void estimateFiresAtUpperWindowBoundaryOnceWithConditionalLanguage()
    {
        poll(9, now, now);
        assertTrue(poll(9, now, now.plusSeconds(1199)).isEmpty());
        List<String> alerts = poll(9, now, now.plusSeconds(1200));
        assertEquals(1, alerts.size()); assertTrue(alerts.get(0).contains("may be ready to check health if growth stayed healthy"));
        assertTrue(alerts.get(0).contains("Visit to confirm"));
        assertTrue(poll(9, now, now.plusSeconds(1300)).isEmpty());
    }
    @Test public void disabledUnselectedStaleAndFutureStatesNeverNotify()
    {
        tracker.poll(Map.of(), EnumSet.of(patch), now, true, true);
        assertTrue(tracker.poll(observation(250, now), EnumSet.of(patch), now, false, false).isEmpty());
        assertTrue(tracker.poll(observation(70, now), EnumSet.noneOf(GardenPatchDefinition.class), now, true, true).isEmpty());
        assertTrue(poll(134, now.minusSeconds(31), now).isEmpty());
        assertTrue(poll(250, now.plusSeconds(1), now).isEmpty());
    }
    @Test public void turningOnEstimateAlertsDoesNotReplayAlreadyElapsedWindows()
    {
        tracker.poll(observation(9, now), EnumSet.of(patch), now, false, false);
        tracker.poll(observation(9, now), EnumSet.of(patch), now.plusSeconds(1200), false, false);
        assertTrue(poll(9, now, now.plusSeconds(1201)).isEmpty());
    }
    @Test public void diseaseAndUnknownGrowthRatePreventEstimatedReadiness()
    {
        poll(9, now, now); poll(70, now.plusSeconds(1), now.plusSeconds(1));
        assertTrue(poll(70, now.plusSeconds(1), now.plusSeconds(10000)).isEmpty());
        GardenPatchObservation unknownRate = new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH, 9), now, FarmingGrowthRate.UNKNOWN);
        assertTrue(tracker.poll(Map.of(patch, unknownRate), EnumSet.of(patch), now.plusSeconds(10000), true, true).isEmpty());
    }
    @Test public void newPlantingCanProduceNewEstimateAndProfileResetSuppressesRestoredEstimate()
    {
        poll(9, now, now); poll(9, now, now.plusSeconds(1200));
        poll(0, now.plusSeconds(1300), now.plusSeconds(1300));
        Instant planted = now.plusSeconds(1400); poll(9, planted, planted);
        assertEquals(1, poll(9, planted, planted.plusSeconds(1200)).size());
        tracker.reset(); assertTrue(poll(9, planted, planted.plusSeconds(1201)).isEmpty());
    }
    @Test public void healthCheckedCactusNeverClaimsProduceAvailable()
    {
        tracker.poll(Map.of(), EnumSet.of(GardenPatchDefinition.AL_KHARID_CACTUS), now, true, true);
        GardenPatchObservation checked = new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.CACTUS, 15), now, FarmingGrowthRate.NORMAL);
        assertTrue(tracker.poll(Map.of(GardenPatchDefinition.AL_KHARID_CACTUS, checked), EnumSet.of(GardenPatchDefinition.AL_KHARID_CACTUS), now, true, true).isEmpty());
    }
    @Test public void cactusAlertsUseSingularFamilyAndDoNotPredictRegrowth()
    {
        GardenPatchDefinition cactus = GardenPatchDefinition.AL_KHARID_CACTUS;
        tracker.poll(Map.of(), EnumSet.of(cactus), now, true, true);
        GardenPatchObservation ready = new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.CACTUS, 31), now, FarmingGrowthRate.NORMAL);
        String message = tracker.poll(Map.of(cactus, ready), EnumSet.of(cactus), now, true, true).get(0);
        assertEquals("Al Kharid cactus patch observed ready to check health.", message);
        GardenPatchObservation checked = new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.CACTUS, 15), now, FarmingGrowthRate.NORMAL);
        assertTrue(tracker.poll(Map.of(cactus, checked), EnumSet.of(cactus), now.plusSeconds(99999), true, true).isEmpty());
    }
    @Test public void mismatchedFamilyCannotEmitObservedOrEstimatedAlerts()
    {
        tracker.poll(Map.of(), EnumSet.of(patch), now, true, true);
        for (int raw : new int[] {14, 19, 25, 31})
        {
            GardenPatchObservation cactus = new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.CACTUS, raw), now, FarmingGrowthRate.NORMAL);
            assertTrue(tracker.poll(Map.of(patch, cactus), EnumSet.of(patch), now.plusSeconds(10000), true, true).isEmpty());
        }
    }
    @Test public void unselectedElapsedEstimateDoesNotReplayWhenSelectedLater()
    {
        tracker.poll(observation(9, now), EnumSet.noneOf(GardenPatchDefinition.class), now, true, true);
        tracker.poll(observation(9, now), EnumSet.noneOf(GardenPatchDefinition.class), now.plusSeconds(1200), true, true);
        assertTrue(poll(9, now, now.plusSeconds(1201)).isEmpty());
    }
    @Test public void alertsDefaultToOff()
    {
        com.farmingnavigator.FarmingNavigatorConfig config = new com.farmingnavigator.FarmingNavigatorConfig() {};
        assertFalse(config.gardenObservedAlerts()); assertFalse(config.gardenEstimatedAlerts());
    }
}
