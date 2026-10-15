package com.farmingnavigator.ui;

import com.farmingnavigator.farming.state.HerbCrop;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchState;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Describes observations as historical facts, rather than live readiness guarantees. */
public final class HerbPatchObservationText
{
    private HerbPatchObservationText()
    {
    }

    public static String describe(HerbPatchObservation observation, ZoneId zone)
    {
        if (observation == null)
        {
            return "Patch state: DATA REQUIRED - visit this patch to observe it.";
        }
        String time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zone)
                .format(observation.getObservedAt());
        HerbPatchState state = observation.getState();
        if (!state.isKnown())
        {
            return "Last observed " + time + ": DATA REQUIRED - unrecognized patch state.";
        }
        String crop = state.getCrop() == HerbCrop.UNKNOWN ? "" : state.getCrop().getDisplayName() + " - ";
        return "Last observed " + time + ": " + crop + state.getCondition().getDisplayName() + ".";
    }
}
