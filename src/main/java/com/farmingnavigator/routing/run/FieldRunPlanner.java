package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
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

/** Reserve three seeds per possible allotment planting; keep mature flowers unless explicitly selected. */
public final class FieldRunPlanner
{
    public FieldRunPlan plan(AccountState account, Set<FieldPatchDefinition> selected, FieldRunCrop crop, FieldRunMode mode,
            boolean protect, Map<FieldPatchDefinition, FieldPatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        return plan(account, selected, crop, mode, protect, FieldRunCompost.NONE, observations, storage, now);
    }
    public FieldRunPlan plan(AccountState account, Set<FieldPatchDefinition> selected, FieldRunCrop crop, FieldRunMode mode,
            boolean protect, FieldRunCompost compost, Map<FieldPatchDefinition, FieldPatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        return plan(account, selected, crop, mode, protect, compost, HerbRunCure.PLANT_CURE, observations, storage, now);
    }
    public FieldRunPlan plan(AccountState account, Set<FieldPatchDefinition> selected, FieldRunCrop crop, FieldRunMode mode,
            boolean protect, FieldRunCompost compost, HerbRunCure cure, Map<FieldPatchDefinition, FieldPatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        return plan(account, selected, crop, mode, protect, compost, cure, FieldRunWatering.NONE, observations, storage, now);
    }
    public FieldRunPlan plan(AccountState account, Set<FieldPatchDefinition> selected, FieldRunCrop crop, FieldRunMode mode,
            boolean protect, FieldRunCompost compost, HerbRunCure cure, FieldRunWatering watering, Map<FieldPatchDefinition, FieldPatchObservation> observations, StorageSnapshot storage, Instant now)
    {
        return plan(account, selected, crop, mode, protect, compost, cure, watering, observations, storage, now, RunVisitSelection.ALL_SELECTED);
    }
    public FieldRunPlan plan(AccountState account, Set<FieldPatchDefinition> selected, FieldRunCrop crop, FieldRunMode mode,
            boolean protect, FieldRunCompost compost, HerbRunCure cure, FieldRunWatering watering, Map<FieldPatchDefinition, FieldPatchObservation> observations, StorageSnapshot storage, Instant now,
            RunVisitSelection visitSelection)
    {
        List<FieldRunStop> stops = new ArrayList<>(); List<SupplyRequirement> supplies = new ArrayList<>();
        Map<FieldPatchDefinition, String> excluded = new EnumMap<>(FieldPatchDefinition.class); List<String> warnings = new ArrayList<>();
        String blocker = account == null || !account.isLoggedIn() ? "Log in to plan a field run."
                : account.getBoostedFarmingLevel() < crop.getRequiredLevel() ? crop + " requires " + crop.getRequiredLevel() + " Farming (boostable)." : "";
        if (!blocker.isEmpty()) { return new FieldRunPlan(crop, mode, stops, List.of(), excluded, warnings, blocker, compost, cure, watering); }
        int replants = 0, payments = 0, disease = 0;
        for (FieldPatchDefinition patch : FieldPatchDefinition.values())
        {
            if (!selected.contains(patch) || patch.getFamily() != crop.getCrop().getFamily()) { continue; }
            String access = new FieldPatchAccess().missingRequirement(patch, account);
            if (!access.isEmpty()) { excluded.put(patch, access); continue; }
            FieldPatchObservation observation = observations.get(patch);
            if (observation != null && (observation.getObservedAt().isAfter(now)
                    || observation.getState().getCrop().getFamily() != null && observation.getState().getCrop().getFamily() != patch.getFamily())) { observation = null; }
            FieldPatchCondition condition = observation == null ? FieldPatchCondition.UNKNOWN : observation.getState().getCondition();
            String visitExclusion = RunVisitSelection.orDefault(visitSelection).exclusion(condition);
            if (!visitExclusion.isEmpty()) { excluded.put(patch, visitExclusion); continue; }
            if (condition == FieldPatchCondition.SCARECROW)
            {
                excluded.put(patch, "Scarecrow observed. Keep it, or remove it manually and prepare a new plan; flower planting is not assumed."); continue;
            }
            if (condition == FieldPatchCondition.GROWING)
            {
                Optional<FieldGrowthWindow> window = new FieldGrowthTimer().estimate(observation, now);
                if (window.isPresent() && now.isBefore(window.get().getEarliest()))
                {
                    excluded.put(patch, "Last observed growing; conditional harvest window has not started."); continue;
                }
            }
            boolean keep = patch.getFamily() == FieldFamily.FLOWER && condition == FieldPatchCondition.HARVESTABLE
                    && mode == FieldRunMode.KEEP_MATURE_FLOWERS;
            boolean replant = condition != FieldPatchCondition.DISEASED && !keep;
            if (replant)
            {
                replants++;
                if (patch != FieldPatchDefinition.HARMONY_ISLAND_ALLOTMENT) { payments++; }
                else if (protect) { warnings.add("Harmony Island has no farmer. No protection payment is reserved for its allotment."); }
            }
            if (condition == FieldPatchCondition.DISEASED) { disease++; }
            stops.add(new FieldRunStop(patch, action(condition, keep), replant));
        }
        if (crop.getCrop().getFamily() == FieldFamily.SEAWEED && (compost.isSpell() && replants > 0 || cure == HerbRunCure.CURE_PLANT && disease > 0))
        {
            return new FieldRunPlan(crop, mode, List.of(), List.of(), excluded, warnings,
                    "Underwater spell care is unverified. Choose bucket compost and plant cure for this seaweed plan.", compost, cure, watering);
        }
        if (!stops.isEmpty())
        {
            warnings.add("Order follows the patch catalogue. Travel is manual until a verified transport is selected.");
            if (watering.appliesTo(crop.getCrop().getFamily()) && replants > 0)
            {
                warnings.add("Reserve one water application after each possible planting. Confirm watering manually; it only covers the current growth cycle. Refills, Gricoller charges and later-cycle watering visits are not assumed.");
            }
            else if (watering != FieldRunWatering.NONE && !watering.appliesTo(crop.getCrop().getFamily()))
            {
                warnings.add("Watering is not applicable to this crop family. No water supplies or watering step are reserved.");
            }
            warnings.add("Inspect old observations. Yield, current compost state, watering, flower/scarecrow protection and leprechaun storage are not checked.");
            if (mode == FieldRunMode.KEEP_MATURE_FLOWERS && crop.getCrop().getFamily() == FieldFamily.FLOWER)
            {
                warnings.add("Keep mature flowers, including a different observed flower crop. No replacement seed is reserved; reprepare if you harvest them.");
            }
            if (crop.getCrop().getFamily() == FieldFamily.SEAWEED)
            {
                warnings.add("Underwater visit: check air supply, diving equipment and current item restrictions yourself. Equipment is optional here, not verified as safe to dive.");
                supplies.add(new SupplyRequirement(ItemID.HUNDRED_PIRATE_DIVING_HELMET, "Fishbowl helmet", 1, true, "Recommended together with diving apparatus; equip and check air supply manually."));
                supplies.add(new SupplyRequirement(ItemID.HUNDRED_PIRATE_DIVING_BACKPACK, "Diving apparatus", 1, true, "Recommended together with fishbowl helmet; diving restrictions are not checked."));
            }
            if (crop.getCrop().getFamily() == FieldFamily.BELLADONNA)
            {
                warnings.add("Equip suitable gloves before picking belladonna to avoid poison damage. Leather gloves are suggested; other glove variants are not checked.");
                supplies.add(new SupplyRequirement(ItemID.LEATHER_GLOVES, "Leather gloves (or check your gloves)", 1, true, "Equip suitable gloves before harvesting belladonna; alternative glove types are not automatically checked."));
            }
            supplies.add(new SupplyRequirement(ItemID.SPADE, "Spade", 1, false, "Harvest allotments or clear dead crops."));
            if (replants > 0)
            {
                supplies.add(new SupplyRequirement(crop.getSeedId(), crop + " seed/spore", (long) replants * crop.getSeedsPerPatch(), false,
                        crop.getSeedsPerPatch() + " seeds/spores per possible planting; seed-saving equipment is not assumed."));
                if(com.farmingnavigator.loadout.PlantingToolPolicy.fieldDibberRequired(account,crop.getCrop().getFamily()))
                { supplies.add(new SupplyRequirement(ItemID.DIBBER,"Seed dibber",1,false,com.farmingnavigator.loadout.PlantingToolPolicy.retainedNote(account))); }
                else { warnings.add(com.farmingnavigator.loadout.PlantingToolPolicy.omittedNote()); }
                supplies.add(new SupplyRequirement(ItemID.RAKE, "Rake", 1, true, "For weeds; auto-weed is not checked."));
                if (compost != FieldRunCompost.NONE && !compost.isSpell())
                {
                    supplies.add(new SupplyRequirement(compost.getItemId(), compost + " (apply to plots)", compost.isBottomless() ? 1 : replants, false,
                            compost.isBottomless()
                                    ? "Bring one filled bottomless bucket. Manually check the selected compost type and at least " + replants
                                            + " uses. Item presence does not verify contents; it cannot replace farmer payments."
                                    : "One unnoted filled bucket per possible new planting, separate from farmer payments. Application is confirmed manually."));
                }
                else if (compost == FieldRunCompost.NONE) { warnings.add("No compost application supplies are reserved."); }
                if (compost == FieldRunCompost.FERTILE_SOIL_ULTRA)
                {
                    supplies.add(new SupplyRequirement(ItemID.FOSSIL_VOLCANIC_ASH, "Volcanic ash", 2L * replants, false,
                            "Two per potential Fertile Soil ultracompost cast; the ash-covered tome unlock must be confirmed."));
                }
                if (protect && crop.hasFarmerProtection()) { crop.protectionPayment(payments).ifPresent(supplies::add); }
                else if (crop.hasFarmerProtection()) { warnings.add("Farmer protection is disabled. No payment is reserved."); }
                else { warnings.add(crop.getCrop().getFamily().getLabel() + " have no farmer protection payment."); }
            }
            if (disease > 0) { supplies.add(new SupplyRequirement(ItemID.PLANT_CURE, "Plant cure", disease, false, "Cure diseased field crops; no replacement seed/payment is reserved for those stops.")); }
        }
        return new FieldRunPlan(crop, mode, stops, new LoadoutService().evaluateInOrder(supplies, storage), excluded, warnings, "", compost, cure, watering);
    }
    private String action(FieldPatchCondition condition, boolean keep)
    {
        if (keep) { return "Inspect and keep the mature flower. Harvesting it may remove protection for nearby allotments."; }
        switch (condition)
        {
            case DISEASED: return "Inspect and use plant cure. Keep the living crop.";
            case HARVESTABLE: return "Harvest manually until empty, then plant the selected crop and pay for protection if applicable and selected.";
            case DEAD: return "Inspect, clear the dead crop, then replant.";
            case EMPTY: return "Inspect, plant the selected crop, then pay for protection if applicable and selected.";
            case WEEDS: return "Inspect and rake, then plant the selected crop and pay for protection if applicable and selected.";
            default: return "Inspect first. Wait for growing crops; reserve seeds only for possible new planting.";
        }
    }
}
