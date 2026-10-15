package com.farmingnavigator.farming.tree;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Opt-in, session-only deduplication. Restored history cannot create a login alert burst. */
public final class TreeAlertTracker
{
    private final Map<TreePatchDefinition, TreePatchState> previous = new EnumMap<>(TreePatchDefinition.class);
    private final Set<TreePatchDefinition> estimated = EnumSet.noneOf(TreePatchDefinition.class);
    private final TreeGrowthTimer timer = new TreeGrowthTimer();
    private boolean initialized;
    public List<String> poll(Map<TreePatchDefinition, TreePatchObservation> observations,
            Set<TreePatchDefinition> selected, Instant now, boolean observedAlerts, boolean estimateAlerts)
    {
        List<String> alerts = new ArrayList<>();
        previous.keySet().retainAll(observations.keySet()); estimated.retainAll(observations.keySet());
        for (Map.Entry<TreePatchDefinition, TreePatchObservation> entry : observations.entrySet())
        {
            TreePatchDefinition patch = entry.getKey(); TreePatchObservation observation = entry.getValue();
            if (observation.getObservedAt().isAfter(now)) { continue; }
            TreePatchState state = observation.getState();
            if (state.getCrop().getFamily() != null && state.getCrop().getFamily() != patch.getFamily()) { continue; }
            TreePatchState old = previous.put(patch, state);
            if (state.getCondition() != TreePatchCondition.GROWING || old == null
                    || old.getCondition() != TreePatchCondition.GROWING || old.getCrop() != state.getCrop()
                    || state.getStage() < old.getStage()) { estimated.remove(patch); }
            boolean fresh = Duration.between(observation.getObservedAt(), now).getSeconds() <= 30;
            if (initialized && fresh && selected.contains(patch) && observedAlerts
                    && (old == null || old.getCondition() != state.getCondition()))
            {
                String message = observedMessage(state.getCondition());
                if (message != null) { alerts.add(patch.getLabel() + " "
                        + patch.getFamily().getPatchLabel()
                        + " patch observed " + message + "."); }
            }
            timer.estimate(observation, now).ifPresent(window -> {
                if (!now.isBefore(window.getLatest()) && estimated.add(patch)
                        && initialized && old != null && selected.contains(patch) && estimateAlerts)
                {
                    alerts.add(patch.getLabel() + " " + state.getCrop().getLabel()
                            + " may be ready to check health if growth stayed healthy. Visit to confirm; disease may delay growth.");
                }
            });
        }
        initialized = true; return alerts;
    }
    private String observedMessage(TreePatchCondition condition)
    {
        switch (condition)
        {
            case CHECK_HEALTH: return "ready to check health";
            case DISEASED: return "diseased";
            case DEAD: return "dead";
            default: return null;
        }
    }
    public void reset() { previous.clear(); estimated.clear(); initialized = false; }
}
