package com.farmingnavigator.guided.combined;

import com.farmingnavigator.routing.run.CombinedRunKind;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Session-only checklist sequencing. Starting another component always needs an explicit command. */
public final class CombinedGuidanceSession
{
    public enum State { INACTIVE, RUNNING, BETWEEN, COMPLETE }
    public enum Progress { INACTIVE, ACTIVE, COMPLETE }
    public enum Command { BEGIN, NEXT, END }

    public static final class Snapshot
    {
        private final State state;
        private final List<CombinedRunKind> sequence;
        private final int index;
        private final long revision;

        private Snapshot(State state, List<CombinedRunKind> sequence, int index, long revision)
        { this.state = state; this.sequence = sequence; this.index = index; this.revision = revision; }
        public State getState() { return state; }
        public List<CombinedRunKind> getSequence() { return sequence; }
        public int getIndex() { return index; }
        public long getRevision() { return revision; }
        public CombinedRunKind getCurrent() { return sequence.isEmpty() ? null : sequence.get(index); }
        public CombinedRunKind getNext() { return index + 1 < sequence.size() ? sequence.get(index + 1) : null; }
        public int getFinished() { return state == State.COMPLETE ? sequence.size() : state == State.BETWEEN ? index + 1 : index; }
        public boolean isActive() { return state == State.RUNNING || state == State.BETWEEN; }
    }

    private State state = State.INACTIVE;
    private List<CombinedRunKind> sequence = List.of();
    private String profile;
    private int index;
    private long revision;

    public Snapshot snapshot() { return new Snapshot(state, sequence, index, revision); }

    public boolean begin(String currentProfile, Set<CombinedRunKind> selected, Predicate<CombinedRunKind> start)
    {
        if (snapshot().isActive() || currentProfile == null || currentProfile.isBlank() || selected == null
                || selected.size() < 2 || selected.stream().anyMatch(Objects::isNull)) { return false; }
        List<CombinedRunKind> ordered = Arrays.stream(CombinedRunKind.values()).filter(selected::contains).collect(Collectors.toList());
        if (!start.test(ordered.get(0))) { return false; }
        sequence = List.copyOf(ordered); profile = currentProfile; index = 0; state = State.RUNNING; revision++;
        return true;
    }

    public boolean next(String currentProfile, Predicate<CombinedRunKind> start)
    {
        if (state != State.BETWEEN || !Objects.equals(profile, currentProfile)) { return false; }
        if (!start.test(sequence.get(index + 1))) { return false; }
        index++; state = State.RUNNING; revision++; return true;
    }

    public void observe(String currentProfile, Map<CombinedRunKind, Progress> progress)
    {
        if (state == State.INACTIVE) { return; }
        if (!Objects.equals(profile, currentProfile)) { clear(); return; }
        if (!snapshot().isActive()) { return; }
        CombinedRunKind current = sequence.get(index);
        if (progress.entrySet().stream()
                .anyMatch(entry -> entry.getKey() != current && entry.getValue() == Progress.ACTIVE))
        { clear(); return; }
        Progress observed = progress.getOrDefault(current, Progress.INACTIVE);
        if (observed == Progress.INACTIVE) { clear(); }
        else if (state == State.RUNNING && observed == Progress.COMPLETE)
        { state = index + 1 == sequence.size() ? State.COMPLETE : State.BETWEEN; revision++; }
    }

    /** Ending the sequence leaves the current individual checklist under its existing controls. */
    public void clear()
    { state = State.INACTIVE; sequence = List.of(); profile = null; index = 0; revision++; }
}
