package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunMetricsTrackerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final RunMetricsTracker tracker = new RunMetricsTracker();
    @Test public void actualXpDeltaAndElapsedRateIncludePreparationTime()
    {
        tracker.start("Tree run", 1000, now);
        tracker.sample(11000, now.plusSeconds(600));
        RunMetricsSnapshot result = tracker.snapshot(now.plusSeconds(600));
        assertEquals(10000, result.getXpGained()); assertTrue(result.isXpKnown()); assertTrue(result.isRecording());
        assertEquals(Duration.ofMinutes(10), result.getElapsed());
        assertEquals(60000d, result.getXpPerHour().getAsDouble(), 0.01);
    }
    @Test public void duplicateSamplesDoNotDoubleCountXp()
    {
        tracker.start("Herb run", 1000, now);
        tracker.sample(1500, now.plusSeconds(1)); tracker.sample(1500, now.plusSeconds(2));
        assertEquals(500, tracker.snapshot(now.plusSeconds(2)).getXpGained());
    }
    @Test public void finishFreezesXpAndElapsedEvenWhenLaterSamplesArrive()
    {
        tracker.start("Herb run", 1000, now); tracker.finish(1600, now.plusSeconds(60), true);
        tracker.sample(10000, now.plusSeconds(120));
        RunMetricsSnapshot result = tracker.snapshot(now.plusSeconds(3600));
        assertEquals(600, result.getXpGained()); assertEquals(Duration.ofMinutes(1), result.getElapsed());
        assertFalse(result.isRecording()); assertTrue(result.isCompleted());
    }
    @Test public void missingBaselineStaysUnknownInsteadOfAssumingZeroXp()
    {
        tracker.start("Herb run", -1, now); tracker.sample(1000, now.plusSeconds(30));
        assertFalse(tracker.snapshot(now.plusSeconds(30)).isXpKnown());
        assertFalse(tracker.snapshot(now.plusSeconds(30)).getXpPerHour().isPresent());
    }
    @Test public void decreasingOrUnavailableXpInvalidatesStatisticsUntilNewRun()
    {
        tracker.start("Tree run", 10000, now); tracker.sample(11000, now.plusSeconds(1));
        tracker.sample(5000, now.plusSeconds(2)); tracker.sample(12000, now.plusSeconds(3));
        assertFalse(tracker.snapshot(now.plusSeconds(3)).isXpKnown());
        tracker.start("Tree run", 10000, now); tracker.sample(-1, now.plusSeconds(1));
        assertFalse(tracker.snapshot(now.plusSeconds(1)).isXpKnown());
    }
    @Test public void outOfOrderSamplesCannotReplaceCurrentExperience()
    {
        tracker.start("Herb run", 1000, now); tracker.sample(2000, now.plusSeconds(20));
        tracker.sample(1500, now.plusSeconds(10));
        RunMetricsSnapshot result = tracker.snapshot(now.plusSeconds(20));
        assertTrue(result.isXpKnown()); assertEquals(1000, result.getXpGained());
    }
    @Test public void clockRegressionCannotMakeDurationNegativeOrMoveBehindLatestSample()
    {
        tracker.start("Herb run", 1000, now);
        assertEquals(Duration.ZERO, tracker.snapshot(now.minusSeconds(1)).getElapsed());
        assertFalse(tracker.snapshot(now).getXpPerHour().isPresent());
        tracker.sample(2000, now.plusSeconds(20)); tracker.finish(1500, now.plusSeconds(10), false);
        assertEquals(Duration.ofSeconds(20), tracker.snapshot(now.plusSeconds(30)).getElapsed());
        assertEquals(1000, tracker.snapshot(now.plusSeconds(30)).getXpGained());
    }
    @Test public void stoppingIsDistinctFromCompletingAndCanRecordZeroXp()
    {
        tracker.start("Tree run", 1000, now); tracker.finish(1000, now.plusSeconds(60), false);
        assertFalse(tracker.snapshot(now).isCompleted());
        assertEquals(0d, tracker.snapshot(now).getXpPerHour().getAsDouble(), 0d);
    }
    @Test public void accountBoundaryClearAndNewRunDoNotLeakOldStatistics()
    {
        tracker.start("Tree run", 1000, now); tracker.sample(2000, now.plusSeconds(5)); tracker.clear();
        assertNull(tracker.snapshot(now)); assertFalse(tracker.isRecording());
        tracker.start("Herb run", 100000, now.plusSeconds(10));
        assertEquals(0, tracker.snapshot(now.plusSeconds(10)).getXpGained());
        assertEquals("Herb run", tracker.snapshot(now.plusSeconds(10)).getRunName());
    }
    @Test public void maximumIntExperienceAndHugeDurationDoNotOverflow()
    {
        tracker.start("Tree run", 0, now); tracker.finish(Integer.MAX_VALUE, Instant.MAX, true);
        RunMetricsSnapshot result = tracker.snapshot(Instant.MAX);
        assertEquals(Integer.MAX_VALUE, result.getXpGained());
        assertTrue(Double.isFinite(result.getXpPerHour().getAsDouble()));
    }
}
