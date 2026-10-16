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
    private long revision;
    public long getRevision() { return revision; }
    private FieldRunPlan plan;
    private FieldRunStep step = FieldRunStep.INACTIVE;
    private Instant enteredAt = Instant.EPOCH, startedAt, finishedAt;
    private FieldCrop curingCrop = FieldCrop.UNKNOWN;
    private boolean protect, compostConfirmed, plantedConfirmed, wateringConfirmed;
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
        if (command == FieldRunCommand.CONFIRM_WATERING)
        {
            if (step != FieldRunStep.WATER) { return false; }
            command = FieldRunCommand.CONFIRM;
        }
        if (command == FieldRunCommand.CONFIRM)
        {
            if (step == FieldRunStep.PREPARE) { enter(FieldRunStep.TRAVEL, now); return true; }
            if (step == FieldRunStep.WATER) { wateringConfirmed = true; completePlanting(now); return true; }
            if (step == FieldRunStep.COMPOST) { compostConfirmed = true; enter(FieldRunStep.PLANT, now); return true; }
            if (step == FieldRunStep.PROTECT) { protectionConfirmed++; finish(false, false, now); return true; }
            if (step == FieldRunStep.KEEP_FLOWER) { finish(true, false, now); return true; }
        }
        if (step != FieldRunStep.PREPARE && command == FieldRunCommand.MARK_COMPLETE) { finish(true, false, now); return true; }
        if (step != FieldRunStep.PREPARE && command == FieldRunCommand.SKIP) { finish(false, true, now); return true; }
        return false;
    }
    public boolean observe(WorldPoint player, Map<FieldPatchDefinition, FieldPatchObservation> observations, Instant now)
    {
        if (!active() || step == FieldRunStep.PREPARE || step == FieldRunStep.PROTECT || step == FieldRunStep.REPREPARE) { return false; }
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
                if (condition == FieldPatchCondition.EMPTY) { preparePlanting(now); }
                else if (condition == FieldPatchCondition.WEEDS && step != FieldRunStep.RAKE) { enter(FieldRunStep.RAKE, now); }
                break;
            case COMPOST:
                if (condition == FieldPatchCondition.GROWING && state.getCrop() != plan.getCrop().getCrop()) { inspect(state, now); }
                break;
            case PLANT:
                if (condition == FieldPatchCondition.GROWING && state.getCrop() == plan.getCrop().getCrop())
                {
                    plantedConfirmed = true;
                    completePlantedCare(state, now);
                }
                break;
            case WATER:
                if (condition == FieldPatchCondition.GROWING && state.getCrop() == plan.getCrop().getCrop())
                {
                    if (state.isWatered()) { wateringConfirmed = true; completePlanting(now); }
                }
                else { inspect(state, now); }
                break;
            case CURE:
                if ((condition == FieldPatchCondition.GROWING || condition == FieldPatchCondition.HARVESTABLE)
                        && state.getCrop() == curingCrop && curingCrop != FieldCrop.UNKNOWN)
                {
                    if (plantedConfirmed) { completePlantedCare(state, now); }
                    else { finish(false, false, now); }
                }
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
            case COMPOST: return condition == FieldPatchCondition.EMPTY || condition == FieldPatchCondition.GROWING;
            case WATER: return condition == FieldPatchCondition.GROWING;
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
            case EMPTY: preparePlanting(now); break;
            case WEEDS: enter(FieldRunStep.RAKE, now); break;
            case HARVESTABLE:
                enter(plan.getCrop().getCrop().getFamily() == FieldFamily.FLOWER && plan.getMode() == FieldRunMode.KEEP_MATURE_FLOWERS
                        ? FieldRunStep.KEEP_FLOWER : FieldRunStep.HARVEST, now); break;
            case DEAD: enter(FieldRunStep.CLEAR, now); break;
            case DISEASED: curingCrop = state.getCrop(); enter(FieldRunStep.CURE, now); break;
            case SCARECROW: if (step != FieldRunStep.SCARECROW) { enter(FieldRunStep.SCARECROW, now); } break;
            case GROWING:
                if (plantedConfirmed && state.getCrop() == plan.getCrop().getCrop())
                {
                    completePlantedCare(state, now);
                }
                else if (step != FieldRunStep.WAIT) { enter(FieldRunStep.WAIT, now); }
                break;
            default: break;
        }
    }
    private void preparePlanting(Instant now)
    {
        if (plantedConfirmed) { enter(FieldRunStep.REPREPARE, now); return; }
        enter(plan.getCompost() == FieldRunCompost.NONE || compostConfirmed ? FieldRunStep.PLANT : FieldRunStep.COMPOST, now);
    }
    private void completePlantedCare(FieldPatchState state, Instant now)
    {
        if (plan.getWatering().appliesTo(plan.getCrop().getCrop().getFamily()) && !wateringConfirmed)
        {
            if (state.isWatered()) { wateringConfirmed = true; }
            else { enter(FieldRunStep.WATER, now); return; }
        }
        completePlanting(now);
    }
    private void completePlanting(Instant now)
    {
        if (protect && plan.getStops().get(index).getPatch() != FieldPatchDefinition.HARMONY_ISLAND_ALLOTMENT) { enter(FieldRunStep.PROTECT, now); }
        else { finish(false, false, now); }
    }
    public void refreshTransports(Map<FieldPatchDefinition, List<VerifiedTransportResult>> routes, HerbTransportPreference preference, RequirementContext context)
    {
        if (active()) { plan = plan.refreshTransports(routes, preference, context); }
    }
    private void finish(boolean manually, boolean skip, Instant now)
    {
        revision++;
        if (skip) { skipped++; } else { completed++; if (manually) { manual++; } }
        curingCrop = FieldCrop.UNKNOWN; compostConfirmed = plantedConfirmed = wateringConfirmed = false; index++;
        if (index >= plan.getStops().size()) { finishedAt = now; enter(FieldRunStep.COMPLETE, now); } else { enter(FieldRunStep.TRAVEL, now); }
    }
    private void enter(FieldRunStep next, Instant now) { if (step != next) { revision++; } step = next; enteredAt = now; }
    private boolean active() { return step != FieldRunStep.INACTIVE && step != FieldRunStep.COMPLETE; }
    public FieldRunSnapshot snapshot(Instant now)
    {
        Instant end = finishedAt == null ? now : finishedAt;
        Duration elapsed = startedAt == null || end.isBefore(startedAt) ? Duration.ZERO : Duration.between(startedAt, end);
        return new FieldRunSnapshot(step, plan == null ? null : plan.getCrop(), plan == null ? null : plan.getMode(),
                active() ? plan.getStops().get(index) : null, plan == null ? 0 : plan.getStops().size(), completed, manual, skipped, protectionConfirmed, elapsed, plan == null ? FieldRunCompost.NONE : plan.getCompost(), plan == null ? HerbRunCure.PLANT_CURE : plan.getCure(), plan == null ? FieldRunWatering.NONE : plan.getWatering(), revision, active() && index + 1 < plan.getStops().size() ? plan.getStops().get(index + 1) : null);
    }
    public void cancel()
    {
        revision++;
        plan = null; step = FieldRunStep.INACTIVE; enteredAt = Instant.EPOCH; startedAt = null; finishedAt = null;
        curingCrop = FieldCrop.UNKNOWN; compostConfirmed = plantedConfirmed = wateringConfirmed = false; index = completed = manual = skipped = protectionConfirmed = 0;
    }
}
