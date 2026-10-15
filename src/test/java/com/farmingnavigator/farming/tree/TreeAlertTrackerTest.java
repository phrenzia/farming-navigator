package com.farmingnavigator.farming.tree;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeAlertTrackerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final TreeAlertTracker tracker = new TreeAlertTracker();
    private final TreePatchDefinition patch = TreePatchDefinition.VARROCK_TREE;
    private Map<TreePatchDefinition, TreePatchObservation> observation(int raw, Instant at)
    {
        return Map.of(patch, new TreePatchObservation(new TreePatchStateDecoder().decode(TreeFamily.TREE, raw), at, FarmingGrowthRate.NORMAL));
    }
    private List<String> poll(int raw, Instant observed, Instant time)
    {
        return tracker.poll(observation(raw, observed), EnumSet.of(patch), time, true, true);
    }
    @Test public void restoredHistoryNeverCreatesLoginBurst()
    {
        assertTrue(poll(12, now.minusSeconds(10), now).isEmpty());
        assertTrue(poll(12, now, now).isEmpty());
        tracker.reset(); assertTrue(poll(11, now.minusSeconds(7200), now).isEmpty());
        assertTrue(poll(11, now.minusSeconds(7200), now.plusSeconds(60)).isEmpty());
    }
    @Test public void freshDiseaseDeathAndHealthCheckReadyNotifyOncePerCondition()
    {
        tracker.poll(Map.of(), EnumSet.of(patch), now, true, true);
        assertTrue(poll(73, now, now).get(0).contains("observed diseased"));
        assertTrue(poll(74, now.plusSeconds(1), now.plusSeconds(1)).isEmpty());
        assertTrue(poll(137, now.plusSeconds(2), now.plusSeconds(2)).get(0).contains("observed dead"));
        assertTrue(poll(12, now.plusSeconds(3), now.plusSeconds(3)).get(0).contains("ready to check health"));
        assertTrue(poll(13, now.plusSeconds(4), now.plusSeconds(4)).isEmpty());
    }
    @Test public void estimateFiresAtUpperWindowBoundaryOnceWithConditionalLanguage()
    {
        poll(11, now, now);
        assertTrue(poll(11, now, now.plusSeconds(2399)).isEmpty());
        List<String> alerts = poll(11, now, now.plusSeconds(2400));
        assertEquals(1, alerts.size()); assertTrue(alerts.get(0).contains("may be ready to check health if growth stayed healthy"));
        assertTrue(alerts.get(0).contains("Visit to confirm"));
        assertTrue(poll(11, now, now.plusSeconds(2500)).isEmpty());
    }
    @Test public void disabledUnselectedStaleAndFutureStatesNeverNotify()
    {
        tracker.poll(Map.of(), EnumSet.of(patch), now, true, true);
        assertTrue(tracker.poll(observation(12, now), EnumSet.of(patch), now, false, false).isEmpty());
        assertTrue(tracker.poll(observation(73, now), EnumSet.noneOf(TreePatchDefinition.class), now, true, true).isEmpty());
        assertTrue(poll(137, now.minusSeconds(31), now).isEmpty());
        assertTrue(poll(12, now.plusSeconds(1), now).isEmpty());
    }
    @Test public void turningOnEstimateAlertsDoesNotReplayAlreadyElapsedWindows()
    {
        tracker.poll(observation(11, now), EnumSet.of(patch), now, false, false);
        tracker.poll(observation(11, now), EnumSet.of(patch), now.plusSeconds(2400), false, false);
        assertTrue(poll(11, now, now.plusSeconds(2401)).isEmpty());
    }
    @Test public void diseaseAndUnknownGrowthRatePreventEstimatedReadiness()
    {
        poll(11, now, now); poll(73, now.plusSeconds(1), now.plusSeconds(1));
        assertTrue(poll(73, now.plusSeconds(1), now.plusSeconds(10000)).isEmpty());
        TreePatchObservation unknownRate = new TreePatchObservation(new TreePatchStateDecoder().decode(TreeFamily.TREE, 11), now, FarmingGrowthRate.UNKNOWN);
        assertTrue(tracker.poll(Map.of(patch, unknownRate), EnumSet.of(patch), now.plusSeconds(10000), true, true).isEmpty());
    }
    @Test public void newPlantingCanProduceNewEstimateAndProfileResetSuppressesRestoredEstimate()
    {
        poll(11, now, now); poll(11, now, now.plusSeconds(2400));
        poll(0, now.plusSeconds(2500), now.plusSeconds(2500));
        Instant planted = now.plusSeconds(2600); poll(11, planted, planted);
        assertEquals(1, poll(11, planted, planted.plusSeconds(2400)).size());
        tracker.reset(); assertTrue(poll(11, planted, planted.plusSeconds(2401)).isEmpty());
    }
    @Test public void healthCheckedFruitNeverClaimsFruitAvailable()
    {
        tracker.poll(Map.of(), EnumSet.of(TreePatchDefinition.CATHERBY_FRUIT), now, true, true);
        TreePatchObservation checked = new TreePatchObservation(new TreePatchStateDecoder().decode(TreeFamily.FRUIT_TREE, 14), now, FarmingGrowthRate.NORMAL);
        assertTrue(tracker.poll(Map.of(TreePatchDefinition.CATHERBY_FRUIT, checked), EnumSet.of(TreePatchDefinition.CATHERBY_FRUIT), now, true, true).isEmpty());
    }
}
