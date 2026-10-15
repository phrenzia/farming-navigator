package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Selected, unlocked herb patches summarized without converting estimates into observations. */
public final class HerbOverview
{
    public enum Group
    {
        DISEASED("Last observed diseased"), DEAD("Last observed dead"), READY("Last observed ready"),
        POSSIBLY_READY("Possibly ready if healthy (estimate)"), PLANTING("Last observed empty or weedy"),
        GROWING("Last observed growing"), UNKNOWN("Unobserved or unknown");

        private final String label;
        Group(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static final class Entry
    {
        private final HerbPatchDefinition patch;
        private final Group group;
        private final HerbPatchObservation observation;

        private Entry(HerbPatchDefinition patch, Group group, HerbPatchObservation observation)
        {
            this.patch = patch;
            this.group = group;
            this.observation = observation;
        }

        public HerbPatchDefinition getPatch() { return patch; }
        public Group getGroup() { return group; }
        public HerbPatchObservation getObservation() { return observation; }
    }

    private final List<Entry> entries;
    private final Map<Group, Integer> counts;
    private final int locked;

    private HerbOverview(List<Entry> entries, Map<Group, Integer> counts, int locked)
    {
        this.entries = Collections.unmodifiableList(entries);
        this.counts = Collections.unmodifiableMap(counts);
        this.locked = locked;
    }

    public static HerbOverview create(List<HerbPatchStatus> statuses, Set<HerbPatchDefinition> selected,
            Map<HerbPatchDefinition, HerbPatchObservation> observations, Instant now)
    {
        List<Entry> entries = new ArrayList<>();
        Map<Group, Integer> counts = new EnumMap<>(Group.class);
        Set<HerbPatchDefinition> seen = EnumSet.noneOf(HerbPatchDefinition.class);
        int locked = 0;
        for (HerbPatchStatus status : statuses)
        {
            HerbPatchDefinition patch = status.getPatch();
            if (!selected.contains(patch) || !seen.add(patch)) { continue; }
            if (!status.isUnlocked()) { locked++; continue; }
            HerbPatchObservation observation = observations.get(patch);
            if (observation != null && observation.getObservedAt().isAfter(now)) { observation = null; }
            Group group = classify(observation, now);
            counts.merge(group, 1, Integer::sum);
            entries.add(new Entry(patch, group, observation));
        }
        entries.sort(Comparator.comparing(Entry::getGroup).thenComparing(Entry::getPatch));
        return new HerbOverview(entries, counts, locked);
    }

    private static Group classify(HerbPatchObservation observation, Instant now)
    {
        if (observation == null) { return Group.UNKNOWN; }
        switch (observation.getState().getCondition())
        {
            case DISEASED: return Group.DISEASED;
            case DEAD: return Group.DEAD;
            case HARVESTABLE: return Group.READY;
            case EMPTY:
            case WEEDS: return Group.PLANTING;
            case GROWING:
                return new HerbGrowthTimer().estimate(observation, now)
                        .map(estimate -> now.isBefore(estimate.getEarliest()) ? Group.GROWING : Group.POSSIBLY_READY)
                        .orElse(Group.GROWING);
            default: return Group.UNKNOWN;
        }
    }

    public List<Entry> getEntries() { return entries; }
    public int getCount(Group group) { return counts.getOrDefault(group, 0); }
    public int getLockedCount() { return locked; }
}
