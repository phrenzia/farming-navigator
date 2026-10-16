package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.*;
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

/** Keep mature plants by default; reserve replacement materials only for possible new planting. */
public final class GardenRunPlanner
{
    public GardenRunPlan plan(AccountState account, Set<GardenPatchDefinition> selected, GardenRunCrop crop, GardenRunMode mode,
            boolean protect, Map<GardenPatchDefinition, GardenPatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        return plan(account, selected, crop, mode, protect, GardenRunCompost.NONE, observations, storage, now);
    }
    public GardenRunPlan plan(AccountState account, Set<GardenPatchDefinition> selected, GardenRunCrop crop, GardenRunMode mode,
            boolean protect, GardenRunCompost compost, Map<GardenPatchDefinition, GardenPatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        return plan(account, selected, crop, mode, protect, compost, HerbRunCure.PLANT_CURE, observations, storage, now);
    }
    public GardenRunPlan plan(AccountState account, Set<GardenPatchDefinition> selected, GardenRunCrop crop, GardenRunMode mode,
            boolean protect, GardenRunCompost compost, HerbRunCure cure, Map<GardenPatchDefinition, GardenPatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        List<GardenRunStop> stops = new ArrayList<>(); List<SupplyRequirement> supplies = new ArrayList<>();
        Map<GardenPatchDefinition, String> excluded = new EnumMap<>(GardenPatchDefinition.class); List<String> warnings = new ArrayList<>();
        String blocker = account == null || !account.isLoggedIn() ? "Log in to plan a bush/cactus run."
                : account.getBoostedFarmingLevel() < crop.getRequiredLevel() ? crop + " requires " + crop.getRequiredLevel() + " Farming (boostable)." : "";
        if (!blocker.isEmpty()) { return new GardenRunPlan(crop, mode, stops, List.of(), excluded, warnings, blocker, compost, cure, 0); }
        int replants = 0, bushDisease = 0, cactusDisease = 0;
        for (GardenPatchDefinition patch : GardenPatchDefinition.values())
        {
            if (!selected.contains(patch) || patch.getFamily() != crop.getCrop().getFamily()) { continue; }
            String access = new GardenPatchAccess().missingRequirement(patch, account);
            if (!access.isEmpty()) { excluded.put(patch, access); continue; }
            GardenPatchObservation observation = observations.get(patch);
            if (observation != null && (observation.getObservedAt().isAfter(now)
                    || observation.getState().getCrop().getFamily() != null && observation.getState().getCrop().getFamily() != patch.getFamily())) { observation = null; }
            GardenPatchCondition condition = observation == null ? GardenPatchCondition.UNKNOWN : observation.getState().getCondition();
            if (condition == GardenPatchCondition.GROWING)
            {
                Optional<GardenGrowthWindow> window = new GardenGrowthTimer().estimate(observation, now);
                if (window.isPresent() && now.isBefore(window.get().getEarliest()))
                {
                    excluded.put(patch, "Last observed growing; conditional first health-check window has not started."); continue;
                }
            }
            boolean mature = condition == GardenPatchCondition.CHECK_HEALTH || condition == GardenPatchCondition.HEALTH_CHECKED;
            boolean replant = condition != GardenPatchCondition.DISEASED && (!mature || mode == GardenRunMode.REPLACE_MATURE);
            if (replant) { replants++; }
            if (condition == GardenPatchCondition.DISEASED)
            {
                if (patch.getFamily() == GardenFamily.BUSH) { bushDisease++; } else { cactusDisease++; }
            }
            stops.add(new GardenRunStop(patch, condition == GardenPatchCondition.DISEASED && cure == HerbRunCure.CURE_PLANT
                    ? "Inspect and cast Lunar Cure Plant. Keep the living plant." : action(condition, mode, patch.getFamily()), replant));
        }
        if (!stops.isEmpty())
        {
            warnings.add("Order follows the patch catalogue. Travel is manual until a verified transport is selected.");
            warnings.add("Inspect old observations. Produce quantities and regrowth are not predicted; current compost state and leprechaun storage are not checked.");
            if (mode == GardenRunMode.KEEP_MATURE) { warnings.add("Keep mature plants. No replacement seed/payment is reserved for observed mature plants; prepare a new plan if they are cleared."); }
            supplies.add(new SupplyRequirement(ItemID.SPADE, "Spade", 1, false, "Clear dead plants or explicitly selected replacements."));
            if (replants > 0)
            {
                supplies.add(new SupplyRequirement(crop.getSeedId(), crop + " seed", replants, false, "One seed per possible new plant; kept mature and diseased plants are excluded."));
                if(com.farmingnavigator.loadout.PlantingToolPolicy.gardenDibberRequired(account,crop.getCrop().getFamily()))
                { supplies.add(new SupplyRequirement(ItemID.DIBBER,"Seed dibber",1,false,com.farmingnavigator.loadout.PlantingToolPolicy.retainedNote(account))); }
                else { warnings.add(com.farmingnavigator.loadout.PlantingToolPolicy.omittedNote()); }
                supplies.add(new SupplyRequirement(ItemID.RAKE, "Rake", 1, true, "For weeds; auto-weed is not checked."));
                if (compost != GardenRunCompost.NONE && !compost.isSpell())
                {
                    supplies.add(new SupplyRequirement(compost.getItemId(), compost + " (apply to patches)", compost.isBottomless() ? 1 : replants, false,
                            compost.isBottomless()
                                    ? "Bring one filled bottomless bucket. Manually check the selected compost type and at least " + replants
                                            + " uses. Item presence does not verify contents; it cannot replace farmer payments."
                                    : "One unnoted filled bucket per possible new planting, separate from farmer payments. Application is confirmed manually."));
                }
                else if (compost == GardenRunCompost.NONE) { warnings.add("No compost application supplies are reserved."); }
                if (compost == GardenRunCompost.FERTILE_SOIL_ULTRA)
                {
                    supplies.add(new SupplyRequirement(ItemID.FOSSIL_VOLCANIC_ASH, "Volcanic ash", 2L * replants, false,
                            "Two per potential Fertile Soil ultracompost cast; the ash-covered tome unlock must be confirmed."));
                }
                if (!crop.hasFarmerProtection()) { warnings.add("Poison ivy has no farmer payment; none is reserved."); }
                else if (protect) { crop.protectionPayment(replants).ifPresent(supplies::add); }
                else { warnings.add("Farmer protection is disabled. No payment is reserved."); }
            }
            if (bushDisease > 0 && cure != HerbRunCure.CURE_PLANT) { supplies.add(new SupplyRequirement(ItemID.SECATEURS, "Secateurs", 1, false, "Prune diseased bushes; magic secateurs also work.", ItemID.FAIRY_ENCHANTED_SECATEURS)); }
            if (cactusDisease > 0 && cure != HerbRunCure.CURE_PLANT) { supplies.add(new SupplyRequirement(ItemID.PLANT_CURE, "Plant cure", cactusDisease, false, "Cure diseased cacti; no replacement seed/payment is reserved.")); }
        }
        return new GardenRunPlan(crop, mode, stops, new LoadoutService().evaluateInOrder(supplies, storage), excluded, warnings, "", compost, cure, bushDisease + cactusDisease);
    }
    private String action(GardenPatchCondition condition, GardenRunMode mode, GardenFamily family)
    {
        switch (condition)
        {
            case DISEASED: return family == GardenFamily.BUSH ? "Inspect and prune disease. Keep the living bush." : "Inspect and use plant cure. Keep the living cactus.";
            case CHECK_HEALTH: return mode == GardenRunMode.KEEP_MATURE ? "Check health, pick any available produce and keep the plant." : "Check health, pick any available produce, then clear and replant only as selected.";
            case HEALTH_CHECKED: return mode == GardenRunMode.KEEP_MATURE ? "Pick any available produce and keep the plant." : "Pick any available produce, then clear and replant only as selected.";
            case DEAD: return "Inspect, clear the dead plant, then plant a new seed.";
            case EMPTY: return "Inspect, then plant one seed and pay for protection if applicable and selected.";
            case WEEDS: return "Inspect and rake, then plant one seed and pay for protection if applicable and selected.";
            default: return "Inspect first. Wait for growing plants; reserve a seed only for possible new planting.";
        }
    }
}
