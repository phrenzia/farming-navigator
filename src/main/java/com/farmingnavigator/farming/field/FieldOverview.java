package com.farmingnavigator.farming.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.run.FieldPatchAccess;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Visit summary for selected accessible plots, preserving historical versus estimated evidence. */
public final class FieldOverview
{
    public enum Group
    {
        DISEASED("Last observed diseased"), DEAD("Last observed dead"), HARVESTABLE("Last observed harvestable; quantity unknown"), COLLECTABLE("Last observed collect option; quantity unknown"),
        POSSIBLY_READY("Possibly harvestable if healthy (estimate)"), PLANTING("Last observed empty or weedy"),
        GROWING("Last observed growing"), SCARECROW("Scarecrow present; not a crop"), UNKNOWN("Unobserved or unknown");
        private final String label;
        Group(String label) { this.label=label; }
        public String getLabel() { return label; }
    }
    public static final class Entry
    {
        private final FieldPatchDefinition patch;
        private final Group group;
        private final FieldPatchObservation observation;
        private Entry(FieldPatchDefinition patch,Group group,FieldPatchObservation observation)
        {
            this.patch=patch; this.group=group; this.observation=observation;
        }
        public FieldPatchDefinition getPatch() { return patch; }
        public Group getGroup() { return group; }
        public FieldPatchObservation getObservation() { return observation; }
    }
    private final List<Entry> entries;
    private final Map<Group,Integer> counts;
    private final int excluded;
    private FieldOverview(List<Entry> entries,Map<Group,Integer> counts,int excluded)
    {
        this.entries=List.copyOf(entries); this.counts=Collections.unmodifiableMap(new EnumMap<>(counts)); this.excluded=excluded;
    }
    public static FieldOverview create(AccountState account,Set<FieldPatchDefinition> selected,FieldFamily family,
            Map<FieldPatchDefinition,FieldPatchObservation> observations,Instant now)
    {
        List<Entry> entries=new ArrayList<>(); Map<Group,Integer> counts=new EnumMap<>(Group.class); int excluded=0;
        for(FieldPatchDefinition patch:FieldPatchDefinition.values())
        {
            if(!selected.contains(patch) || patch.getFamily()!=family) { continue; }
            if(!new FieldPatchAccess().missingRequirement(patch,account).isEmpty()) { excluded++; continue; }
            FieldPatchObservation observation=observations.get(patch);
            if(observation!=null && (observation.getObservedAt().isAfter(now) || observation.getState().getCrop().getFamily()!=null
                    && observation.getState().getCrop().getFamily()!=family)) { observation=null; }
            Group group=classify(observation,now); entries.add(new Entry(patch,group,observation)); counts.merge(group,1,Integer::sum);
        }
        entries.sort(Comparator.comparing(Entry::getGroup).thenComparing(Entry::getPatch));
        return new FieldOverview(entries,counts,excluded);
    }
    private static Group classify(FieldPatchObservation observation,Instant now)
    {
        if(observation==null) { return Group.UNKNOWN; }
        switch(observation.getState().getCondition())
        {
            case DISEASED:return Group.DISEASED;
            case DEAD:return Group.DEAD;
            case HARVESTABLE:return Group.HARVESTABLE;
            case COLLECTABLE:return Group.COLLECTABLE;
            case EMPTY:
            case WEEDS:return Group.PLANTING;
            case SCARECROW:return Group.SCARECROW;
            case GROWING:return new FieldGrowthTimer().estimate(observation,now)
                    .map(window -> now.isBefore(window.getEarliest())?Group.GROWING:Group.POSSIBLY_READY).orElse(Group.GROWING);
            default:return Group.UNKNOWN;
        }
    }
    public List<Entry> getEntries() { return entries; }
    public int getCount(Group group) { return counts.getOrDefault(group,0); }
    public int getExcludedCount() { return excluded; }
}
