package com.farmingnavigator.farming.field;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Opt-in, session-only deduplication. Restored history cannot create a login alert burst. */
public final class FieldAlertTracker
{
    private final Map<FieldPatchDefinition, FieldPatchState> previous = new EnumMap<>(FieldPatchDefinition.class);
    private final Set<FieldPatchDefinition> estimated = EnumSet.noneOf(FieldPatchDefinition.class);
    private final FieldGrowthTimer timer = new FieldGrowthTimer();
    private boolean initialized;
    public List<String> poll(Map<FieldPatchDefinition, FieldPatchObservation> observations,
            Set<FieldPatchDefinition> selected, Instant now, boolean observedAlerts, boolean estimateAlerts)
    {
        List<String> alerts = new ArrayList<>();
        previous.keySet().retainAll(observations.keySet()); estimated.retainAll(observations.keySet());
        for (Map.Entry<FieldPatchDefinition, FieldPatchObservation> entry : observations.entrySet())
        {
            FieldPatchDefinition patch = entry.getKey(); FieldPatchObservation observation = entry.getValue();
            if (observation.getObservedAt().isAfter(now)) { continue; }
            FieldPatchState state = observation.getState();
            if (state.getCrop().getFamily() != null && state.getCrop().getFamily() != patch.getFamily()) { continue; }
            FieldPatchState old = previous.put(patch, state);
            if (state.getCondition() != FieldPatchCondition.GROWING || old == null
                    || old.getCondition() != FieldPatchCondition.GROWING || old.getCrop() != state.getCrop()
                    || state.getStage() < old.getStage()) { estimated.remove(patch); }
            boolean fresh = Duration.between(observation.getObservedAt(), now).getSeconds() <= 30;
            if (initialized && fresh && selected.contains(patch) && observedAlerts
                    && (old == null || old.getCondition() != state.getCondition()))
            {
                String message = observedMessage(state.getCondition());
                if (message != null) { alerts.add(patch.getLabel() + " - " + state.getCrop().getLabel() + " observed " + message + "."); }
            }
            timer.estimate(observation, now).ifPresent(window -> {
                if (!now.isBefore(window.getLatest()) && estimated.add(patch)
                        && initialized && old != null && selected.contains(patch) && estimateAlerts)
                {
                    alerts.add(patch.getLabel() + " " + state.getCrop().getLabel()
                            + " may be ready to harvest if growth stayed healthy. Visit to confirm; disease may delay growth.");
                }
            });
        }
        initialized = true; return alerts;
    }
    private String observedMessage(FieldPatchCondition condition)
    {
        switch (condition)
        {
            case HARVESTABLE: return "harvestable (quantity unknown)";
            case DISEASED: return "diseased";
            case DEAD: return "dead";
            default: return null;
        }
    }
    public void reset() { previous.clear(); estimated.clear(); initialized = false; }
}
