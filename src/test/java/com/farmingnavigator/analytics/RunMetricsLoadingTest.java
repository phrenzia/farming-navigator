package com.farmingnavigator.analytics;

import java.time.Instant;
import java.time.Duration;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunMetricsLoadingTest
{
    private final Instant now = Instant.parse("2026-10-17T18:00:00Z");

    @Test public void loadingSamplesPreserveXpWhileElapsedTimeAndStopOutcomesRemainVisible()
    {
        RunMetricsTracker tracker = new RunMetricsTracker();
        tracker.start("Field run", 100, now, RunHistoryContext.of(RunHistoryStore.Kind.FIELD, "POTATO", 2));
        tracker.sampleStops(1, 1, 0); tracker.sample(true, 150, now.plusSeconds(10));
        tracker.sample(false, -1, now.plusSeconds(15));
        RunMetricsSnapshot loading = tracker.snapshot(now.plusSeconds(20));
        assertTrue(loading.isRecording()); assertTrue(loading.isXpKnown()); assertEquals(50, loading.getXpGained());
        assertEquals(Duration.ofSeconds(20), loading.getElapsed()); assertEquals(1, loading.getStopOutcomes().getCompleted());
        tracker.sample(true, 200, now.plusSeconds(30)); tracker.finish(200, now.plusSeconds(40), false);
        RunMetricsSnapshot ended = tracker.snapshot(now.plusSeconds(60));
        assertEquals(100, ended.getXpGained()); assertTrue(ended.isXpKnown());
        assertEquals(9000d, ended.getXpPerHour().getAsDouble(), 0.001);
        assertEquals(Duration.ofSeconds(40), ended.getElapsed());
    }

    @Test public void realUnknownOrDecreasingXpStillInvalidatesTheRecordedBaseline()
    {
        for (int bad : new int[]{-1, 99})
        {
            RunMetricsTracker tracker = new RunMetricsTracker(); tracker.start("Herb run", 100, now);
            tracker.sample(false, bad, now.plusSeconds(5)); assertTrue(tracker.snapshot(now.plusSeconds(6)).isXpKnown());
            tracker.sample(true, bad, now.plusSeconds(10)); assertFalse(tracker.snapshot(now.plusSeconds(11)).isXpKnown());
            tracker.sample(true, 200, now.plusSeconds(20)); assertFalse(tracker.snapshot(now.plusSeconds(21)).isXpKnown());
            assertFalse(tracker.snapshot(now.plusSeconds(21)).getXpPerHour().isPresent());
        }
    }

    @Test public void skippedLoadingSamplesCannotShiftTheAcceptedSampleClockOrRestartEndedMetrics()
    {
        RunMetricsTracker tracker = new RunMetricsTracker(); tracker.start("Tree run", 100, now);
        tracker.sample(false, -1, now.plusSeconds(300));
        tracker.sample(true, 150, now.plusSeconds(10)); // no account sample was accepted at the later clock
        assertEquals(50, tracker.snapshot(now.plusSeconds(10)).getXpGained());
        tracker.finish(150, now.plusSeconds(20), false);
        tracker.sample(false, -1, now.plusSeconds(30)); tracker.sample(true, 1000, now.plusSeconds(40));
        assertEquals(50, tracker.snapshot(now.plusSeconds(40)).getXpGained()); assertFalse(tracker.isRecording());
        tracker.clear(); tracker.sample(false, -1, now.plusSeconds(50)); assertNull(tracker.snapshot(now.plusSeconds(60)));
    }
}
