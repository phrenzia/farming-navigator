package com.farmingnavigator.ui;

import com.farmingnavigator.farming.garden.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class GardenObservationText
{
    private GardenObservationText() { }
    public static String describe(GardenPatchObservation observation, ZoneId zone)
    {
        if (observation == null) { return "Data required - visit this bush/cactus patch."; }
        GardenPatchState state = observation.getState();
        String crop = state.getCrop() == GardenCrop.UNKNOWN ? "" : state.getCrop().getLabel() + " - ";
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone).format(observation.getObservedAt());
        return "Last observed " + time + ": " + crop + state.getCondition().getLabel() + ".";
    }

    public static String timer(GardenPatchObservation observation, Instant now)
    {
        Optional<GardenGrowthWindow> estimate = new GardenGrowthTimer().estimate(observation, now);
        if (!estimate.isPresent()) { return ""; }
        if (!now.isBefore(estimate.get().getLatest()))
        {
            return "Expected health-check ready if healthy - visit to confirm.";
        }
        if (!now.isBefore(estimate.get().getEarliest()))
        {
            return "May be ready to check health if healthy - visit to confirm.";
        }
        return "Estimated health check in " + minutes(now, estimate.get().getEarliest()) + "-"
                + minutes(now, estimate.get().getLatest()) + " min if healthy.";
    }

    private static long minutes(Instant now, Instant target) { return Math.max(0, (Duration.between(now, target).getSeconds() + 59) / 60); }
}
