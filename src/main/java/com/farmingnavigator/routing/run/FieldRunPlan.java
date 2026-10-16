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
    private final List<FieldRunStop> stops;
    private final List<SupplyCheck> supplies;
    private final Map<FieldPatchDefinition, String> excluded;
    private final List<String> warnings;
    private final String blocker;
    private final RunTransportResources transportResources;
    FieldRunPlan(FieldRunCrop crop, FieldRunMode mode, List<FieldRunStop> stops, List<SupplyCheck> supplies,
            Map<FieldPatchDefinition, String> excluded, List<String> warnings, String blocker)
    {
        this(crop, mode, stops, supplies, excluded, warnings, blocker, null);
    }
    private FieldRunPlan(FieldRunCrop crop, FieldRunMode mode, List<FieldRunStop> stops, List<SupplyCheck> supplies,
            Map<FieldPatchDefinition, String> excluded, List<String> warnings, String blocker, RunTransportResources resources)
    {
        this.crop = crop; this.mode = mode; this.stops = List.copyOf(stops); this.supplies = List.copyOf(supplies);
        EnumMap<FieldPatchDefinition, String> copy = new EnumMap<>(FieldPatchDefinition.class); copy.putAll(excluded);
        this.excluded = Collections.unmodifiableMap(copy); this.warnings = List.copyOf(warnings); this.blocker = blocker; this.transportResources = resources;
    }
    public FieldRunCrop getCrop() { return crop; }
    public FieldRunMode getMode() { return mode; }
    public List<FieldRunStop> getStops() { return stops; }
    public List<SupplyCheck> getSupplies() { return supplies; }
    public Map<FieldPatchDefinition, String> getExcluded() { return excluded; }
    public List<String> getWarnings() { return warnings; }
    public String getBlocker() { return blocker; }
    public RunTransportResources getTransportResources() { return transportResources; }
    public FieldRunPlan withTransports(Map<FieldPatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context)
    {
        List<FieldRunStop> routed = new ArrayList<>();
        for (FieldRunStop stop : stops)
        {
            List<VerifiedTransportResult> usable = HerbTransportSelector.select(transports.getOrDefault(stop.getPatch(), List.of()), preference);
            routed.add(stop.withTransports(usable.isEmpty() ? null : usable.get(0), usable.size() < 2 ? null : usable.get(1)));
        }
        return new FieldRunPlan(crop, mode, routed, supplies, excluded, warnings, blocker, new RunTransportResourcePlanner().evaluate(routed, context));
    }
}
