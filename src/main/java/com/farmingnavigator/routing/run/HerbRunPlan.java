package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.loadout.SupplyCheck;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HerbRunPlan
{
    private final HerbRunCrop crop;
    private final List<HerbRunStop> stops;
    private final List<SupplyCheck> supplies;
    private final Map<HerbPatchDefinition, String> excluded;
    private final String blocker;
    private final RunTransportResources transportResources;

    HerbRunPlan(HerbRunCrop crop, List<HerbRunStop> stops, List<SupplyCheck> supplies,
            Map<HerbPatchDefinition, String> excluded, String blocker)
    {
        this(crop, stops, supplies, excluded, blocker, null);
    }

    private HerbRunPlan(HerbRunCrop crop, List<HerbRunStop> stops, List<SupplyCheck> supplies,
            Map<HerbPatchDefinition, String> excluded, String blocker, RunTransportResources transportResources)
    {
        this.crop = crop;
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
        this.supplies = Collections.unmodifiableList(new ArrayList<>(supplies));
        EnumMap<HerbPatchDefinition, String> copy = new EnumMap<>(HerbPatchDefinition.class);
        copy.putAll(excluded);
        this.excluded = Collections.unmodifiableMap(copy);
        this.blocker = blocker;
        this.transportResources = transportResources;
    }

    public HerbRunCrop getCrop() { return crop; }
    public List<HerbRunStop> getStops() { return stops; }
    public List<SupplyCheck> getSupplies() { return supplies; }
    public Map<HerbPatchDefinition, String> getExcluded() { return excluded; }
    public String getBlocker() { return blocker; }
    public RunTransportResources getTransportResources() { return transportResources; }

    public HerbRunPlan withTransportResources(RunTransportResources resources)
    {
        return new HerbRunPlan(crop, stops, supplies, excluded, blocker, resources);
    }
}
