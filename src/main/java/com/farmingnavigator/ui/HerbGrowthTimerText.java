package com.farmingnavigator.ui;

import com.farmingnavigator.farming.state.HerbGrowthEstimate;
import com.farmingnavigator.farming.state.HerbGrowthTimer;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.PatchCondition;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class HerbGrowthTimerText
{
    private HerbGrowthTimerText()
    {
    }

    public static String describe(HerbPatchObservation observation, Instant now)
    {
        if (observation == null || observation.getState().getCondition() != PatchCondition.GROWING)
        {
            return "";
        }
        Optional<HerbGrowthEstimate> estimate = new HerbGrowthTimer().estimate(observation, now);
        if (!estimate.isPresent())
        {
            return "Timer: DATA REQUIRED - observe this patch's growth rate.";
        }
        HerbGrowthEstimate window = estimate.get();
        if (!now.isBefore(window.getLatest()))
        {
            return "Expected ready if healthy - visit to confirm.";
        }
        if (!now.isBefore(window.getEarliest()))
        {
            return "May be ready - visit to confirm.";
        }
        long earliestMinutes = minutesUntil(now, window.getEarliest());
        long latestMinutes = minutesUntil(now, window.getLatest());
        return "Estimated harvest in " + earliestMinutes + "-" + latestMinutes + " min if healthy.";
    }

    private static long minutesUntil(Instant now, Instant target)
    {
        long seconds = Duration.between(now, target).getSeconds();
        return Math.max(0, (seconds + 59) / 60);
    }
}
