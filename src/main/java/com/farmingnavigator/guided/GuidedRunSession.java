package com.farmingnavigator.guided;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.state.HerbCrop;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchState;
import com.farmingnavigator.farming.state.PatchCondition;
import com.farmingnavigator.routing.run.HerbRunCompost;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbRunStop;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.routing.run.HerbTransportPreference;
import com.farmingnavigator.routing.run.HerbTransportSelector;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/** Client-thread checklist state only. It never performs or sends a gameplay action. */
public final class GuidedRunSession
{
    private List<HerbRunStop> stops = Collections.emptyList();
    private HerbRunCrop crop;
    private HerbRunCompost compost;
    private GuidedRunStep step = GuidedRunStep.INACTIVE;
    private Instant enteredAt = Instant.EPOCH;
    private Instant interruptedHarvestAt = Instant.EPOCH;
    private HerbCrop curingCrop = HerbCrop.UNKNOWN;
    private int index;
    private int completed;
    private int skipped;
    private int manuallyCompleted;

    public boolean start(HerbRunPlan plan, HerbRunCompost compost, Instant now)
    {
        if (plan == null || !plan.getBlocker().isEmpty() || plan.getStops().isEmpty())
        {
            return false;
        }
        cancel();
        stops = new ArrayList<>(plan.getStops());
        crop = plan.getCrop();
        this.compost = compost;
        enter(GuidedRunStep.PREPARE, now);
        return true;
    }

    public boolean confirm(Instant now)
    {
        if (step == GuidedRunStep.PREPARE)
        {
            enter(GuidedRunStep.TRAVEL, now);
            return true;
        }
        if (step == GuidedRunStep.COMPOST)
        {
            enter(GuidedRunStep.PLANT, now);
            return true;
        }
        return false;
    }

    public boolean observe(WorldPoint player, Map<HerbPatchDefinition, HerbPatchObservation> observations, Instant now)
    {
        if (!snapshot().isActive() || step == GuidedRunStep.PREPARE)
        {
            return false;
        }
        HerbRunStop stop = stops.get(index);
        if (!HerbPatchDestination.isNear(stop.getPatch(), player))
        {
            return false;
        }
        HerbPatchObservation observation = observations.get(stop.getPatch());
        if (observation == null || observation.getObservedAt().isAfter(now))
        {
            return false;
        }
        // A recent observation may select arrival guidance; action completion needs a new observation.
        Instant earliest = step == GuidedRunStep.TRAVEL ? enteredAt.minusSeconds(30) : enteredAt;
        if (observation.getObservedAt().isBefore(earliest))
        {
            return false;
        }
        HerbPatchState state = observation.getState();
        if (!state.isKnown() && step != GuidedRunStep.INSPECT)
        {
            enter(GuidedRunStep.INSPECT, now);
            return true;
        }
        if (step == GuidedRunStep.TRAVEL || step == GuidedRunStep.INSPECT || step == GuidedRunStep.WAIT)
        {
            GuidedRunStep next = stepFor(state.getCondition());
            if (next != step)
            {
                curingCrop = state.getCrop();
                enter(next, now);
                return true;
            }
            return false;
        }
        if ((step == GuidedRunStep.RAKE || step == GuidedRunStep.HARVEST || step == GuidedRunStep.CLEAR
                || step == GuidedRunStep.NOTE_PRODUCE)
                && state.getCondition() == PatchCondition.EMPTY)
        {
            enter(compost == HerbRunCompost.NONE ? GuidedRunStep.PLANT : GuidedRunStep.COMPOST, now);
            return true;
        }
        if ((step == GuidedRunStep.PLANT || step == GuidedRunStep.COMPOST || step == GuidedRunStep.HARVEST
                || step == GuidedRunStep.CLEAR || step == GuidedRunStep.RAKE)
                && state.getCondition() == PatchCondition.GROWING && state.getCrop().name().equals(crop.name()))
        {
            finishStop(false, false, now);
            return true;
        }
        if (step == GuidedRunStep.CURE && state.getCondition() == PatchCondition.GROWING && state.getCrop() == curingCrop)
        {
            finishStop(false, false, now);
            return true;
        }
        return false;
    }

    public boolean skip(Instant now)
    {
        if (!snapshot().isActive() || step == GuidedRunStep.PREPARE)
        {
            return false;
        }
        finishStop(true, false, now);
        return true;
    }

    public boolean observeInventory(int freeSlots, WorldPoint player, Instant now)
    {
        if (!snapshot().isActive() || !HerbPatchDestination.isNear(stops.get(index).getPatch(), player))
        {
            return false;
        }
        if (step == GuidedRunStep.HARVEST && freeSlots == 0)
        {
            interruptedHarvestAt = enteredAt;
            enter(GuidedRunStep.NOTE_PRODUCE, now);
            return true;
        }
        if (step == GuidedRunStep.NOTE_PRODUCE && freeSlots > 0)
        {
            // This resumes the same harvest action, so preserve its evidence cutoff.
            step = GuidedRunStep.HARVEST;
            enteredAt = interruptedHarvestAt;
            return true;
        }
        return false;
    }

    public boolean markComplete(Instant now)
    {
        if (!snapshot().isActive() || step == GuidedRunStep.PREPARE)
        {
            return false;
        }
        finishStop(false, true, now);
        return true;
    }

    public void refreshTransports(Map<HerbPatchDefinition, List<VerifiedTransportResult>> results)
    {
        refreshTransports(results, HerbTransportPreference.CATALOGUE);
    }

    public void refreshTransports(Map<HerbPatchDefinition, List<VerifiedTransportResult>> results,
            HerbTransportPreference preference)
    {
        for (int i = index; i < stops.size(); i++)
        {
            HerbRunStop stop = stops.get(i);
            List<VerifiedTransportResult> usable = HerbTransportSelector.select(
                    results.getOrDefault(stop.getPatch(), Collections.emptyList()), preference);
            stops.set(i, stop.withTransports(usable.isEmpty() ? null : usable.get(0), usable.size() < 2 ? null : usable.get(1)));
        }
    }

    public GuidedRunSnapshot snapshot()
    {
        return new GuidedRunSnapshot(step, crop, index < stops.size() ? stops.get(index) : null,
                stops.size(), completed, skipped, manuallyCompleted, compost);
    }

    public void cancel()
    {
        stops = Collections.emptyList();
        crop = null;
        step = GuidedRunStep.INACTIVE;
        index = completed = skipped = manuallyCompleted = 0;
    }

    private void finishStop(boolean skip, boolean manual, Instant now)
    {
        if (skip) { skipped++; } else { completed++; }
        if (manual) { manuallyCompleted++; }
        index++;
        enter(index == stops.size() ? GuidedRunStep.COMPLETE : GuidedRunStep.TRAVEL, now);
    }

    private void enter(GuidedRunStep next, Instant now)
    {
        step = next;
        enteredAt = now;
    }

    private GuidedRunStep stepFor(PatchCondition condition)
    {
        switch (condition)
        {
            case WEEDS: return GuidedRunStep.RAKE;
            case HARVESTABLE: return GuidedRunStep.HARVEST;
            case DEAD: return GuidedRunStep.CLEAR;
            case DISEASED: return GuidedRunStep.CURE;
            case GROWING: return GuidedRunStep.WAIT;
            case EMPTY: return compost == HerbRunCompost.NONE ? GuidedRunStep.PLANT : GuidedRunStep.COMPOST;
            default: return GuidedRunStep.INSPECT;
        }
    }
}
