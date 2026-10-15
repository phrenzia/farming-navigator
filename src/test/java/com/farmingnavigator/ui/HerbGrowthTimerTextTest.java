package com.farmingnavigator.ui;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchStateDecoder;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HerbGrowthTimerTextTest
{
    private final Instant observedAt = Instant.parse("2026-10-15T12:00:00Z");

    @Test
    public void countdownIsAnEstimateConditionalOnHealthyGrowth()
    {
        HerbPatchObservation growing = observation(32, FarmingGrowthRate.NORMAL);
        assertEquals("Estimated harvest in 60-80 min if healthy.", HerbGrowthTimerText.describe(growing, observedAt));
        assertEquals("Estimated harvest in 1-21 min if healthy.", HerbGrowthTimerText.describe(growing, observedAt.plusSeconds(59 * 60)));
        assertEquals("May be ready - visit to confirm.", HerbGrowthTimerText.describe(growing, observedAt.plusSeconds(60 * 60)));
        assertEquals("Expected ready if healthy - visit to confirm.", HerbGrowthTimerText.describe(growing, observedAt.plusSeconds(80 * 60)));
    }

    @Test
    public void diseaseDeadHarvestedEmptyAndUnknownHaveNoGrowingCountdown()
    {
        for (int raw : new int[] {140, 170, 36, 0, 220})
        {
            assertEquals("", HerbGrowthTimerText.describe(observation(raw, FarmingGrowthRate.NORMAL), observedAt));
        }
        assertEquals("", HerbGrowthTimerText.describe(null, observedAt));
    }

    @Test
    public void unknownGrowthRateExplainsMissingTimer()
    {
        assertEquals("Timer: DATA REQUIRED - observe this patch's growth rate.",
                HerbGrowthTimerText.describe(observation(32, FarmingGrowthRate.UNKNOWN), observedAt));
    }

    private HerbPatchObservation observation(int raw, FarmingGrowthRate rate)
    {
        return new HerbPatchObservation(new HerbPatchStateDecoder().decode(raw), observedAt, rate);
    }
}
