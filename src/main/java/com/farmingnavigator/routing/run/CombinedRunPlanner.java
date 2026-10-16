package com.farmingnavigator.routing.run;

import com.farmingnavigator.loadout.*;
import com.farmingnavigator.routing.requirement.*;
import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Sum consumables and spell/charge budgets; share reusable tools without consuming them per component. */
public final class CombinedRunPlanner
{
    private static final Set<Integer> TOOLS = Set.of(ItemID.SPADE, ItemID.RAKE, ItemID.DIBBER,
            ItemID.SECATEURS, ItemID.FAIRY_ENCHANTED_SECATEURS, ItemID.BRONZE_AXE, ItemID.IRON_AXE,
            ItemID.STEEL_AXE, ItemID.BLACK_AXE, ItemID.MITHRIL_AXE, ItemID.ADAMANT_AXE, ItemID.RUNE_AXE, ItemID.DRAGON_AXE,
            ItemID.LEATHER_GLOVES, ItemID.HUNDRED_PIRATE_DIVING_HELMET, ItemID.HUNDRED_PIRATE_DIVING_BACKPACK);
    public CombinedRunPlan plan(Set<CombinedRunKind> selected, HerbRunPlan herb, TreeRunPlan tree, GardenRunPlan garden, FieldRunPlan field,
            RequirementContext context)
    {
        Assembly assembly = new Assembly();
        if (selected.size() < 2) { assembly.blockers.add("Select at least two run types for combined preparation."); }
        if (context == null || context.getAccountState() == null || !context.getAccountState().isLoggedIn() || context.getConfig() == null)
        {
            assembly.blockers.add("Log in with current preparation settings to combine runs.");
            return assembly.build(context);
        }
        for (CombinedRunKind kind : CombinedRunKind.values())
        {
            if (!selected.contains(kind)) { continue; }
            switch (kind)
            {
                case HERB:
                    if (herb == null) { assembly.blockers.add("Herb preparation is unavailable."); break; }
                    assembly.add(kind, kind + " - " + herb.getCrop(), herb.getStops(), herb.getSupplies(), herb.getCareResources(), herb.getBlocker(),
                            herb.getTransportResources(), herb.getExcluded().size(), List.of(), context.getConfig().herbRunCompost().name(), (int) herb.getStops().stream().filter(HerbRunStop::isReplant).count()); break;
                case TREE:
                    if (tree == null) { assembly.blockers.add("Tree preparation is unavailable."); break; }
                    assembly.add(kind, kind + " - " + tree.getCrop(), tree.getStops(), tree.getSupplies(), null, tree.getBlocker(), tree.getTransportResources(), tree.getExcluded().size(), tree.getWarnings(), "NONE", 0); break;
                case GARDEN:
                    if (garden == null) { assembly.blockers.add("Bush/cactus preparation is unavailable."); break; }
                    assembly.add(kind, kind + " - " + garden.getCrop(), garden.getStops(), garden.getSupplies(), garden.getCareResources(), garden.getBlocker(),
                            garden.getTransportResources(), garden.getExcluded().size(), garden.getWarnings(), garden.getCompost().name(), (int) garden.getStops().stream().filter(GardenRunStop::isReplant).count()); break;
                case FIELD:
                    if (field == null) { assembly.blockers.add("Field preparation is unavailable."); break; }
                    assembly.add(kind, kind + " - " + field.getCrop(), field.getStops(), field.getSupplies(), field.getCareResources(), field.getBlocker(),
                            field.getTransportResources(), field.getExcluded().size(), field.getWarnings(), field.getCompost().name(), (int) field.getStops().stream().filter(FieldRunStop::isReplant).count()); break;
                default: break;
            }
        }
        return assembly.build(context);
    }
    private static final class Assembly
    {
        private final List<String> components = new ArrayList<>(), warnings = new ArrayList<>(), blockers = new ArrayList<>();
        private final List<RunTravelStop> stops = new ArrayList<>();
        private final Map<String, SupplyRequirement> supplies = new LinkedHashMap<>();
        private final Map<String, Integer> bottomlessUses = new LinkedHashMap<>();
        private final List<RequirementResult> careChecks = new ArrayList<>();
        private final RuneCost.Builder runes = RuneCost.builder();
        private void add(CombinedRunKind kind, String label, List<? extends RunTravelStop> sequence, List<SupplyCheck> checks, HerbRunCareResources care,
                String blocker, RunTransportResources resources, int excludedCount, List<String> componentWarnings, String compost, int replants)
        {
            components.add(label + " - " + sequence.size() + " stops; excluded: " + excludedCount);
            componentWarnings.forEach(note -> warnings.add(label + ": " + note));
            if (!blocker.isEmpty()) { blockers.add(label + ": " + blocker); return; }
            if (resources == null || kind != CombinedRunKind.TREE && care == null)
            {
                blockers.add(label + ": refresh its individual preparation before combining."); return;
            }
            stops.addAll(sequence);
            if (care != null)
            {
                care.getRunes().getQuantities().forEach(runes::add); careChecks.addAll(care.getChecks());
            }
            for (SupplyCheck check : checks)
            {
                SupplyRequirement requirement = check.getRequirement();
                boolean bottomless = requirement.getItemId() == ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED;
                String key = requirement.getItemId() + ":" + requirement.getAcceptedItemIds() + (bottomless ? ":" + compost : "");
                SupplyRequirement previous = supplies.get(key);
                long quantity = previous == null ? requirement.getQuantity() : TOOLS.contains(requirement.getItemId()) || bottomless
                        ? Math.max(previous.getQuantity(), requirement.getQuantity()) : Math.addExact(previous.getQuantity(), requirement.getQuantity());
                boolean optional = requirement.isOptional() && (previous == null || previous.isOptional());
                String note = bottomless ? "Manually check this declared compost type and the combined use count below. Other filled bucket types cannot substitute."
                        : "Combined quantity for selected runs. " + requirement.getNote();
                int[] alternatives = requirement.getAcceptedItemIds().stream().filter(id -> id != requirement.getItemId()).mapToInt(Integer::intValue).toArray();
                supplies.put(key, new SupplyRequirement(requirement.getItemId(), previous == null ? requirement.getName() : previous.getName(), quantity, optional, note, alternatives));
                if (bottomless) { bottomlessUses.merge(compost, replants, Integer::sum); }
            }
        }
        private CombinedRunPlan build(RequirementContext context)
        {
            warnings.add("Preparation worksheet: follow the component runs in the listed order, using one guided checklist at a time. This does not optimize or interleave routes.");
            warnings.add("All component arrival legs remain budgeted. Shared local walks already verified inside a component remain local; no teleport is removed just because two patches look nearby.");
            warnings.add("Reusable tools can satisfy multiple tool rows. Consumables and different declared bottomless contents share one stock allocation. Inventory capacity, yield and profit are not predicted.");
            bottomlessUses.forEach((type, uses) -> warnings.add(type.replace('_', ' ') + ": manually check at least " + uses + " total uses. One bucket of this declared type is reserved; distinct types need separate buckets."));
            if (!blockers.isEmpty()) { return new CombinedRunPlan(components, List.of(), List.of(), List.of(), null, warnings, blockers); }
            List<SupplyRequirement> consumable = new ArrayList<>(), reusable = new ArrayList<>();
            supplies.values().forEach(requirement -> (TOOLS.contains(requirement.getItemId()) ? reusable : consumable).add(requirement));
            // Mandatory and restricted forms first; optional conveniences cannot consume required stock.
            consumable.sort(Comparator.comparing(SupplyRequirement::isOptional).thenComparingInt(requirement -> requirement.getAcceptedItemIds().size()));
            LoadoutService service = new LoadoutService();
            List<SupplyCheck> checks = new ArrayList<>(service.evaluateInOrder(consumable, context.getStorageSnapshot()));
            checks.addAll(service.evaluate(reusable, context.getStorageSnapshot()));
            return new CombinedRunPlan(components, stops, checks, careChecks, new RunTransportResourcePlanner().evaluate(stops, context, runes.build()), warnings, blockers);
        }
    }
}
