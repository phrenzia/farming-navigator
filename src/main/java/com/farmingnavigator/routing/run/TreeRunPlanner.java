package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.loadout.LoadoutService;
import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.loadout.SupplyCheck;
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
        return plan(account, selected, crop, protect, TreeRunCompost.NONE, TreeRunCure.PRUNE, observations, storage, now);
    }
    public TreeRunPlan plan(AccountState account, Set<TreePatchDefinition> selected, TreeRunCrop crop,
            boolean protect, TreeRunCompost compost, TreeRunCure cure, Map<TreePatchDefinition, TreePatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        return plan(account, selected, crop, protect, compost, cure, observations, storage, now, RunVisitSelection.ALL_SELECTED);
    }
    public TreeRunPlan plan(AccountState account, Set<TreePatchDefinition> selected, TreeRunCrop crop,
            boolean protect, TreeRunCompost compost, TreeRunCure cure, Map<TreePatchDefinition, TreePatchObservation> observations, StorageSnapshot storage, Instant now,
            RunVisitSelection visitSelection)
    {
        List<TreeRunStop> stops = new ArrayList<>();
        List<SupplyRequirement> supplies = new ArrayList<>();
        Map<TreePatchDefinition, String> excluded = new EnumMap<>(TreePatchDefinition.class);
        List<String> warnings = new ArrayList<>();
        String blocker = account == null || !account.isLoggedIn() ? "Log in to plan a tree run."
                : account.getBoostedFarmingLevel() < crop.getRequiredLevel()
                        ? crop + " requires " + crop.getRequiredLevel() + " Farming; current level is " + account.getBoostedFarmingLevel() + "." : "";
        if (!blocker.isEmpty()) { return new TreeRunPlan(crop, stops, List.of(), excluded, warnings, blocker, compost, cure, 0); }
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
            String visitExclusion = RunVisitSelection.orDefault(visitSelection).exclusion(condition);
            if (!visitExclusion.isEmpty()) { excluded.put(patch, visitExclusion); continue; }
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
            // A checked redwood is normally retained. Replacement supplies need an explicit new plan after removal.
            if (patch.getFamily() == TreeFamily.REDWOOD && (condition == TreePatchCondition.HEALTH_CHECKED
                    || condition == TreePatchCondition.CHECK_HEALTH)) { replant = false; }
            if (replant) { replants++; }
            if (condition == TreePatchCondition.DISEASED) { diseased++; }
            stops.add(new TreeRunStop(patch, condition == TreePatchCondition.DISEASED && cure == TreeRunCure.CURE_PLANT
                    ? "Inspect and cast Lunar Cure Plant. Keep the living tree." : action(condition, patch.getFamily()), replant));
        }
        if (crop == TreeRunCrop.CRYSTAL && compost != TreeRunCompost.NONE && replants > 0)
        {
            return new TreeRunPlan(crop, List.of(), List.of(), excluded, warnings,
                    "Crystal-tree composting is unverified. Select no compost for this plan.", compost, cure, diseased);
        }
        if (!stops.isEmpty())
        {
            warnings.add("Order follows the patch catalogue. Check travel supplies; unsupported destinations require manual travel.");
            warnings.add("Inspect old observations before acting. Removing a healthy tree is a choice; bring a usable axe or check the farmer's removal fee separately.");
            if (crop == TreeRunCrop.CELASTRUS)
            {
                warnings.add("Bring a usable axe to harvest celastrus bark and cut the depleted tree. Harvest yield is not predicted; selected compost is reserved only for possible new plants.");
            }
            if (crop == TreeRunCrop.REDWOOD)
            {
                warnings.add("Keep a mature redwood unless you choose to replace it. Alexandra charges 2,000 coins to remove it; removal coins are not budgeted. After removal, prepare a new plan for replant supplies.");
            }
            supplies.add(new SupplyRequirement(ItemID.SPADE, "Spade", 1, false, "Plant finished saplings and clear stumps."));
            if (replants > 0)
            {
                supplies.add(new SupplyRequirement(crop.getSaplingId(), crop + " sapling", replants, false,
                        "One unnoted, fully grown sapling per potential replant. Seeds and seedlings do not count."));
                supplies.add(new SupplyRequirement(ItemID.RAKE, "Rake", 1, true, "For weeds; auto-weed and leprechaun storage are not checked."));
                if (compost != TreeRunCompost.NONE && !compost.isSpell())
                {
                    supplies.add(new SupplyRequirement(compost.getItemId(), compost + " (apply to new tree patches)", compost.isBottomless() ? 1 : replants, false,
                            compost.isBottomless() ? "Check the declared bucket type and at least " + replants + " remaining uses manually. Contents are not verified."
                                    : "One unnoted bucket per possible new planting. Existing compost and application are not observed; confirm manually."));
                }
                else if (compost == TreeRunCompost.NONE) { warnings.add("No compost application supplies are reserved."); }
                if (compost == TreeRunCompost.FERTILE_SOIL_ULTRA)
                {
                    supplies.add(new SupplyRequirement(ItemID.FOSSIL_VOLCANIC_ASH, "Volcanic ash", 2L * replants, false,
                            "Two per potential Fertile Soil ultracompost cast; the tome unlock must be confirmed manually."));
                }
                if (!crop.hasFarmerProtection())
                {
                    warnings.add("Crystal trees have no farmer protection payment. Composting is unverified and harvest yield is not predicted.");
                }
                else if (protect)
                {
                    Optional<SupplyRequirement> payment = crop.protectionPayment(replants);
                    if (payment.isPresent()) { supplies.add(payment.get()); }
                    else { warnings.add("Data required: " + crop + " farmer payment is unverified. Confirm and bring protection supplies yourself."); }
                }
                else { warnings.add("Farmer protection is disabled. No payment is reserved; selected compost is separate."); }
            }
            if (diseased > 0 && cure == TreeRunCure.PRUNE)
            {
                supplies.add(new SupplyRequirement(ItemID.SECATEURS, "Secateurs", 1, false,
                        "Prune observed diseased trees; magic secateurs also work.", ItemID.FAIRY_ENCHANTED_SECATEURS));
            }
        }
        List<SupplyCheck> checks = new ArrayList<>(new LoadoutService().evaluate(supplies, storage));
        if (stops.stream().anyMatch(TreeRunStop::isReplant) && (crop == TreeRunCrop.CELASTRUS || crop == TreeRunCrop.CRYSTAL))
        {
            checks.add(TreeAxePreparation.evaluate(account, storage));
        }
        return new TreeRunPlan(crop, stops, checks, excluded, warnings, "", compost, cure, diseased);
    }
    private String action(TreePatchCondition condition, TreeFamily family)
    {
        if (family == TreeFamily.CRYSTAL_TREE && (condition == TreePatchCondition.EMPTY || condition == TreePatchCondition.WEEDS))
        {
            return "Inspect, rake if needed, then plant a finished crystal sapling. No farmer payment is available.";
        }
        switch (condition)
        {
            case DISEASED: return "Inspect and prune disease. Keep the living tree; no replacement reserved.";
            case EMPTY: return "Inspect, then plant the sapling and pay for protection if selected.";
            case WEEDS: return "Inspect, rake, then plant and pay for protection if selected.";
            case DEAD: return "Inspect, clear the dead tree, then replant if desired.";
            case STUMP: return "Inspect, clear the stump, then replant if desired.";
            case CHECK_HEALTH: return "Inspect and check health. Remove and replant only if desired.";
            case HARVESTABLE: return "Harvest available celastrus bark with a usable axe before choosing whether to remove the depleted tree.";
            case HARVESTED: return "Celastrus bark was harvested. Keep the depleted tree or cut it down, clear its stump and replant as desired.";
            case HEALTH_CHECKED: return family == TreeFamily.FRUIT_TREE
                    ? "Inspect and pick any available fruit. Keep or remove and replant as desired."
                    : "Inspect the healthy tree. Keep or remove and replant as desired.";
            default: return "Inspect first. Check health if ready; reserve the sapling for a possible replant.";
        }
    }
}
