package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FieldRunPlan
{
    private final FieldRunCrop crop;
    private final FieldRunMode mode;
    private final FieldRunCompost compost;
    private final FieldRunWatering watering;
    private final WateringCanBudget wateringBudget;
    private final HerbRunCure cure;
    private final HerbRunCareResources careResources;
    private final List<SupplyCheck> plannedSupplies;
    private final List<FieldRunStop> stops;
    private final List<SupplyCheck> supplies;
    private final Map<FieldPatchDefinition, String> excluded;
    private final List<String> warnings;
    private final String blocker;
    private final RunTransportResources transportResources;
    FieldRunPlan(FieldRunCrop crop, FieldRunMode mode, List<FieldRunStop> stops, List<SupplyCheck> supplies,
            Map<FieldPatchDefinition, String> excluded, List<String> warnings, String blocker)
    {
        this(crop, mode, stops, supplies, excluded, warnings, blocker, FieldRunCompost.NONE);
    }
    FieldRunPlan(FieldRunCrop crop, FieldRunMode mode, List<FieldRunStop> stops, List<SupplyCheck> supplies,
            Map<FieldPatchDefinition, String> excluded, List<String> warnings, String blocker, FieldRunCompost compost)
    {
        this(crop, mode, stops, supplies, excluded, warnings, blocker, compost, HerbRunCure.PLANT_CURE);
    }
    FieldRunPlan(FieldRunCrop crop, FieldRunMode mode, List<FieldRunStop> stops, List<SupplyCheck> supplies,
            Map<FieldPatchDefinition, String> excluded, List<String> warnings, String blocker, FieldRunCompost compost, HerbRunCure cure)
    {
        this(crop, mode, stops, supplies, excluded, warnings, blocker, compost, cure, FieldRunWatering.NONE);
    }
    FieldRunPlan(FieldRunCrop crop, FieldRunMode mode, List<FieldRunStop> stops, List<SupplyCheck> supplies,
            Map<FieldPatchDefinition, String> excluded, List<String> warnings, String blocker, FieldRunCompost compost, HerbRunCure cure, FieldRunWatering watering)
    {
        this(crop, mode, stops, supplies, excluded, warnings, blocker, compost, cure, watering, null, null, null);
    }
    private FieldRunPlan(FieldRunCrop crop, FieldRunMode mode, List<FieldRunStop> stops, List<SupplyCheck> supplies,
            Map<FieldPatchDefinition, String> excluded, List<String> warnings, String blocker, FieldRunCompost compost, HerbRunCure cure, FieldRunWatering watering, RunTransportResources resources, HerbRunCareResources careResources, WateringCanBudget wateringBudget)
    {
        this.crop = crop; this.mode = mode; this.compost = compost; this.stops = List.copyOf(stops); this.plannedSupplies = List.copyOf(supplies);
        this.cure = cure; this.careResources = careResources; this.watering = watering; this.wateringBudget = wateringBudget;
        this.supplies = List.copyOf(supplies.stream().filter(check -> cure != HerbRunCure.CURE_PLANT
                || check.getRequirement().getItemId() != net.runelite.api.gameval.ItemID.PLANT_CURE).collect(java.util.stream.Collectors.toList()));
        EnumMap<FieldPatchDefinition, String> copy = new EnumMap<>(FieldPatchDefinition.class); copy.putAll(excluded);
        this.excluded = Collections.unmodifiableMap(copy); this.warnings = List.copyOf(warnings); this.blocker = blocker; this.transportResources = resources;
    }
    public FieldRunCrop getCrop() { return crop; }
    public FieldRunMode getMode() { return mode; }
    public FieldRunCompost getCompost() { return compost; }
    public FieldRunWatering getWatering() { return watering; }
    public int getWateringApplications() { return watering.appliesTo(crop.getCrop().getFamily()) ? (int) stops.stream().filter(FieldRunStop::isReplant).count() : 0; }
    public WateringCanBudget getWateringBudget() { return wateringBudget; }
    public HerbRunCure getCure() { return cure; }
    public HerbRunCareResources getCareResources() { return careResources; }
    public List<FieldRunStop> getStops() { return stops; }
    public List<SupplyCheck> getSupplies() { return supplies; }
    public Map<FieldPatchDefinition, String> getExcluded() { return excluded; }
    public List<String> getWarnings() { return warnings; }
    public String getBlocker() { return blocker; }
    public RunTransportResources getTransportResources() { return transportResources; }
    public FieldRunPlan withTransports(Map<FieldPatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context)
    { return route(transports,preference,context,true); }
    /** Active guidance refreshes verification without searching against already-consumed whole-run supplies. */
    public FieldRunPlan refreshTransports(Map<FieldPatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context)
    { return route(transports,preference,context,false); }
    private FieldRunPlan route(Map<FieldPatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context, boolean repair)
    {
        List<FieldRunStop> routed = new ArrayList<>();
        com.farmingnavigator.farming.field.FieldPatchRegion previousRegion = null;
        for (FieldRunStop stop : stops)
        {
            List<VerifiedTransportResult> usable = repair
                    ? HerbTransportSelector.select(transports.getOrDefault(stop.getPatch(), List.of()), preference)
                    : HerbTransportSelector.selectForRefresh(transports.getOrDefault(stop.getPatch(), List.of()), preference,stop.getRecommended());
            com.farmingnavigator.farming.field.FieldPatchRegion region = com.farmingnavigator.farming.field.FieldPatchRegion.forLocation(stop.getPatch().getLocation());
            routed.add(region != null && region == previousRegion ? stop.withLocalWalk()
                    : stop.withTransports(usable.isEmpty() ? null : usable.get(0), usable.size() < 2 ? null : usable.get(1)));
            previousRegion = region;
        }
        int diseaseCount = (int) plannedSupplies.stream().filter(check -> check.getRequirement().getItemId() == net.runelite.api.gameval.ItemID.PLANT_CURE)
                .mapToLong(check -> check.getRequirement().getQuantity()).sum();
        HerbRunCareResources care = HerbRunCareResources.evaluateCounts(stops.stream().filter(FieldRunStop::isReplant).count(),
                diseaseCount, cure, HerbRunCompost.valueOf(compost.name()),
                context != null && context.getConfig() != null && context.getConfig().fieldAshTomeRead(), context);
        RunTransportResources resources;
        if (repair && HerbTransportPreference.repairsSharedBudget(preference))
        {
            SharedTransportRepair.Result<FieldRunStop> repaired=new SharedTransportRepair().repair(routed,
                    stop -> transports.getOrDefault(stop.getPatch(),List.of()),
                    (stop,primary) -> stop.withTransports(primary,HerbTransportSelector.fallback(transports.getOrDefault(stop.getPatch(),List.of()),primary)),
                    context,care.getRunes(),preference==HerbTransportPreference.SHARED_BUDGET_PAIR);
            routed=repaired.getStops(); resources=repaired.getResources();
        }
        else { resources=new RunTransportResourcePlanner().evaluate(routed,context,care.getRunes()); }
        return new FieldRunPlan(crop, mode, routed, plannedSupplies, excluded, warnings, blocker, compost, cure, watering,
                resources, care,
                WateringCanBudget.evaluate(getWateringApplications(), context == null ? null : context.getStorageSnapshot()));
    }
}
