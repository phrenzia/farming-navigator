package com.farmingnavigator.routing.spirit;

import java.util.Objects;
import java.util.Set;

/** Client-thread, session-only declarations. No persistence and no inferred unlocks. */
public final class SpiritTreeDeclarations
{
    public static final class Snapshot
    {
        private final String profile;
        private final long revision;
        private final Set<PlantedSpiritTree> trees;
        private Snapshot(String profile,long revision,Set<PlantedSpiritTree> trees)
        { this.profile=profile; this.revision=revision; this.trees=Set.copyOf(trees); }
        public String getProfile() { return profile; }
        public long getRevision() { return revision; }
        public Set<PlantedSpiritTree> getTrees() { return trees; }
    }
    private String profile;
    private long revision;
    private Set<PlantedSpiritTree> trees=Set.of();
    public Snapshot synchronize(String next,boolean enabled)
    {
        if (!enabled || next!=null && next.isEmpty()) { next=null; }
        if (!Objects.equals(profile,next)) { profile=next; trees=Set.of(); revision++; }
        return snapshot();
    }
    public boolean apply(String expectedProfile,long expectedRevision,Set<PlantedSpiritTree> next)
    {
        if (profile==null || !Objects.equals(profile,expectedProfile) || revision!=expectedRevision) { return false; }
        trees=Set.copyOf(next); revision++; return true;
    }
    public void clear() { profile=null; trees=Set.of(); revision++; }
    public Snapshot snapshot() { return new Snapshot(profile,revision,trees); }
}
