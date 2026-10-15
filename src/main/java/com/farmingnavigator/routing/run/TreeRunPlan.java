package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.loadout.SupplyCheck;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class TreeRunPlan
{
    private final TreeRunCrop crop;
    private final List<TreeRunStop> stops;
    private final List<SupplyCheck> supplies;
    private final Map<TreePatchDefinition, String> excluded;
    private final List<String> warnings;
    private final String blocker;
    TreeRunPlan(TreeRunCrop crop, List<TreeRunStop> stops, List<SupplyCheck> supplies,
            Map<TreePatchDefinition, String> excluded, List<String> warnings, String blocker)
    {
        this.crop = crop;
        this.stops = List.copyOf(stops);
        this.supplies = List.copyOf(supplies);
        EnumMap<TreePatchDefinition, String> copy = new EnumMap<>(TreePatchDefinition.class);
        copy.putAll(excluded);
        this.excluded = Collections.unmodifiableMap(copy);
        this.warnings = List.copyOf(warnings);
        this.blocker = blocker;
    }
    public TreeRunCrop getCrop() { return crop; }
    public List<TreeRunStop> getStops() { return stops; }
    public List<SupplyCheck> getSupplies() { return supplies; }
    public Map<TreePatchDefinition, String> getExcluded() { return excluded; }
    public List<String> getWarnings() { return warnings; }
    public String getBlocker() { return blocker; }
}
