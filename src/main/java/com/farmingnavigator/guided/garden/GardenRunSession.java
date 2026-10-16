package com.farmingnavigator.guided.garden;

import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/** Client-thread checklist. Observations choose guidance; commands never send game actions. */
public final class GardenRunSession
{
    private long revision;
    public long getRevision() { return revision; }
    private GardenRunPlan plan;
    private GardenRunStep step = GardenRunStep.INACTIVE;
    private Instant enteredAt = Instant.EPOCH, startedAt, finishedAt;
    private GardenCrop curingCrop = GardenCrop.UNKNOWN;
    private boolean protect, compostConfirmed;
    private int index, completed, manual, skipped, protectionConfirmed;
    public boolean start(GardenRunPlan plan, boolean protect, Instant now)
    {
        if (plan == null || !plan.getBlocker().isEmpty() || plan.getStops().isEmpty()) { return false; }
        cancel(); this.plan = plan; this.protect = protect && plan.getCrop().hasFarmerProtection(); startedAt = now;
        enter(GardenRunStep.PREPARE, now); return true;
    }
    public boolean command(GardenRunCommand command, Instant now)
    {
        if (command == GardenRunCommand.CANCEL) { cancel(); return true; }
        if (!active()) { return false; }
        if (command == GardenRunCommand.CONFIRM)
        {
            if (step == GardenRunStep.PREPARE) { enter(GardenRunStep.TRAVEL, now); return true; }
            if (step == GardenRunStep.COMPOST) { compostConfirmed = true; enter(GardenRunStep.PLANT, now); return true; }
            if (step == GardenRunStep.PROTECT) { protectionConfirmed++; finish(false, false, now); return true; }
            if (step == GardenRunStep.HARVEST)
            {
                if (plan.getMode() == GardenRunMode.KEEP_MATURE) { finish(true, false, now); }
                else { enter(GardenRunStep.KEEP_OR_REPLACE, now); }
                return true;
            }
        }
        if (step == GardenRunStep.KEEP_OR_REPLACE)
        {
            if (command == GardenRunCommand.KEEP_PLANT) { finish(true, false, now); return true; }
            if (command == GardenRunCommand.REPLACE_PLANT && plan.getStops().get(index).isReplant())
            {
                enter(GardenRunStep.CLEAR, now); return true;
            }
        }
        if (step != GardenRunStep.PREPARE && command == GardenRunCommand.MARK_COMPLETE) { finish(true, false, now); return true; }
        if (step != GardenRunStep.PREPARE && command == GardenRunCommand.SKIP) { finish(false, true, now); return true; }
        return false;
    }
    public boolean observe(WorldPoint player, Map<GardenPatchDefinition, GardenPatchObservation> observations, Instant now)
    {
        if (!active() || step == GardenRunStep.PREPARE || step == GardenRunStep.PROTECT) { return false; }
        GardenRunStop stop = plan.getStops().get(index);
        if (!stop.getPatch().isNear(player)) { return false; }
        GardenPatchObservation observation = observations.get(stop.getPatch());
        if (observation == null || observation.getObservedAt().isBefore(enteredAt) || observation.getObservedAt().isAfter(now)
                || Duration.between(observation.getObservedAt(), now).compareTo(Duration.ofSeconds(30)) > 0) { return false; }
        GardenPatchState state = observation.getState(); GardenPatchCondition condition = state.getCondition();
        if (state.getCrop().getFamily() != null && state.getCrop().getFamily() != stop.getPatch().getFamily()) { return false; }
        GardenRunStep before = step;
        if (!stop.isReplant() && (condition == GardenPatchCondition.EMPTY || condition == GardenPatchCondition.WEEDS))
        {
            finish(false, false, now); return true;
        }
        if (condition == GardenPatchCondition.UNKNOWN) { enter(GardenRunStep.INSPECT, now); return step != before; }
        if (step == GardenRunStep.TRAVEL) { enter(GardenRunStep.INSPECT, now); }
        switch (step)
        {
            case INSPECT:
            case WAIT: inspect(state, now); break;
            case CHECK_HEALTH:
                if (condition == GardenPatchCondition.HEALTH_CHECKED) { enter(GardenRunStep.HARVEST, now); } break;
            case CLEAR:
            case RAKE:
                if (condition == GardenPatchCondition.EMPTY) { preparePlanting(now); }
                else if (condition == GardenPatchCondition.WEEDS && step != GardenRunStep.RAKE) { enter(GardenRunStep.RAKE, now); }
                break;
            case COMPOST:
                if (condition == GardenPatchCondition.GROWING && state.getCrop() != plan.getCrop().getCrop()) { inspect(state, now); }
                break;
            case PLANT:
                if (condition == GardenPatchCondition.GROWING && state.getCrop() == plan.getCrop().getCrop())
                {
                    if (protect) { enter(GardenRunStep.PROTECT, now); } else { finish(false, false, now); }
                }
                break;
            case CURE:
            case PRUNE:
                if ((condition == GardenPatchCondition.GROWING || condition == GardenPatchCondition.CHECK_HEALTH)
                        && state.getCrop() == curingCrop && curingCrop != GardenCrop.UNKNOWN) { finish(false, false, now); }
                break;
            default: break;
        }
        if (step == before && !expected(condition)) { inspect(state, now); }
        return step != before;
    }
    private boolean expected(GardenPatchCondition condition)
    {
        switch (step)
        {
            case COMPOST: return condition == GardenPatchCondition.EMPTY || condition == GardenPatchCondition.GROWING;
            case PLANT: return condition == GardenPatchCondition.EMPTY;
            case CHECK_HEALTH: return condition == GardenPatchCondition.CHECK_HEALTH;
            case HARVEST:
            case KEEP_OR_REPLACE: return condition == GardenPatchCondition.HEALTH_CHECKED;
            case PRUNE:
            case CURE: return condition == GardenPatchCondition.DISEASED;
            case RAKE: return condition == GardenPatchCondition.WEEDS;
            case CLEAR: return condition == GardenPatchCondition.DEAD || condition == GardenPatchCondition.HEALTH_CHECKED;
            default: return true;
        }
    }
    private void inspect(GardenPatchState state, Instant now)
    {
        switch (state.getCondition())
        {
            case EMPTY: preparePlanting(now); break;
            case WEEDS: enter(GardenRunStep.RAKE, now); break;
            case CHECK_HEALTH: enter(GardenRunStep.CHECK_HEALTH, now); break;
            case HEALTH_CHECKED: enter(GardenRunStep.HARVEST, now); break;
            case DEAD: enter(GardenRunStep.CLEAR, now); break;
            case DISEASED:
                curingCrop = state.getCrop();
                enter(plan.getCure() == HerbRunCure.CURE_PLANT ? GardenRunStep.CURE
                        : state.getCrop().getFamily() == GardenFamily.BUSH ? GardenRunStep.PRUNE : GardenRunStep.CURE, now); break;
            case GROWING: if (step != GardenRunStep.WAIT) { enter(GardenRunStep.WAIT, now); } break;
            default: break;
        }
    }
    private void preparePlanting(Instant now)
    {
        enter(plan.getCompost() == GardenRunCompost.NONE || compostConfirmed ? GardenRunStep.PLANT : GardenRunStep.COMPOST, now);
    }
    public void refreshTransports(Map<GardenPatchDefinition, List<VerifiedTransportResult>> routes, HerbTransportPreference preference, RequirementContext context)
    {
        if (active()) { plan = plan.withTransports(routes, preference, context); }
    }
    private void finish(boolean manually, boolean skip, Instant now)
    {
        revision++;
        if (skip) { skipped++; } else { completed++; if (manually) { manual++; } }
        curingCrop = GardenCrop.UNKNOWN; compostConfirmed = false; index++;
        if (index >= plan.getStops().size()) { finishedAt = now; enter(GardenRunStep.COMPLETE, now); } else { enter(GardenRunStep.TRAVEL, now); }
    }
    private void enter(GardenRunStep next, Instant now) { if (step != next) { revision++; } step = next; enteredAt = now; }
    private boolean active() { return step != GardenRunStep.INACTIVE && step != GardenRunStep.COMPLETE; }
    public GardenRunSnapshot snapshot(Instant now)
    {
        Instant end = finishedAt == null ? now : finishedAt;
        Duration elapsed = startedAt == null || end.isBefore(startedAt) ? Duration.ZERO : Duration.between(startedAt, end);
        return new GardenRunSnapshot(step, plan == null ? null : plan.getCrop(), plan == null ? null : plan.getMode(),
                active() ? plan.getStops().get(index) : null, plan == null ? 0 : plan.getStops().size(), completed, manual, skipped, protectionConfirmed, elapsed, plan == null ? GardenRunCompost.NONE : plan.getCompost(), plan == null ? HerbRunCure.PLANT_CURE : plan.getCure(), revision);
    }
    public void cancel()
    {
        revision++;
        plan = null; step = GardenRunStep.INACTIVE; enteredAt = Instant.EPOCH; startedAt = null; finishedAt = null;
        curingCrop = GardenCrop.UNKNOWN; compostConfirmed = false; index = completed = manual = skipped = protectionConfirmed = 0;
    }
}
