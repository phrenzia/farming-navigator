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
        List<FieldRunStop> stops = new ArrayList<>(); List<SupplyRequirement> supplies = new ArrayList<>();
        Map<FieldPatchDefinition, String> excluded = new EnumMap<>(FieldPatchDefinition.class); List<String> warnings = new ArrayList<>();
        String blocker = account == null || !account.isLoggedIn() ? "Log in to plan a field run."
                : account.getBoostedFarmingLevel() < crop.getRequiredLevel() ? crop + " requires " + crop.getRequiredLevel() + " Farming (boostable)." : "";
        if (!blocker.isEmpty()) { return new FieldRunPlan(crop, mode, stops, List.of(), excluded, warnings, blocker, compost); }
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
        if (!stops.isEmpty())
        {
            warnings.add("Order follows the patch catalogue. Travel is manual until a verified transport is selected.");
            warnings.add("Inspect old observations. Yield, current compost state, watering, flower/scarecrow protection and leprechaun storage are not checked.");
            if (mode == FieldRunMode.KEEP_MATURE_FLOWERS && crop.getCrop().getFamily() == FieldFamily.FLOWER)
            {
                warnings.add("Keep mature flowers, including a different observed flower crop. No replacement seed is reserved; reprepare if you harvest them.");
            }
            supplies.add(new SupplyRequirement(ItemID.SPADE, "Spade", 1, false, "Harvest allotments or clear dead crops."));
            if (replants > 0)
            {
                supplies.add(new SupplyRequirement(crop.getSeedId(), crop + " seed", (long) replants * crop.getSeedsPerPatch(), false,
                        crop.getSeedsPerPatch() + " seeds per possible planting; seed-saving equipment is not assumed."));
                supplies.add(new SupplyRequirement(ItemID.DIBBER, "Seed dibber", 1, false, "Planting tool; Barbarian Training is not checked."));
                supplies.add(new SupplyRequirement(ItemID.RAKE, "Rake", 1, true, "For weeds; auto-weed is not checked."));
                if (compost != FieldRunCompost.NONE)
                {
                    supplies.add(new SupplyRequirement(compost.getItemId(), compost + " (apply to plots)", compost.isBottomless() ? 1 : replants, false,
                            compost.isBottomless()
                                    ? "Bring one filled bottomless bucket. Manually check the selected compost type and at least " + replants
                                            + " uses. Item presence does not verify contents; it cannot replace farmer payments."
                                    : "One unnoted filled bucket per possible new planting, separate from farmer payments. Application is confirmed manually."));
                }
                else { warnings.add("No compost application supplies are reserved."); }
                if (protect && crop.hasFarmerProtection()) { crop.protectionPayment(payments).ifPresent(supplies::add); }
                else if (crop.hasFarmerProtection()) { warnings.add("Farmer protection is disabled. No payment is reserved."); }
                else { warnings.add("Flowers have no farmer protection payment."); }
            }
            if (disease > 0) { supplies.add(new SupplyRequirement(ItemID.PLANT_CURE, "Plant cure", disease, false, "Cure diseased field crops; no replacement seed/payment is reserved for those stops.")); }
        }
        return new FieldRunPlan(crop, mode, stops, new LoadoutService().evaluateInOrder(supplies, storage), excluded, warnings, "", compost);
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
