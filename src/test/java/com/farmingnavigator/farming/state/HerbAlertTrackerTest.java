package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class HerbAlertTrackerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final HerbAlertTracker tracker = new HerbAlertTracker();
    private final HerbPatchDefinition patch = HerbPatchDefinition.FALADOR;

    private Map<HerbPatchDefinition, HerbPatchObservation> observation(int raw, Instant time)
    {
        return Collections.singletonMap(patch, new HerbPatchObservation(new HerbPatchStateDecoder().decode(raw),
                time, FarmingGrowthRate.NORMAL));
    }

    private List<String> poll(int raw, Instant observed, Instant time)
    {
        return tracker.poll(observation(raw, observed), EnumSet.of(patch), time, true, true);
    }

    @Test public void restoredHistoryDoesNotProduceLoginBurst()
    {
        assertTrue(poll(170, now.minusSeconds(600), now).isEmpty());
        assertTrue(poll(170, now, now).isEmpty());
        tracker.reset();
        assertTrue(poll(7, now.minusSeconds(3600), now).isEmpty());
        assertTrue(poll(7, now.minusSeconds(3600), now.plusSeconds(60)).isEmpty());
    }

    @Test public void newlyObservedDiseaseReadyAndDeathAlertOncePerCondition()
    {
        tracker.poll(Collections.emptyMap(), EnumSet.of(patch), now, true, true);
        assertTrue(poll(128, now, now).get(0).contains("observed diseased"));
        assertTrue(poll(129, now.plusSeconds(1), now.plusSeconds(1)).isEmpty());
        assertTrue(poll(170, now.plusSeconds(2), now.plusSeconds(2)).get(0).contains("observed dead"));
        assertTrue(poll(8, now.plusSeconds(3), now.plusSeconds(3)).get(0).contains("ready to harvest"));
    }

    @Test public void estimateAlertsAtUpperBoundOnceWithConditionalLanguage()
    {
        assertTrue(poll(7, now, now).isEmpty());
        assertTrue(poll(7, now, now.plusSeconds(1199)).isEmpty());
        List<String> alerts = poll(7, now, now.plusSeconds(1200));
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).contains("estimated ready if healthy"));
        assertTrue(alerts.get(0).contains("Visit to confirm"));
        assertTrue(poll(7, now, now.plusSeconds(1260)).isEmpty());
    }

    @Test public void diseaseStopsEstimateAndCuredGrowthCanHaveNewWindow()
    {
        poll(7, now, now);
        poll(128, now.plusSeconds(60), now.plusSeconds(60));
        assertTrue(poll(128, now.plusSeconds(60), now.plusSeconds(1300)).isEmpty());
        poll(7, now.plusSeconds(1400), now.plusSeconds(1400));
        assertEquals(1, poll(7, now.plusSeconds(1400), now.plusSeconds(2600)).size());
    }

    @Test public void replantedSameCropCanAlertInNextCycle()
    {
        poll(7, now, now);
        poll(7, now, now.plusSeconds(1200));
        poll(4, now.plusSeconds(1300), now.plusSeconds(1300));
        assertEquals(1, poll(4, now.plusSeconds(1300), now.plusSeconds(6100)).size());
    }

    @Test public void disabledOrDeselectedPatchesDoNotAlertOrQueueLateBursts()
    {
        poll(7, now, now);
        assertTrue(tracker.poll(observation(7, now), EnumSet.of(patch), now.plusSeconds(1200), false, false).isEmpty());
        assertTrue(poll(7, now, now.plusSeconds(1260)).isEmpty());
        assertTrue(tracker.poll(observation(128, now.plusSeconds(1300)), Collections.emptySet(),
                now.plusSeconds(1300), true, true).isEmpty());
        assertTrue(poll(128, now.plusSeconds(1300), now.plusSeconds(1300)).isEmpty());
    }

    @Test public void staleFutureAndUnknownDataDoNotClaimObservedStatus()
    {
        tracker.poll(Collections.emptyMap(), EnumSet.of(patch), now, true, true);
        assertTrue(poll(128, now.minusSeconds(31), now).isEmpty());
        assertTrue(poll(170, now.plusSeconds(1), now).isEmpty());
        assertTrue(poll(110, now, now).isEmpty());
    }

    @Test public void unknownGrowthRateDoesNotProduceEstimate()
    {
        HerbPatchObservation unknown = new HerbPatchObservation(new HerbPatchStateDecoder().decode(7), now);
        Map<HerbPatchDefinition, HerbPatchObservation> values = Collections.singletonMap(patch, unknown);
        tracker.poll(values, EnumSet.of(patch), now, true, true);
        assertTrue(tracker.poll(values, EnumSet.of(patch), now.plusSeconds(9999), true, true).isEmpty());
    }
}
