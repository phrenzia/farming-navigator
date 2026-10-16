package com.farmingnavigator.routing.run;

import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.routing.requirement.RequirementResult;
import java.util.List;

/** Preparation worksheet only; component sequences retain their existing order and guidance. */
public final class CombinedRunPlan
{
    private final List<String> components, warnings, blockers;
    private final List<RunTravelStop> stops;
    private final List<SupplyCheck> supplies;
    private final List<RequirementResult> careChecks;
    private final RunTransportResources resources;
    CombinedRunPlan(List<String> components, List<RunTravelStop> stops, List<SupplyCheck> supplies,
            List<RequirementResult> careChecks, RunTransportResources resources, List<String> warnings, List<String> blockers)
    {
        this.components = List.copyOf(components); this.stops = List.copyOf(stops); this.supplies = List.copyOf(supplies);
        this.careChecks = List.copyOf(careChecks); this.resources = resources; this.warnings = List.copyOf(warnings); this.blockers = List.copyOf(blockers);
    }
    public List<String> getComponents() { return components; }
    public List<RunTravelStop> getStops() { return stops; }
    public List<SupplyCheck> getSupplies() { return supplies; }
    public List<RequirementResult> getCareChecks() { return careChecks; }
    public RunTransportResources getResources() { return resources; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getBlockers() { return blockers; }
}
