package com.farmingnavigator.farming.state;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.routing.run.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Immutable cross-family visit summary. Estimates never become observed readiness. */
public final class ReadinessDashboard
{
    public enum Group
    {
        DISEASED("Last observed diseased"), DEAD("Last observed dead"), HARVESTABLE("Last observed harvestable; quantity unknown"),
        COLLECTABLE("Last observed collect option; quantity unknown"),
        CHECK_HEALTH("Last observed ready for health check"), POSSIBLY_READY("Possibly ready if healthy (estimate)"),
        PLANTING("Last observed empty or weedy"), CLEARING("Last observed stump"),
        MATURE("Last observed health checked / bark harvested; yield unknown"), GROWING("Last observed growing"),
        SCARECROW("Scarecrow; not a crop"), UNKNOWN("Unobserved or unknown");
        private final String label;
        Group(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
    public static final class Entry
    {
        private final String identity, label, crop;
        private final CombinedRunKind runKind;
        private final Group group;
        private final Instant observedAt, earliest, latest;
        private Entry(CombinedRunKind runKind, String identity, String label, String crop, Group group, Instant observedAt, Instant earliest, Instant latest)
        {
            this.runKind = runKind; this.identity = identity; this.label = label; this.crop = crop; this.group = group;
            this.observedAt = observedAt; this.earliest = earliest; this.latest = latest;
        }
        public CombinedRunKind getRunKind() { return runKind; }
        public String getIdentity() { return identity; }
        public String getLabel() { return label; }
        public String getCrop() { return crop; }
        public Group getGroup() { return group; }
        public Instant getObservedAt() { return observedAt; }
        public Instant getEarliest() { return earliest; }
        public Instant getLatest() { return latest; }
    }
    private final List<Entry> entries;
    private final int excluded;
    private final Instant now;
    private ReadinessDashboard(Builder builder)
    {
        List<Entry> sorted = new ArrayList<>(builder.entries.values());
        sorted.sort(Comparator.comparing(Entry::getGroup)
                .thenComparing(Entry::getEarliest, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Entry::getIdentity));
        entries = List.copyOf(sorted); excluded = builder.excluded.size(); now = builder.now;
    }
    public List<Entry> getEntries() { return entries; }
    public int getExcludedCount() { return excluded; }
    public long getCount(Group group) { return entries.stream().filter(entry -> entry.group == group).count(); }
    /** Additional growing plots whose earliest conditional window starts within the horizon; cumulative, excluding estimates already due. */
    public long getUpcomingCount(Duration horizon)
    {
        if (horizon.isNegative() || horizon.isZero()) { return 0; }
        Instant until = now.plus(horizon);
        return entries.stream().filter(entry -> entry.earliest != null && entry.earliest.isAfter(now) && !entry.earliest.isAfter(until)).count();
    }
    public Optional<Entry> getNext()
    {
        return entries.stream().filter(entry -> entry.earliest != null && entry.earliest.isAfter(now))
                .min(Comparator.comparing(Entry::getEarliest).thenComparing(Entry::getIdentity));
    }
    /** Selected accessible patches for this run type, across its selected crop families. Not a prepared route. */
    public List<Entry> getEntries(CombinedRunKind kind)
    {
        List<Entry> selected = new ArrayList<>();
        for (Entry entry : entries) { if (entry.runKind == kind) { selected.add(entry); } }
        return List.copyOf(selected);
    }
    public long getCount(CombinedRunKind kind, Group group)
    { return entries.stream().filter(entry -> entry.runKind == kind && entry.group == group).count(); }
    public long getUpcomingCount(CombinedRunKind kind, Duration horizon)
    {
        if (horizon.isNegative() || horizon.isZero()) { return 0; }
        Instant until = now.plus(horizon);
        return entries.stream().filter(entry -> entry.runKind == kind && entry.earliest != null
                && entry.earliest.isAfter(now) && !entry.earliest.isAfter(until)).count();
    }
    public Optional<Entry> getNext(CombinedRunKind kind)
    {
        return entries.stream().filter(entry -> entry.runKind == kind && entry.earliest != null && entry.earliest.isAfter(now))
                .min(Comparator.comparing(Entry::getEarliest).thenComparing(Entry::getIdentity));
    }
    public static Builder at(Instant now) { return new Builder(now); }
    public static final class Builder
    {
        private final Instant now;
        private final Map<String, Entry> entries = new LinkedHashMap<>();
        private final Set<String> excluded = new HashSet<>();
        private Builder(Instant now) { this.now = Objects.requireNonNull(now); }
        public Builder herbs(List<HerbPatchStatus> statuses, Set<HerbPatchDefinition> selected, Map<HerbPatchDefinition, HerbPatchObservation> observations)
        {
            CombinedRunKind kind = CombinedRunKind.HERB;
            for (HerbPatchStatus status : statuses)
            {
                HerbPatchDefinition patch = status.getPatch(); if (!selected.contains(patch)) { continue; }
                String id = "HERB:" + patch.name();
                if (!status.isUnlocked()) { exclude(id); continue; }
                HerbPatchObservation observation = observations.get(patch);
                if (observation == null || observation.getObservedAt().isAfter(now)) { unknown(kind,id,patch.getDisplayName() + " herb"); continue; }
                HerbGrowthEstimate window = new HerbGrowthTimer().estimate(observation,now).orElse(null);
                add(kind,id,patch.getDisplayName() + " herb",observation.getState().getCrop().getDisplayName(),observation.getState().getCondition().name(),observation.getObservedAt(),
                        window == null ? null : window.getEarliest(),window == null ? null : window.getLatest());
            }
            return this;
        }
        public Builder trees(AccountState account, Set<TreePatchDefinition> selected, Map<TreePatchDefinition, TreePatchObservation> observations)
        {
            CombinedRunKind kind = CombinedRunKind.TREE;
            for (TreePatchDefinition patch : TreePatchDefinition.values())
            {
                if (!selected.contains(patch)) { continue; } String id = "TREE:" + patch.name();
                if (!new TreePatchAccess().missingRequirement(patch,account).isEmpty()) { exclude(id); continue; }
                String label = patch.getLabel() + " " + patch.getFamily().getPatchLabel(); TreePatchObservation observation = observations.get(patch);
                if (observation == null || observation.getObservedAt().isAfter(now) || observation.getState().getCrop().getFamily() != null
                        && observation.getState().getCrop().getFamily() != patch.getFamily()) { unknown(kind,id,label); continue; }
                TreeGrowthWindow window = new TreeGrowthTimer().estimate(observation,now).orElse(null);
                add(kind,id,label,observation.getState().getCrop().getLabel(),observation.getState().getCondition().name(),observation.getObservedAt(),
                        window == null ? null : window.getEarliest(),window == null ? null : window.getLatest());
            }
            return this;
        }
        public Builder gardens(AccountState account, Set<GardenPatchDefinition> selected, Map<GardenPatchDefinition, GardenPatchObservation> observations)
        {
            CombinedRunKind kind = CombinedRunKind.GARDEN;
            for (GardenPatchDefinition patch : GardenPatchDefinition.values())
            {
                if (!selected.contains(patch)) { continue; } String id = "GARDEN:" + patch.name();
                if (!new GardenPatchAccess().missingRequirement(patch,account).isEmpty()) { exclude(id); continue; }
                String label = patch.getLabel() + " " + patch.getFamily().getPatchLabel(); GardenPatchObservation observation = observations.get(patch);
                if (observation == null || observation.getObservedAt().isAfter(now) || observation.getState().getCrop().getFamily() != null
                        && observation.getState().getCrop().getFamily() != patch.getFamily()) { unknown(kind,id,label); continue; }
                GardenGrowthWindow window = new GardenGrowthTimer().estimate(observation,now).orElse(null);
                add(kind,id,label,observation.getState().getCrop().getLabel(),observation.getState().getCondition().name(),observation.getObservedAt(),
                        window == null ? null : window.getEarliest(),window == null ? null : window.getLatest());
            }
            return this;
        }
        public Builder fields(AccountState account, Set<FieldPatchDefinition> selected, Map<FieldPatchDefinition, FieldPatchObservation> observations)
        {
            CombinedRunKind kind = CombinedRunKind.FIELD;
            for (FieldPatchDefinition patch : FieldPatchDefinition.values())
            {
                if (!selected.contains(patch)) { continue; } String id = "FIELD:" + patch.name();
                if (!new FieldPatchAccess().missingRequirement(patch,account).isEmpty()) { exclude(id); continue; }
                String label = patch.getLabel() + " " + patch.getFamily().getLabel(); FieldPatchObservation observation = observations.get(patch);
                if (observation == null || observation.getObservedAt().isAfter(now) || observation.getState().getCrop().getFamily() != null
                        && observation.getState().getCrop().getFamily() != patch.getFamily()) { unknown(kind,id,label); continue; }
                FieldGrowthWindow window = new FieldGrowthTimer().estimate(observation,now).orElse(null);
                add(kind,id,label,observation.getState().getCrop().getLabel(),observation.getState().getCondition().name(),observation.getObservedAt(),
                        window == null ? null : window.getEarliest(),window == null ? null : window.getLatest());
            }
            return this;
        }
        private void exclude(String id) { excluded.add(id); entries.remove(id); }
        private void unknown(CombinedRunKind kind,String id,String label) { add(kind,id,label,"","UNKNOWN",null,null,null); }
        private void add(CombinedRunKind kind,String id,String label,String crop,String condition,Instant at,Instant earliest,Instant latest)
        {
            if (excluded.contains(id)) { return; }
            Group group;
            switch(condition)
            {
                case "DISEASED": group = Group.DISEASED; break;
                case "DEAD": group = Group.DEAD; break;
                case "HARVESTABLE": group = Group.HARVESTABLE; break;
                case "COLLECTABLE": group = Group.COLLECTABLE; break;
                case "CHECK_HEALTH": group = Group.CHECK_HEALTH; break;
                case "EMPTY": case "WEEDS": group = Group.PLANTING; break;
                case "STUMP": group = Group.CLEARING; break;
                case "HEALTH_CHECKED": case "HARVESTED": group = Group.MATURE; break;
                case "GROWING": group = earliest != null && !now.isBefore(earliest) ? Group.POSSIBLY_READY : Group.GROWING; break;
                case "SCARECROW": group = Group.SCARECROW; crop = ""; break;
                default: group = Group.UNKNOWN; crop = ""; break;
            }
            entries.put(id,new Entry(kind,id,label,crop,group,at,earliest,latest));
        }
        public ReadinessDashboard build() { return new ReadinessDashboard(this); }
    }
}
