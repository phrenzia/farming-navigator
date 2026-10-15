package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.HerbGrowthEstimate;
import com.farmingnavigator.farming.state.HerbGrowthTimer;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.PatchCondition;
import com.farmingnavigator.loadout.LoadoutService;
import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;

/** A deterministic suggested circuit, with independent transport alternatives and quantity-aware supplies. */
public final class HerbRunPlanner
{
    public HerbRunPlan plan(AccountState account, List<HerbPatchStatus> patchStatuses,
            Set<HerbPatchDefinition> selected, HerbRunCrop crop, HerbRunCompost compost,
            Map<HerbPatchDefinition, HerbPatchObservation> observations,
            Map<HerbPatchDefinition, List<VerifiedTransportResult>> transports,
            StorageSnapshot storage, Instant now)
    {
        Map<HerbPatchDefinition, String> excluded = new EnumMap<>(HerbPatchDefinition.class);
        List<HerbRunStop> stops = new ArrayList<>();
        if (account == null || !account.isLoggedIn())
        {
            return new HerbRunPlan(crop, stops, Collections.emptyList(), excluded, "Log in to plan a herb run.");
        }
        if (account.getBoostedFarmingLevel() < crop.getRequiredLevel())
        {
            return new HerbRunPlan(crop, stops, Collections.emptyList(), excluded,
                    crop.getDisplayName() + " needs " + crop.getRequiredLevel() + " Farming; current level is "
                            + account.getBoostedFarmingLevel() + ".");
        }
        int replantCount = 0;
        int cureCount = 0;
        for (HerbPatchStatus status : patchStatuses)
        {
            HerbPatchDefinition patch = status.getPatch();
            if (!selected.contains(patch))
            {
                continue;
            }
            if (!status.isUnlocked())
            {
                excluded.put(patch, String.join(", ", status.getMissingRequirements()));
                continue;
            }
            HerbPatchObservation observation = observations.get(patch);
            PatchCondition condition = observation == null ? PatchCondition.UNKNOWN : observation.getState().getCondition();
            if (condition == PatchCondition.GROWING)
            {
                Optional<HerbGrowthEstimate> estimate = new HerbGrowthTimer().estimate(observation, now);
                if (estimate.isPresent() && now.isBefore(estimate.get().getEarliest()))
                {
                    excluded.put(patch, "Last observed growing; estimated harvest window has not started.");
                    continue;
                }
            }
            boolean replant = condition != PatchCondition.DISEASED;
            replantCount += replant ? 1 : 0;
            cureCount += replant ? 0 : 1;
            List<VerifiedTransportResult> usable = transports.getOrDefault(patch, Collections.emptyList()).stream()
                    .filter(VerifiedTransportResult::isUsableForPlannedRun)
                    .sorted(Comparator.comparingInt((VerifiedTransportResult result) ->
                            result.getState() == VerifiedTransportState.READY_NOW ? 0 : 1)
                            .thenComparingInt(result -> result.getOption().getBaselineRank()))
                    .collect(Collectors.toList());
            stops.add(new HerbRunStop(patch, nextAction(condition),
                    usable.isEmpty() ? null : usable.get(0), usable.size() < 2 ? null : usable.get(1), replant));
        }
        List<SupplyRequirement> supplies = new ArrayList<>();
        if (!stops.isEmpty())
        {
            supplies.add(new SupplyRequirement(ItemID.SPADE, "Spade", 1, false, "Harvest or clear a patch."));
            if (replantCount > 0)
            {
                supplies.add(new SupplyRequirement(crop.getSeedId(), crop.getDisplayName() + " seed", replantCount,
                        false, "One seed per potential replant stop; inspect unknown patches first."));
                supplies.add(new SupplyRequirement(ItemID.DIBBER, "Seed dibber", 1, true,
                        "Bring unless barbarian seed planting is unlocked."));
                supplies.add(new SupplyRequirement(ItemID.RAKE, "Rake", 1, true,
                        "Useful if weeds are present; tool leprechaun storage is not checked."));
                if (compost != HerbRunCompost.NONE)
                {
                    supplies.add(new SupplyRequirement(compost.getItemId(), compost.toString(), replantCount, true,
                            "One bucket per potential replant; bottomless bucket and Fertile Soil are not checked."));
                }
            }
            supplies.add(new SupplyRequirement(ItemID.FAIRY_ENCHANTED_SECATEURS, "Magic secateurs", 1, true,
                    "Optional harvest-yield equipment."));
            if (cureCount > 0)
            {
                supplies.add(new SupplyRequirement(ItemID.PLANT_CURE, "Plant cure", cureCount, true,
                        "For observed diseased patches; Cure Plant is an alternative."));
            }
        }
        return new HerbRunPlan(crop, stops, new LoadoutService().evaluate(supplies, storage), excluded, "");
    }

    private String nextAction(PatchCondition condition)
    {
        switch (condition)
        {
            case EMPTY: return "Inspect, then compost and plant.";
            case WEEDS: return "Inspect, then rake, compost and plant.";
            case HARVESTABLE: return "Inspect, then harvest and replant.";
            case DISEASED: return "Inspect and cure the diseased herb.";
            case DEAD: return "Inspect, then clear the dead herb and replant.";
            default: return "Inspect the patch; replant only when it is ready.";
        }
    }
}
