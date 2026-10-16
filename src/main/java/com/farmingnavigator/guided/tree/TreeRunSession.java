package com.farmingnavigator.guided.tree;

import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/** Client-thread checklist only. No menus, input, dialogue inference or game actions. */
public final class TreeRunSession
{
    private long revision;
    public long getRevision() { return revision; }
    private TreeRunPlan plan;
    private TreeRunStep step = TreeRunStep.INACTIVE;
    private Instant enteredAt = Instant.EPOCH;
    private Instant startedAt;
    private Instant finishedAt;
    private TreeCrop pruningCrop = TreeCrop.UNKNOWN;
    private boolean protect, compostConfirmed;
    private int index;
    private int completed;
    private int manual;
    private int skipped;
    private int protectionConfirmed;

    public boolean start(TreeRunPlan plan, boolean protect, Instant now)
    {
        if (plan == null || !plan.getBlocker().isEmpty() || plan.getStops().isEmpty()) { return false; }
        cancel();
        this.plan = plan; this.protect = protect && plan.getCrop().hasFarmerProtection(); startedAt = now;
        enter(TreeRunStep.PREPARE, now); return true;
    }
    public boolean command(TreeRunCommand command, Instant now)
    {
        if (command == TreeRunCommand.CANCEL) { cancel(); return true; }
        if (!active()) { return false; }
        if (command == TreeRunCommand.CONFIRM && step == TreeRunStep.PREPARE)
        {
            enter(TreeRunStep.TRAVEL, now); return true;
        }
        if (command == TreeRunCommand.CONFIRM && step == TreeRunStep.COMPOST)
        {
            compostConfirmed = true; enter(TreeRunStep.PLANT, now); return true;
        }
        if (command == TreeRunCommand.CONFIRM && step == TreeRunStep.PROTECT)
        {
            protectionConfirmed++; finish(false, false, now); return true;
        }
        if (command == TreeRunCommand.KEEP_TREE && step == TreeRunStep.KEEP_OR_REMOVE)
        {
            finish(true, false, now); return true;
        }
        if (command == TreeRunCommand.REMOVE_TREE && step == TreeRunStep.KEEP_OR_REMOVE)
        {
            enter(TreeRunStep.REMOVE, now); return true;
        }
        if (step != TreeRunStep.PREPARE && command == TreeRunCommand.MARK_COMPLETE)
        {
            finish(true, false, now); return true;
        }
        if (step != TreeRunStep.PREPARE && command == TreeRunCommand.SKIP)
        {
            finish(false, true, now); return true;
        }
        return false;
    }
    public boolean observe(WorldPoint player, Map<TreePatchDefinition, TreePatchObservation> observations, Instant now)
    {
        if (!active() || step == TreeRunStep.PREPARE || step == TreeRunStep.PROTECT) { return false; }
        TreeRunStop stop = plan.getStops().get(index);
        if (!TreePatchDestination.isNear(stop.getPatch(), player)) { return false; }
        TreePatchObservation observation = observations.get(stop.getPatch());
        if (observation == null || observation.getObservedAt().isBefore(enteredAt) || observation.getObservedAt().isAfter(now)
                || Duration.between(observation.getObservedAt(), now).compareTo(Duration.ofSeconds(30)) > 0) { return false; }
        TreePatchState state = observation.getState();
        if (state.getCrop().getFamily() != null && state.getCrop().getFamily() != stop.getPatch().getFamily()) { return false; }
        TreePatchCondition condition = state.getCondition();
        TreeRunStep before = step;
        if (!stop.isReplant() && (condition == TreePatchCondition.EMPTY || condition == TreePatchCondition.WEEDS))
        {
            finish(false, false, now); return true;
        }
        if (condition == TreePatchCondition.UNKNOWN)
        {
            if (step != TreeRunStep.INSPECT) { enter(TreeRunStep.INSPECT, now); }
            return step != before;
        }
        if (step == TreeRunStep.TRAVEL) { enter(TreeRunStep.INSPECT, now); }
        switch (step)
        {
            case INSPECT:
            case WAIT:
                inspect(state, now); break;
            case CHECK_HEALTH:
                if (condition == TreePatchCondition.HEALTH_CHECKED) { enter(TreeRunStep.KEEP_OR_REMOVE, now); }
                else if (condition == TreePatchCondition.HARVESTABLE) { enter(TreeRunStep.HARVEST_BARK, now); }
                else if (condition == TreePatchCondition.HARVESTED) { enter(TreeRunStep.KEEP_OR_REMOVE, now); }
                break;
            case HARVEST_BARK:
                if (condition == TreePatchCondition.HARVESTED) { enter(TreeRunStep.KEEP_OR_REMOVE, now); }
                break;
            case REMOVE:
            case CLEAR:
            case RAKE:
                if (condition == TreePatchCondition.EMPTY)
                {
                    if (step == TreeRunStep.REMOVE && !stop.isReplant()) { finish(false, false, now); }
                    else { preparePlanting(now); }
                }
                else if (condition == TreePatchCondition.WEEDS && step != TreeRunStep.RAKE) { enter(TreeRunStep.RAKE, now); }
                else if (condition == TreePatchCondition.STUMP && step == TreeRunStep.REMOVE) { enter(TreeRunStep.CLEAR, now); }
                break;
            case COMPOST:
                if (condition == TreePatchCondition.GROWING && state.getCrop() != plan.getCrop().getCrop()) { inspect(state, now); }
                break;
            case PLANT:
                if (condition == TreePatchCondition.GROWING && state.getCrop() == plan.getCrop().getCrop())
                {
                    if (protect) { enter(TreeRunStep.PROTECT, now); }
                    else { finish(false, false, now); }
                }
                break;
            case CURE:
            case PRUNE:
                if ((condition == TreePatchCondition.GROWING || condition == TreePatchCondition.CHECK_HEALTH)
                        && state.getCrop() == pruningCrop && pruningCrop != TreeCrop.UNKNOWN) { finish(false, false, now); }
                break;
            default: break;
        }
        if (step == before && !expectedCondition(condition)) { inspect(state, now); }
        return step != before;
    }
    private boolean expectedCondition(TreePatchCondition condition)
    {
        switch (step)
        {
            case COMPOST: return condition == TreePatchCondition.EMPTY || condition == TreePatchCondition.GROWING;
            case PLANT: return condition == TreePatchCondition.EMPTY;
            case CHECK_HEALTH: return condition == TreePatchCondition.CHECK_HEALTH;
            case HARVEST_BARK: return condition == TreePatchCondition.HARVESTABLE;
            case CURE:
            case PRUNE: return condition == TreePatchCondition.DISEASED;
            case RAKE: return condition == TreePatchCondition.WEEDS;
            case CLEAR: return condition == TreePatchCondition.DEAD || condition == TreePatchCondition.STUMP;
            case REMOVE:
            case KEEP_OR_REMOVE: return condition == TreePatchCondition.HEALTH_CHECKED || condition == TreePatchCondition.HARVESTED;
            default: return true;
        }
    }
    private void inspect(TreePatchState state, Instant now)
    {
        switch (state.getCondition())
        {
            case EMPTY: preparePlanting(now); break;
            case WEEDS: enter(TreeRunStep.RAKE, now); break;
            case CHECK_HEALTH: enter(TreeRunStep.CHECK_HEALTH, now); break;
            case HEALTH_CHECKED: enter(TreeRunStep.KEEP_OR_REMOVE, now); break;
            case HARVESTABLE: enter(TreeRunStep.HARVEST_BARK, now); break;
            case HARVESTED: enter(TreeRunStep.KEEP_OR_REMOVE, now); break;
            case STUMP:
            case DEAD: enter(TreeRunStep.CLEAR, now); break;
            case DISEASED: pruningCrop = state.getCrop(); enter(plan.getCure() == TreeRunCure.CURE_PLANT ? TreeRunStep.CURE : TreeRunStep.PRUNE, now); break;
            case GROWING:
                if (step != TreeRunStep.WAIT) { enter(TreeRunStep.WAIT, now); } break;
            default: break;
        }
    }
    private void preparePlanting(Instant now)
    {
        enter(plan.getCompost() == TreeRunCompost.NONE || compostConfirmed ? TreeRunStep.PLANT : TreeRunStep.COMPOST, now);
    }
    public void refreshTransports(Map<TreePatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context)
    {
        if (active()) { plan = plan.withTransports(transports, preference, context); }
    }
    private void finish(boolean manually, boolean skip, Instant now)
    {
        revision++;
        if (skip) { skipped++; } else { completed++; if (manually) { manual++; } }
        pruningCrop = TreeCrop.UNKNOWN;
        compostConfirmed = false;
        index++;
        if (index >= plan.getStops().size()) { finishedAt = now; enter(TreeRunStep.COMPLETE, now); }
        else { enter(TreeRunStep.TRAVEL, now); }
    }
    private void enter(TreeRunStep next, Instant now) { if (step != next) { revision++; } step = next; enteredAt = now; }
    private boolean active() { return step != TreeRunStep.INACTIVE && step != TreeRunStep.COMPLETE; }
    public TreeRunSnapshot snapshot(Instant now)
    {
        Instant end = finishedAt == null ? now : finishedAt;
        Duration elapsed = startedAt == null || end.isBefore(startedAt) ? Duration.ZERO : Duration.between(startedAt, end);
        return new TreeRunSnapshot(step, plan == null ? null : plan.getCrop(), active() ? plan.getStops().get(index) : null,
                plan == null ? 0 : plan.getStops().size(), completed, manual, skipped, protectionConfirmed, elapsed, plan == null ? TreeRunCompost.NONE : plan.getCompost(), plan == null ? TreeRunCure.PRUNE : plan.getCure(), revision, active() && index + 1 < plan.getStops().size() ? plan.getStops().get(index + 1) : null);
    }
    public void cancel()
    {
        revision++;
        compostConfirmed = false;
        plan = null; step = TreeRunStep.INACTIVE; enteredAt = Instant.EPOCH; startedAt = null; finishedAt = null;
        index = completed = manual = skipped = protectionConfirmed = 0; pruningCrop = TreeCrop.UNKNOWN;
    }
}
