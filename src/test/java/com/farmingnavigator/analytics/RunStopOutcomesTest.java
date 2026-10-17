package com.farmingnavigator.analytics;

import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunStopOutcomesTest
{
    private final Instant now = Instant.parse("2026-10-17T18:00:00Z");
    private final Map<String, String> records = new HashMap<>();
    private int writes;
    private final ObservationPersistenceBackend backend = new ObservationPersistenceBackend() {
        public String read(String profile, String key) { assertEquals("runHistoryV1", key); return records.get(profile); }
        public void write(String profile, String key, String value) { records.put(profile, value); writes++; }
    };
    private RunHistoryContext context() { return RunHistoryContext.of(RunHistoryStore.Kind.FIELD, "POTATO", 3); }
    private RunMetricsSnapshot entry(RunHistoryStore.Kind kind, String crop, int ago, boolean complete)
    {
        RunMetricsTracker tracker = new RunMetricsTracker();
        tracker.start(kind.toString(), 100, now.minusSeconds(ago).plusNanos(123), RunHistoryContext.of(kind, crop, 3));
        assertTrue(tracker.sampleStops(complete ? 2 : 1, 1, 1));
        tracker.finish(250, now.minusSeconds(ago - 60).plusNanos(123), complete);
        return tracker.snapshot(now);
    }

    @Test public void invalidOrOverflowedCountsCannotDescribeAValidChecklist()
    {
        for (int[] values : new int[][]{{-1, 0, 0}, {0, -1, 0}, {0, 0, -1}, {1, 2, 0}})
        {
            try { RunStopOutcomes.of(values[0], values[1], values[2]); fail("Invalid counts"); }
            catch (IllegalArgumentException expected) { }
        }
        assertTrue(RunStopOutcomes.UNKNOWN.isValidFor(RunHistoryContext.UNKNOWN, true));
        assertFalse(RunStopOutcomes.of(0, 0, 0).isValidFor(RunHistoryContext.UNKNOWN, false));
        assertTrue(RunStopOutcomes.of(0, 0, 0).isValidFor(context(), false));
        assertFalse(RunStopOutcomes.of(0, 0, 0).isValidFor(context(), true));
        assertTrue(RunStopOutcomes.of(2, 1, 1).isValidFor(context(), true));
        assertTrue(RunStopOutcomes.of(0, 0, 3).isValidFor(context(), true));
        assertFalse(RunStopOutcomes.of(3, 0, 1).isValidFor(context(), false));
        RunStopOutcomes overflow = RunStopOutcomes.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(4294967294L, overflow.getFinished()); assertFalse(overflow.isValidFor(context(), false));
    }

    @Test public void trackerRetainsMonotonicPartialOutcomesAndFreezesEndedSnapshots()
    {
        RunMetricsTracker tracker = new RunMetricsTracker(); tracker.start("Field run", 100, now, context());
        assertFalse(tracker.snapshot(now).getStopOutcomes().isKnown());
        assertTrue(tracker.sampleStops(0, 0, 0));
        RunMetricsSnapshot before = tracker.snapshot(now);
        assertTrue(tracker.sampleStops(1, 1, 0));
        assertFalse(tracker.sampleStops(0, 0, 0)); assertFalse(tracker.sampleStops(1, 0, 0));
        assertFalse(tracker.sampleStops(4, 1, 0)); assertFalse(tracker.sampleStops(1, 2, 0));
        assertTrue(tracker.sampleStops(1, 1, 1));
        tracker.finish(250, now.plusSeconds(60), false);
        assertFalse(tracker.sampleStops(2, 1, 1));
        RunMetricsSnapshot after = tracker.snapshot(now.plusSeconds(300));
        assertEquals(0, before.getStopOutcomes().getCompleted());
        assertEquals(1, after.getStopOutcomes().getCompleted()); assertEquals(1, after.getStopOutcomes().getManual());
        assertEquals(1, after.getStopOutcomes().getSkipped());
        assertTrue(after.getStopOutcomes().describe(after.getContext()).contains("1 unfinished"));
        tracker.start("Herb run", 250, now.plusSeconds(61));
        assertFalse(tracker.sampleStops(0, 0, 0)); assertFalse(tracker.snapshot(now.plusSeconds(62)).getStopOutcomes().isKnown());
        tracker.clear(); assertNull(tracker.snapshot(now));
    }

    @Test public void allFourKindsRoundTripCompletedAndStoppedOutcomesWithoutChangingXpOrTimestamps()
    {
        RunHistoryStore store = new RunHistoryStore(backend); store.loadProfile("A", now);
        Map<RunHistoryStore.Kind, String> crops = Map.of(RunHistoryStore.Kind.HERB, "RANARR", RunHistoryStore.Kind.TREE, "OAK",
                RunHistoryStore.Kind.GARDEN, "REDBERRIES", RunHistoryStore.Kind.FIELD, "POTATO");
        for (RunHistoryStore.Kind kind : RunHistoryStore.Kind.values())
        { assertTrue(store.observe(entry(kind, crops.get(kind), 600 + kind.ordinal() * 120, kind.ordinal() % 2 == 0), true, now)); }
        assertTrue(records.get("A").startsWith("3\n"));
        store.detach(); store.loadProfile("A", now); assertEquals(4, store.snapshot().size());
        for (RunMetricsSnapshot saved : store.snapshot())
        {
            assertEquals(saved.isCompleted() ? 2 : 1, saved.getStopOutcomes().getCompleted());
            assertEquals(1, saved.getStopOutcomes().getManual()); assertEquals(1, saved.getStopOutcomes().getSkipped());
            assertEquals(150, saved.getXpGained()); assertEquals(Duration.ofSeconds(60), saved.getElapsed());
            assertEquals(123, saved.getStartedAt().getNano()); assertEquals(123, saved.getFinishedAt().getNano());
            assertTrue(saved.getStopOutcomes().isValidFor(saved.getContext(), saved.isCompleted()));
            assertFalse(store.observe(saved, true, now));
        }
    }

    @Test public void legacyFormatsRemainUnknownUntilAnExplicitNewSampleAndMigrateOnlyOnWrite()
    {
        String old = "FIELD|" + now.minusSeconds(180) + "|" + now.minusSeconds(120) + "|1|1|150";
        records.put("one", "1\n" + old); records.put("two", "2\n" + old + "|POTATO|3");
        RunHistoryStore store = new RunHistoryStore(backend);
        for (String profile : List.of("one", "two"))
        {
            store.loadProfile(profile, now); assertEquals(1, store.snapshot().size());
            assertFalse(store.snapshot().get(0).getStopOutcomes().isKnown());
            assertEquals(profile.equals("two"), store.snapshot().get(0).getContext().isKnown());
        }
        assertEquals(0, writes);
        assertTrue(store.observe(entry(RunHistoryStore.Kind.FIELD, "POTATO", 100, false), true, now));
        assertTrue(records.get("two").startsWith("3\n")); assertTrue(records.get("two").contains("|-1|-1|-1"));
        store.detach(); store.loadProfile("two", now); assertEquals(2, store.snapshot().size());
        assertEquals(1, store.snapshot().stream().filter(value -> value.getStopOutcomes().isKnown()).count());
        store.clear(); assertEquals("3", records.get("two")); store.detach(); store.loadProfile("two", now); assertTrue(store.snapshot().isEmpty());
        store.loadProfile("one", now); assertFalse(store.snapshot().get(0).getStopOutcomes().isKnown());
    }

    @Test public void malformedVersionThreeCountsCannotBeHiddenByDuplicateIdentity()
    {
        String prefix = "FIELD|" + now.minusSeconds(120) + "|" + now.minusSeconds(60) + "|1|1|150|";
        List<String> invalid = List.of("POTATO|3|1|0|1", "POTATO|3|3|4|0", "POTATO|3|-1|0|0", "POTATO|3|3|0|-1",
                "POTATO|3|3|0|1", "POTATO|3|2147483647|2147483647|2147483647", "POTATO|3|2147483648|0|0",
                "-|0|0|0|0", "POTATO|3|-1|-1|0", "POTATO|3|2|1", "POTATO|3|2|1|1|extra");
        RunHistoryStore store = new RunHistoryStore(backend);
        for (int i = 0; i < invalid.size(); i++)
        {
            records.put("bad" + i, "3\n" + prefix + invalid.get(i)); store.loadProfile("bad" + i, now);
            assertTrue(invalid.get(i), store.snapshot().isEmpty());
        }
        records.put("future-version", "4\n" + prefix + "POTATO|3|2|1|1"); store.loadProfile("future-version", now); assertTrue(store.snapshot().isEmpty());
        records.put("good", "3\n" + prefix + "POTATO|3|2|1|1"); store.loadProfile("good", now); assertEquals(1, store.snapshot().size());
        records.put("unknown", "3\n" + prefix + "-|0|-1|-1|-1"); store.loadProfile("unknown", now); assertEquals(1, store.snapshot().size());
        assertFalse(store.snapshot().get(0).getStopOutcomes().isKnown());
    }

    @Test public void completedRecordsNeedEveryPlannedStopAccountedForAndRealZeroIsNotUnknown()
    {
        RunMetricsSnapshot invalid = new RunMetricsSnapshot("Field run", false, true, true, 0, Duration.ofSeconds(60),
                now.minusSeconds(120), now.minusSeconds(60), context(), RunStopOutcomes.of(1, 1, 1));
        assertFalse(RunHistoryStore.valid(invalid, now));
        RunMetricsTracker tracker = new RunMetricsTracker(); tracker.start("Field run", -1, now.minusSeconds(60), context());
        tracker.sampleStops(0, 0, 0); tracker.finish(-1, now, false);
        RunMetricsSnapshot stopped = tracker.snapshot(now);
        assertTrue(RunHistoryStore.valid(stopped, now)); assertTrue(stopped.getStopOutcomes().isKnown());
        assertEquals(0, stopped.getStopOutcomes().getCompleted()); assertFalse(stopped.isXpKnown());
        assertTrue(stopped.getStopOutcomes().describe(stopped.getContext()).contains("3 unfinished"));
        RunMetricsSnapshot legacy = new RunMetricsSnapshot("Field run", false, true, true, 0, Duration.ZERO);
        assertFalse(legacy.getStopOutcomes().isKnown());
    }
}
