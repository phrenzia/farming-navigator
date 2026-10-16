package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.loadout.SupplyCheck;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.transport.VerifiedTransportResult;

public final class TreeRunPlan
{
    private final TreeRunCrop crop;
    private final TreeRunCompost compost;
    private final TreeRunCure cure;
    private final int potentialCures;
    private final HerbRunCareResources careResources;
    private final List<TreeRunStop> stops;
    private final List<SupplyCheck> supplies;
    private final Map<TreePatchDefinition, String> excluded;
    private final List<String> warnings;
    private final String blocker;
    private final RunTransportResources transportResources;
    TreeRunPlan(TreeRunCrop crop, List<TreeRunStop> stops, List<SupplyCheck> supplies,
            Map<TreePatchDefinition, String> excluded, List<String> warnings, String blocker)
    {
        this(crop, stops, supplies, excluded, warnings, blocker, TreeRunCompost.NONE, TreeRunCure.PRUNE, 0);
    }
    TreeRunPlan(TreeRunCrop crop, List<TreeRunStop> stops, List<SupplyCheck> supplies,
            Map<TreePatchDefinition, String> excluded, List<String> warnings, String blocker, TreeRunCompost compost, TreeRunCure cure, int potentialCures)
    {
        this(crop, stops, supplies, excluded, warnings, blocker, compost, cure, potentialCures, null, null);
    }
    private TreeRunPlan(TreeRunCrop crop, List<TreeRunStop> stops, List<SupplyCheck> supplies,
            Map<TreePatchDefinition, String> excluded, List<String> warnings, String blocker, TreeRunCompost compost, TreeRunCure cure, int potentialCures, RunTransportResources resources, HerbRunCareResources careResources)
    {
        this.compost = compost; this.cure = cure; this.potentialCures = potentialCures; this.careResources = careResources;
        this.crop = crop;
        this.stops = List.copyOf(stops);
        this.supplies = List.copyOf(supplies);
        EnumMap<TreePatchDefinition, String> copy = new EnumMap<>(TreePatchDefinition.class);
        copy.putAll(excluded);
        this.excluded = Collections.unmodifiableMap(copy);
        this.warnings = List.copyOf(warnings);
        this.blocker = blocker;
        this.transportResources = resources;
    }
    public TreeRunCompost getCompost() { return compost; }
    public TreeRunCure getCure() { return cure; }
    public HerbRunCareResources getCareResources() { return careResources; }
    public TreeRunCrop getCrop() { return crop; }
    public List<TreeRunStop> getStops() { return stops; }
    public List<SupplyCheck> getSupplies() { return supplies; }
    public Map<TreePatchDefinition, String> getExcluded() { return excluded; }
    public List<String> getWarnings() { return warnings; }
    public String getBlocker() { return blocker; }
    public RunTransportResources getTransportResources() { return transportResources; }
    public TreeRunPlan withTransports(Map<TreePatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context)
    { return route(transports,preference,context,true); }
    /** Active guidance refreshes verification without searching against already-consumed whole-run supplies. */
    public TreeRunPlan refreshTransports(Map<TreePatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context)
    { return route(transports,preference,context,false); }
    private TreeRunPlan route(Map<TreePatchDefinition, List<VerifiedTransportResult>> transports,
            HerbTransportPreference preference, RequirementContext context, boolean repair)
    {
        List<TreeRunStop> routed = new ArrayList<>();
        boolean previousFossil = false;
        for (TreeRunStop stop : stops)
        {
            boolean fossil = stop.getPatch() == TreePatchDefinition.FOSSIL_ISLAND_EAST
                    || stop.getPatch() == TreePatchDefinition.FOSSIL_ISLAND_MIDDLE
                    || stop.getPatch() == TreePatchDefinition.FOSSIL_ISLAND_WEST;
            List<VerifiedTransportResult> usable = repair
                    ? HerbTransportSelector.select(transports.getOrDefault(stop.getPatch(), List.of()), preference)
                    : HerbTransportSelector.selectForRefresh(transports.getOrDefault(stop.getPatch(), List.of()), preference,stop.getRecommended());
            routed.add(fossil && previousFossil ? stop.withLocalWalk()
                    : stop.withTransports(usable.isEmpty() ? null : usable.get(0), usable.size() < 2 ? null : usable.get(1)));
            previousFossil = fossil;
        }
        HerbRunCareResources care = HerbRunCareResources.evaluateCounts(stops.stream().filter(TreeRunStop::isReplant).count(),
                potentialCures, cure.asHerbCure(), HerbRunCompost.valueOf(compost.name()),
                context != null && context.getConfig() != null && context.getConfig().treeAshTomeRead(), context);
        RunTransportResources resources;
        if (repair && preference==HerbTransportPreference.SHARED_BUDGET)
        {
            SharedTransportRepair.Result<TreeRunStop> repaired=new SharedTransportRepair().repair(routed,
                    stop -> transports.getOrDefault(stop.getPatch(),List.of()),
                    (stop,primary) -> stop.withTransports(primary,HerbTransportSelector.fallback(transports.getOrDefault(stop.getPatch(),List.of()),primary)),
                    context,care.getRunes());
            routed=repaired.getStops(); resources=repaired.getResources();
        }
        else { resources=new RunTransportResourcePlanner().evaluate(routed,context,care.getRunes()); }
        return new TreeRunPlan(crop, routed, supplies, excluded, warnings, blocker, compost, cure, potentialCures,
                resources, care);
    }
}
