package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class GardenRunPlan
{
    private final GardenRunCrop crop;
    private final GardenRunMode mode;
    private final GardenRunCompost compost;
    private final HerbRunCure cure;
    private final int potentialCures;
    private final HerbRunCareResources careResources;
    private final List<GardenRunStop> stops;
    private final List<SupplyCheck> supplies;
    private final Map<GardenPatchDefinition, String> excluded;
    private final List<String> warnings;
    private final String blocker;
    private final RunTransportResources transportResources;
    GardenRunPlan(GardenRunCrop crop, GardenRunMode mode, List<GardenRunStop> stops, List<SupplyCheck> supplies,
            Map<GardenPatchDefinition, String> excluded, List<String> warnings, String blocker)
    {
        this(crop, mode, stops, supplies, excluded, warnings, blocker, GardenRunCompost.NONE);
    }
    GardenRunPlan(GardenRunCrop crop, GardenRunMode mode, List<GardenRunStop> stops, List<SupplyCheck> supplies,
            Map<GardenPatchDefinition, String> excluded, List<String> warnings, String blocker, GardenRunCompost compost)
    {
        this(crop, mode, stops, supplies, excluded, warnings, blocker, compost, HerbRunCure.PLANT_CURE, 0);
    }
    GardenRunPlan(GardenRunCrop crop, GardenRunMode mode, List<GardenRunStop> stops, List<SupplyCheck> supplies,
            Map<GardenPatchDefinition, String> excluded, List<String> warnings, String blocker, GardenRunCompost compost, HerbRunCure cure, int potentialCures)
    {
        this(crop, mode, stops, supplies, excluded, warnings, blocker, compost, cure, potentialCures, null, null);
    }
    private GardenRunPlan(GardenRunCrop crop, GardenRunMode mode, List<GardenRunStop> stops, List<SupplyCheck> supplies,
            Map<GardenPatchDefinition, String> excluded, List<String> warnings, String blocker, GardenRunCompost compost, HerbRunCure cure, int potentialCures, RunTransportResources resources, HerbRunCareResources careResources)
    {
        this.cure = cure; this.potentialCures = potentialCures; this.careResources = careResources;
        this.crop = crop; this.mode = mode; this.compost = compost; this.stops = List.copyOf(stops); this.supplies = List.copyOf(supplies);
        EnumMap<GardenPatchDefinition, String> copy = new EnumMap<>(GardenPatchDefinition.class); copy.putAll(excluded);
        this.excluded = Collections.unmodifiableMap(copy); this.warnings = List.copyOf(warnings); this.blocker = blocker; this.transportResources = resources;
    }
    public GardenRunCrop getCrop() { return crop; }
    public GardenRunMode getMode() { return mode; }
    public GardenRunCompost getCompost() { return compost; }
    public HerbRunCure getCure() { return cure; }
    public HerbRunCareResources getCareResources() { return careResources; }
    public List<GardenRunStop> getStops() { return stops; }
    public List<SupplyCheck> getSupplies() { return supplies; }
    public Map<GardenPatchDefinition, String> getExcluded() { return excluded; }
    public List<String> getWarnings() { return warnings; }
    public String getBlocker() { return blocker; }
    public RunTransportResources getTransportResources() { return transportResources; }
    public GardenRunPlan withTransports(Map<GardenPatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context)
    {
        List<GardenRunStop> routed = new ArrayList<>();
        for (GardenRunStop stop : stops)
        {
            List<VerifiedTransportResult> usable = HerbTransportSelector.select(transports.getOrDefault(stop.getPatch(), List.of()), preference);
            routed.add(stop.withTransports(usable.isEmpty() ? null : usable.get(0), usable.size() < 2 ? null : usable.get(1)));
        }
        HerbRunCareResources care = HerbRunCareResources.evaluateCounts(stops.stream().filter(GardenRunStop::isReplant).count(),
                potentialCures, cure, HerbRunCompost.valueOf(compost.name()),
                context != null && context.getConfig() != null && context.getConfig().gardenAshTomeRead(), context);
        return new GardenRunPlan(crop, mode, routed, supplies, excluded, warnings, blocker, compost, cure, potentialCures,
                new RunTransportResourcePlanner().evaluate(routed, context, care.getRunes()), care);
    }
}
