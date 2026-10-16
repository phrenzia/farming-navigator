package com.farmingnavigator.analytics;

import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Bounded local profile history. Only ended checklists are saved, once per session identity. */
public final class RunHistoryStore
{
    public static final String KEY = "runHistoryV1";
    public static final int LIMIT = 20;
    private final ObservationPersistenceBackend backend;
    private final List<RunMetricsSnapshot> entries = new ArrayList<>();
    private String profile, lastSeen;
    public RunHistoryStore(ObservationPersistenceBackend backend) { this.backend = backend; }
    public enum Kind
    {
        HERB("Herb run"), TREE("Tree run"), GARDEN("Bush/cactus run"), FIELD("Field run");
        private final String label;
        Kind(String label) { this.label = label; }
        @Override public String toString() { return label; }
        public static Kind fromLabel(String label)
        {
            for (Kind kind : values()) { if (kind.label.equals(label)) { return kind; } }
            return null;
        }
    }
    public void loadProfile(String next, Instant now)
    {
        if (next != null && next.isEmpty()) { next = null; }
        if (Objects.equals(profile, next)) { return; }
        profile = next; lastSeen = null; entries.clear();
        if (profile == null) { return; }
        String encoded = backend.read(profile, KEY);
        if (encoded == null || encoded.length() > 8192) { return; }
        String[] lines = encoded.split("\n", -1);
        if (!"1".equals(lines[0])) { return; }
        Set<String> seen = new HashSet<>();
        for (int i = 1; i < lines.length; i++)
        {
            RunMetricsSnapshot value = decode(lines[i], now);
            if (value != null && seen.add(identity(value))) { entries.add(value); }
        }
        trim();
    }
    public boolean observe(RunMetricsSnapshot metrics, boolean enabled, Instant now)
    {
        if (profile == null || !valid(metrics, now)) { return false; }
        String id = identity(metrics);
        if (id.equals(lastSeen)) { return false; }
        lastSeen = id;
        if (!enabled || entries.stream().anyMatch(entry -> identity(entry).equals(id))) { return false; }
        entries.add(metrics); trim(); save(); return true;
    }
    public List<RunMetricsSnapshot> snapshot() { return List.copyOf(entries); }
    public void clear()
    {
        entries.clear(); if (profile != null) { save(); }
        // Keep lastSeen so refreshing the last ended checklist cannot restore a deliberately cleared entry.
    }
    public void detach() { profile = lastSeen = null; entries.clear(); }
    private void trim()
    {
        entries.sort(Comparator.comparing(RunMetricsSnapshot::getFinishedAt).reversed());
        if (entries.size() > LIMIT) { entries.subList(LIMIT, entries.size()).clear(); }
    }
    private String identity(RunMetricsSnapshot value) { return Kind.fromLabel(value.getRunName()).name() + "|" + value.getStartedAt(); }
    static boolean valid(RunMetricsSnapshot value, Instant now)
    {
        return value != null && !value.isRecording() && Kind.fromLabel(value.getRunName()) != null
                && value.getStartedAt() != null && value.getFinishedAt() != null
                && !value.getFinishedAt().isBefore(value.getStartedAt()) && !value.getFinishedAt().isAfter(now)
                && Duration.between(value.getStartedAt(), value.getFinishedAt()).compareTo(Duration.ofDays(30)) <= 0
                && value.getXpGained() >= 0 && value.getXpGained() <= Integer.MAX_VALUE
                && (value.isXpKnown() || value.getXpGained() == 0);
    }
    private RunMetricsSnapshot decode(String line, Instant now)
    {
        if (line.length() > 256) { return null; }
        try
        {
            String[] fields = line.split("[|]", -1);
            if (fields.length != 6 || !("0".equals(fields[3]) || "1".equals(fields[3]))
                    || !("0".equals(fields[4]) || "1".equals(fields[4]))) { return null; }
            Kind kind = Kind.valueOf(fields[0]); Instant start = Instant.parse(fields[1]), end = Instant.parse(fields[2]);
            RunMetricsSnapshot value = new RunMetricsSnapshot(kind.label, false, "1".equals(fields[3]), "1".equals(fields[4]),
                    Long.parseLong(fields[5]), Duration.between(start, end), start, end);
            return valid(value, now) ? value : null;
        }
        catch (java.time.DateTimeException | IllegalArgumentException ex) { return null; }
    }
    private void save()
    {
        StringBuilder encoded = new StringBuilder("1");
        for (RunMetricsSnapshot entry : entries)
        {
            encoded.append('\n').append(Kind.fromLabel(entry.getRunName()).name()).append('|').append(entry.getStartedAt())
                    .append('|').append(entry.getFinishedAt()).append('|').append(entry.isCompleted() ? '1' : '0')
                    .append('|').append(entry.isXpKnown() ? '1' : '0').append('|').append(entry.getXpGained());
        }
        backend.write(profile, KEY, encoded.toString());
    }
}
