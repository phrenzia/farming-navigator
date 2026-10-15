package com.farmingnavigator.ui;

import com.farmingnavigator.farming.tree.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class TreeObservationText
{
    private TreeObservationText() { }
    public static String describe(TreePatchObservation observation, ZoneId zone)
    {
        if (observation == null) { return "Data required - visit this tree patch."; }
        TreePatchState state = observation.getState();
        String crop = state.getCrop() == TreeCrop.UNKNOWN ? "" : state.getCrop().getLabel() + " - ";
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone).format(observation.getObservedAt());
        return "Last observed " + time + ": " + crop + state.getCondition().getLabel() + ".";
    }

    public static String timer(TreePatchObservation observation, Instant now)
    {
        Optional<TreeGrowthWindow> estimate = new TreeGrowthTimer().estimate(observation, now);
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
