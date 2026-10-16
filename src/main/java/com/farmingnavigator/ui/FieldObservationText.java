package com.farmingnavigator.ui;

import com.farmingnavigator.farming.field.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class FieldObservationText
{
    private FieldObservationText() { }
    public static String describe(FieldPatchObservation observation, ZoneId zone)
    {
        if (observation == null) { return "Data required - visit this allotment/flower patch."; }
        FieldPatchState state = observation.getState();
        String crop = state.getCrop() == FieldCrop.UNKNOWN ? "" : state.getCrop().getLabel() + " - ";
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone).format(observation.getObservedAt());
        return "Last observed " + time + ": " + crop + state.getCondition().getLabel() + ".";
    }

    public static String timer(FieldPatchObservation observation, Instant now)
    {
        Optional<FieldGrowthWindow> estimate = new FieldGrowthTimer().estimate(observation, now);
        if (!estimate.isPresent()) { return ""; }
        if (!now.isBefore(estimate.get().getLatest()))
        {
            return "Expected to be harvestable if healthy - visit to confirm.";
        }
        if (!now.isBefore(estimate.get().getEarliest()))
        {
            return "May be ready to harvest if healthy - visit to confirm.";
        }
        return "Estimated harvest in " + minutes(now, estimate.get().getEarliest()) + "-"
                + minutes(now, estimate.get().getLatest()) + " min if healthy.";
    }

    private static long minutes(Instant now, Instant target) { return Math.max(0, (Duration.between(now, target).getSeconds() + 59) / 60); }
}
