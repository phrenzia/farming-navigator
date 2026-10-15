package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Session alert deduplication. Estimates never become observed-ready claims. */
public final class HerbAlertTracker
{
    private final Map<HerbPatchDefinition, HerbPatchState> previous = new EnumMap<>(HerbPatchDefinition.class);
    private final Set<HerbPatchDefinition> estimated = java.util.EnumSet.noneOf(HerbPatchDefinition.class);
    private final HerbGrowthTimer timer = new HerbGrowthTimer();
    private boolean initialized;

    public List<String> poll(Map<HerbPatchDefinition, HerbPatchObservation> observations,
            Set<HerbPatchDefinition> selected, Instant now, boolean observedAlerts, boolean estimateAlerts)
    {
        List<String> alerts = new ArrayList<>();
        previous.keySet().retainAll(observations.keySet());
        estimated.retainAll(observations.keySet());
        for (Map.Entry<HerbPatchDefinition, HerbPatchObservation> entry : observations.entrySet())
        {
            HerbPatchDefinition patch = entry.getKey();
            HerbPatchObservation observation = entry.getValue();
            HerbPatchState state = observation.getState();
            HerbPatchState old = previous.put(patch, state);
            if (state.getCondition() != PatchCondition.GROWING || old == null
                    || old.getCondition() != PatchCondition.GROWING || old.getCrop() != state.getCrop()
                    || state.getStage() < old.getStage())
            {
                estimated.remove(patch);
            }
            boolean fresh = !observation.getObservedAt().isAfter(now)
                    && Duration.between(observation.getObservedAt(), now).getSeconds() <= 30;
            if (initialized && fresh && selected.contains(patch) && observedAlerts
                    && (old == null || old.getCondition() != state.getCondition()))
            {
                String condition = observedMessage(state.getCondition());
                if (condition != null)
                {
                    alerts.add(patch.getDisplayName() + " herb patch observed " + condition + ".");
                }
            }
            timer.estimate(observation, now).ifPresent(estimate -> {
                if (!now.isBefore(estimate.getLatest()) && estimated.add(patch)
                        && initialized && old != null && selected.contains(patch) && estimateAlerts)
                {
                    alerts.add(patch.getDisplayName()
                            + " herbs are estimated ready if healthy. Visit to confirm; disease may delay growth.");
                }
            });
        }
        initialized = true;
        return alerts;
    }

    private static String observedMessage(PatchCondition condition)
    {
        switch (condition)
        {
            case HARVESTABLE: return "ready to harvest";
            case DISEASED: return "diseased";
            case DEAD: return "dead";
            default: return null;
        }
    }

    public void reset()
    {
        previous.clear();
        estimated.clear();
        initialized = false;
    }
}
