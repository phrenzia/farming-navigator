package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.loadout.LoadoutService;
import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/** Inspect-first circuit. No inferred health checks, protection purchases, harvests or teleports. */
public final class TreeRunPlanner
{
    public TreeRunPlan plan(AccountState account, Set<TreePatchDefinition> selected, TreeRunCrop crop,
            boolean protect, Map<TreePatchDefinition, TreePatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        List<TreeRunStop> stops = new ArrayList<>();
        List<SupplyRequirement> supplies = new ArrayList<>();
        Map<TreePatchDefinition, String> excluded = new EnumMap<>(TreePatchDefinition.class);
        List<String> warnings = new ArrayList<>();
        String blocker = account == null || !account.isLoggedIn() ? "Log in to plan a tree run."
                : account.getBoostedFarmingLevel() < crop.getRequiredLevel()
                        ? crop + " requires " + crop.getRequiredLevel() + " Farming; current level is " + account.getBoostedFarmingLevel() + "." : "";
        if (!blocker.isEmpty()) { return new TreeRunPlan(crop, stops, List.of(), excluded, warnings, blocker); }
        int replants = 0;
        int diseased = 0;
        for (TreePatchDefinition patch : TreePatchDefinition.values())
        {
            if (!selected.contains(patch) || patch.getFamily() != crop.getCrop().getFamily()) { continue; }
            String access = new TreePatchAccess().missingRequirement(patch, account);
            if (!access.isEmpty()) { excluded.put(patch, access); continue; }
            TreePatchObservation observation = observations.get(patch);
            if (observation != null && (observation.getObservedAt().isAfter(now)
                    || observation.getState().getCrop().getFamily() != null
                    && observation.getState().getCrop().getFamily() != patch.getFamily())) { observation = null; }
            TreePatchCondition condition = observation == null ? TreePatchCondition.UNKNOWN : observation.getState().getCondition();
            if (condition == TreePatchCondition.GROWING)
            {
                Optional<TreeGrowthWindow> window = new TreeGrowthTimer().estimate(observation, now);
                if (window.isPresent() && now.isBefore(window.get().getEarliest()))
                {
                    excluded.put(patch, "Last observed growing; conditional health-check window has not started.");
                    continue;
                }
            }
            boolean replant = condition != TreePatchCondition.DISEASED;
            if (replant) { replants++; } else { diseased++; }
            stops.add(new TreeRunStop(patch, action(condition, patch.getFamily()), replant));
        }
        if (!stops.isEmpty())
        {
            warnings.add("Travel manually; tree teleports and travel supplies are not planned yet. Order follows the patch catalogue.");
            warnings.add("Inspect old observations before acting. Removing a healthy tree is a choice; bring a usable axe or check the farmer's removal fee separately.");
            supplies.add(new SupplyRequirement(ItemID.SPADE, "Spade", 1, false, "Plant finished saplings and clear stumps."));
            if (replants > 0)
            {
                supplies.add(new SupplyRequirement(crop.getSaplingId(), crop + " sapling", replants, false,
                        "One unnoted, fully grown sapling per potential replant. Seeds and seedlings do not count."));
                supplies.add(new SupplyRequirement(ItemID.RAKE, "Rake", 1, true, "For weeds; auto-weed and leprechaun storage are not checked."));
                if (protect)
                {
                    Optional<SupplyRequirement> payment = crop.protectionPayment(replants);
                    if (payment.isPresent()) { supplies.add(payment.get()); }
                    else { warnings.add("Data required: " + crop + " farmer payment is unverified. Confirm and bring protection supplies yourself."); }
                }
                else { warnings.add("Farmer protection is disabled. Disease prevention and compost supplies are not budgeted in this plan."); }
            }
            if (diseased > 0)
            {
                supplies.add(new SupplyRequirement(ItemID.SECATEURS, "Secateurs", 1, false,
                        "Prune observed diseased trees; magic secateurs also work.", ItemID.FAIRY_ENCHANTED_SECATEURS));
            }
        }
        return new TreeRunPlan(crop, stops, new LoadoutService().evaluate(supplies, storage), excluded, warnings, "");
    }
    private String action(TreePatchCondition condition, TreeFamily family)
    {
        switch (condition)
        {
            case DISEASED: return "Inspect and prune disease. Keep the living tree; no replacement reserved.";
            case EMPTY: return "Inspect, then plant the sapling and pay for protection if selected.";
            case WEEDS: return "Inspect, rake, then plant and pay for protection if selected.";
            case DEAD: return "Inspect, clear the dead tree, then replant if desired.";
            case STUMP: return "Inspect, clear the stump, then replant if desired.";
            case CHECK_HEALTH: return "Inspect and check health. Remove and replant only if desired.";
            case HEALTH_CHECKED: return family == TreeFamily.FRUIT_TREE
                    ? "Inspect and pick any available fruit. Keep or remove and replant as desired."
                    : "Inspect the healthy tree. Keep or remove and replant as desired.";
            default: return "Inspect first. Check health if ready; reserve the sapling for a possible replant.";
        }
    }
}
