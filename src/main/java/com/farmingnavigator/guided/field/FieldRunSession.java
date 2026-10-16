package com.farmingnavigator.guided.field;

import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/** Manual checklist; only fresh nearby plot evidence advances observed actions. */
public final class FieldRunSession
{
    private FieldRunPlan plan;
    private FieldRunStep step = FieldRunStep.INACTIVE;
    private Instant enteredAt = Instant.EPOCH, startedAt, finishedAt;
    private FieldCrop curingCrop = FieldCrop.UNKNOWN;
    private boolean protect;
    private int index, completed, manual, skipped, protectionConfirmed;
    public boolean start(FieldRunPlan plan, boolean protect, Instant now)
    {
        if (plan == null || !plan.getBlocker().isEmpty() || plan.getStops().isEmpty()) { return false; }
        cancel(); this.plan = plan; this.protect = protect && plan.getCrop().hasFarmerProtection(); startedAt = now;
        enter(FieldRunStep.PREPARE, now); return true;
    }
    public boolean command(FieldRunCommand command, Instant now)
    {
        if (command == FieldRunCommand.CANCEL) { cancel(); return true; }
        if (!active()) { return false; }
        if (command == FieldRunCommand.CONFIRM)
        {
            if (step == FieldRunStep.PREPARE) { enter(FieldRunStep.TRAVEL, now); return true; }
            if (step == FieldRunStep.PROTECT) { protectionConfirmed++; finish(false, false, now); return true; }
            if (step == FieldRunStep.KEEP_FLOWER) { finish(true, false, now); return true; }
        }
        if (step != FieldRunStep.PREPARE && command == FieldRunCommand.MARK_COMPLETE) { finish(true, false, now); return true; }
        if (step != FieldRunStep.PREPARE && command == FieldRunCommand.SKIP) { finish(false, true, now); return true; }
        return false;
    }
    public boolean observe(WorldPoint player, Map<FieldPatchDefinition, FieldPatchObservation> observations, Instant now)
    {
        if (!active() || step == FieldRunStep.PREPARE || step == FieldRunStep.PROTECT) { return false; }
        FieldRunStop stop = plan.getStops().get(index);
        if (!stop.getPatch().isNear(player)) { return false; }
        FieldPatchObservation observation = observations.get(stop.getPatch());
        if (observation == null || observation.getObservedAt().isBefore(enteredAt) || observation.getObservedAt().isAfter(now)
                || Duration.between(observation.getObservedAt(), now).compareTo(Duration.ofSeconds(30)) > 0) { return false; }
        FieldPatchState state = observation.getState(); FieldPatchCondition condition = state.getCondition();
        if (state.getCrop().getFamily() != null && state.getCrop().getFamily() != stop.getPatch().getFamily()) { return false; }
        FieldRunStep before = step;
        if (!stop.isReplant() && (condition == FieldPatchCondition.EMPTY || condition == FieldPatchCondition.WEEDS))
        {
            finish(false, false, now); return true;
        }
        if (condition == FieldPatchCondition.UNKNOWN) { enter(FieldRunStep.INSPECT, now); return step != before; }
        if (step == FieldRunStep.TRAVEL) { enter(FieldRunStep.INSPECT, now); }
        switch (step)
        {
            case INSPECT:
            case WAIT:
            case SCARECROW: inspect(state, now); break;
            case HARVEST:
            case CLEAR:
            case RAKE:
                if (condition == FieldPatchCondition.EMPTY) { enter(FieldRunStep.PLANT, now); }
                else if (condition == FieldPatchCondition.WEEDS && step != FieldRunStep.RAKE) { enter(FieldRunStep.RAKE, now); }
                break;
            case PLANT:
                if (condition == FieldPatchCondition.GROWING && state.getCrop() == plan.getCrop().getCrop())
                {
                    if (protect && stop.getPatch() != FieldPatchDefinition.HARMONY_ISLAND_ALLOTMENT) { enter(FieldRunStep.PROTECT, now); }
                    else { finish(false, false, now); }
                }
                break;
            case CURE:
                if ((condition == FieldPatchCondition.GROWING || condition == FieldPatchCondition.HARVESTABLE)
                        && state.getCrop() == curingCrop && curingCrop != FieldCrop.UNKNOWN) { finish(false, false, now); }
                break;
            default: break;
        }
        if (step == before && !expected(condition)) { inspect(state, now); }
        return step != before;
    }
    private boolean expected(FieldPatchCondition condition)
    {
        switch (step)
        {
            case PLANT: return condition == FieldPatchCondition.EMPTY;
            case HARVEST:
            case KEEP_FLOWER: return condition == FieldPatchCondition.HARVESTABLE;
            case CURE: return condition == FieldPatchCondition.DISEASED;
            case RAKE: return condition == FieldPatchCondition.WEEDS;
            case CLEAR: return condition == FieldPatchCondition.DEAD;
            default: return true;
        }
    }
    private void inspect(FieldPatchState state, Instant now)
    {
        switch (state.getCondition())
        {
            case EMPTY: enter(FieldRunStep.PLANT, now); break;
            case WEEDS: enter(FieldRunStep.RAKE, now); break;
            case HARVESTABLE:
                enter(plan.getCrop().getCrop().getFamily() == FieldFamily.FLOWER && plan.getMode() == FieldRunMode.KEEP_MATURE_FLOWERS
                        ? FieldRunStep.KEEP_FLOWER : FieldRunStep.HARVEST, now); break;
            case DEAD: enter(FieldRunStep.CLEAR, now); break;
            case DISEASED: curingCrop = state.getCrop(); enter(FieldRunStep.CURE, now); break;
            case SCARECROW: if (step != FieldRunStep.SCARECROW) { enter(FieldRunStep.SCARECROW, now); } break;
            case GROWING: if (step != FieldRunStep.WAIT) { enter(FieldRunStep.WAIT, now); } break;
            default: break;
        }
    }
    public void refreshTransports(Map<FieldPatchDefinition, List<VerifiedTransportResult>> routes, HerbTransportPreference preference, RequirementContext context)
    {
        if (active()) { plan = plan.withTransports(routes, preference, context); }
    }
    private void finish(boolean manually, boolean skip, Instant now)
    {
        if (skip) { skipped++; } else { completed++; if (manually) { manual++; } }
        curingCrop = FieldCrop.UNKNOWN; index++;
        if (index >= plan.getStops().size()) { finishedAt = now; enter(FieldRunStep.COMPLETE, now); } else { enter(FieldRunStep.TRAVEL, now); }
    }
    private void enter(FieldRunStep next, Instant now) { step = next; enteredAt = now; }
    private boolean active() { return step != FieldRunStep.INACTIVE && step != FieldRunStep.COMPLETE; }
    public FieldRunSnapshot snapshot(Instant now)
    {
        Instant end = finishedAt == null ? now : finishedAt;
        Duration elapsed = startedAt == null || end.isBefore(startedAt) ? Duration.ZERO : Duration.between(startedAt, end);
        return new FieldRunSnapshot(step, plan == null ? null : plan.getCrop(), plan == null ? null : plan.getMode(),
                active() ? plan.getStops().get(index) : null, plan == null ? 0 : plan.getStops().size(), completed, manual, skipped, protectionConfirmed, elapsed);
    }
    public void cancel()
    {
        plan = null; step = FieldRunStep.INACTIVE; enteredAt = Instant.EPOCH; startedAt = null; finishedAt = null;
        curingCrop = FieldCrop.UNKNOWN; index = completed = manual = skipped = protectionConfirmed = 0;
    }
}
