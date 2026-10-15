package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.loadout.SupplyCheck;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;

public final class HerbRunPlan
{
    private final HerbRunCrop crop;
    private final List<HerbRunStop> stops;
    private final List<SupplyCheck> supplies;
    private final List<SupplyCheck> plannedSupplies;
    private final Map<HerbPatchDefinition, String> excluded;
    private final String blocker;
    private final RunTransportResources transportResources;
    private final HerbRunCareResources careResources;

    HerbRunPlan(HerbRunCrop crop, List<HerbRunStop> stops, List<SupplyCheck> supplies,
            Map<HerbPatchDefinition, String> excluded, String blocker)
    {
        this(crop, stops, supplies, excluded, blocker, null, null);
    }

    private HerbRunPlan(HerbRunCrop crop, List<HerbRunStop> stops, List<SupplyCheck> supplies,
            Map<HerbPatchDefinition, String> excluded, String blocker, RunTransportResources transportResources,
            HerbRunCareResources careResources)
    {
        this.crop = crop;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
        this.plannedSupplies = Collections.unmodifiableList(new ArrayList<>(supplies));
        this.supplies = Collections.unmodifiableList(plannedSupplies.stream().filter(check -> careResources == null
                || careResources.getCure() != HerbRunCure.CURE_PLANT || check.getRequirement().getItemId() != ItemID.PLANT_CURE)
                .collect(Collectors.toList()));
        EnumMap<HerbPatchDefinition, String> copy = new EnumMap<>(HerbPatchDefinition.class);
        copy.putAll(excluded);
        this.excluded = Collections.unmodifiableMap(copy);
        this.blocker = blocker;
        this.transportResources = transportResources;
        this.careResources = careResources;
    }

    public HerbRunCrop getCrop() { return crop; }
    public List<HerbRunStop> getStops() { return stops; }
    public List<SupplyCheck> getSupplies() { return supplies; }
    public Map<HerbPatchDefinition, String> getExcluded() { return excluded; }
    public String getBlocker() { return blocker; }
    public RunTransportResources getTransportResources() { return transportResources; }
    public HerbRunCareResources getCareResources() { return careResources; }
    public HerbRunCure getCure() { return careResources == null ? HerbRunCure.PLANT_CURE : careResources.getCure(); }

    public HerbRunPlan withCareResources(HerbRunCareResources resources)
    {
        return new HerbRunPlan(crop, stops, plannedSupplies, excluded, blocker, transportResources, resources);
    }

    public HerbRunPlan withTransportResources(RunTransportResources resources)
    {
        return new HerbRunPlan(crop, stops, plannedSupplies, excluded, blocker, resources, careResources);
    }
}
