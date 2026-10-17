package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunStopOutcomeSummaryTest
{
    private final Instant now = Instant.parse("2026-10-17T18:00:00Z");

    private RunMetricsSnapshot entry(int ago, RunHistoryStore.Kind kind, String crop, boolean complete, RunStopOutcomes stops)
    {
        Instant end = now.minusSeconds(ago);
        String label = kind == RunHistoryStore.Kind.FIELD ? "Field run" : "Tree run";
        return new RunMetricsSnapshot(label, false, complete, false, 0, Duration.ofSeconds(30), end.minusSeconds(30), end,
                RunHistoryContext.of(kind, crop, 3), stops);
    }
    private RunMetricsSnapshot field(int ago, boolean complete, RunStopOutcomes stops)
    { return entry(ago, RunHistoryStore.Kind.FIELD, "POTATO", complete, stops); }
    private RunHistorySummary summary(List<RunMetricsSnapshot> records)
    { return RunHistorySummary.create(records, RunHistoryPeriod.ALL, null, now); }

    @Test public void partialAndCompletedCountsIncludeManualSubsetAndIgnoreUnknownXp()
    {
        RunStopOutcomeSummary result = summary(List.of(field(1, false, RunStopOutcomes.of(1, 1, 1)),
                field(2, true, RunStopOutcomes.of(2, 1, 1)), field(3, true, RunStopOutcomes.UNKNOWN))).getStopOutcomes();
        assertEquals(2, result.getKnownCount()); assertEquals(1, result.getUnknownCount());
        assertEquals(3, result.getCompleted()); assertEquals(2, result.getManual());
        assertEquals(2, result.getSkipped()); assertEquals(1, result.getUnfinished());
        assertTrue(result.describe().contains("3 completed (2 completed manually), 2 skipped, 1 unfinished"));
        assertTrue(result.describeCoverage().contains("2 / 3 saved checklists"));
    }

    @Test public void knownZeroWorkIsDistinctFromLegacyOrEmptySamples()
    {
        RunStopOutcomeSummary zero = summary(List.of(field(1, false, RunStopOutcomes.of(0, 0, 0)))).getStopOutcomes();
        assertEquals(1, zero.getKnownCount()); assertEquals(3, zero.getUnfinished());
        assertTrue(zero.describe().contains("0 completed"));
        for (List<RunMetricsSnapshot> entries : List.of(List.<RunMetricsSnapshot>of(), List.of(field(1, false, RunStopOutcomes.UNKNOWN))))
        {
            RunStopOutcomeSummary unknown = summary(entries).getStopOutcomes();
            assertEquals(0, unknown.getKnownCount()); assertTrue(unknown.describe().contains("unavailable"));
        }
    }

    @Test public void invalidRecordsAndDuplicateIdentitiesCannotInflateCounts()
    {
        RunMetricsSnapshot valid = field(1, false, RunStopOutcomes.of(1, 0, 0));
        RunHistorySummary result = summary(Arrays.asList(valid, valid, null,
                field(-1, true, RunStopOutcomes.of(3, 0, 0)),
                field(2, true, RunStopOutcomes.of(1, 0, 0)),
                field(3, false, RunStopOutcomes.of(3, 0, 1))));
        assertEquals(1, result.getEntries().size()); assertEquals(1, result.getStopOutcomes().getCompleted());
        assertEquals(2, result.getStopOutcomes().getUnfinished());
    }

    @Test public void globalCapPrecedesFiltersAndUnknownRowsStillOccupyTheirSavedSlots()
    {
        List<RunMetricsSnapshot> entries = new ArrayList<>();
        for (int i = 1; i <= 20; i++) { entries.add(field(i, false, RunStopOutcomes.UNKNOWN)); }
        entries.add(field(21, true, RunStopOutcomes.of(3, 2, 0)));
        RunHistorySummary result = summary(entries);
        assertEquals(20, result.getStopOutcomes().getUnknownCount()); assertEquals(0, result.getStopOutcomes().getKnownCount());
        assertEquals(0, RunHistorySummary.create(entries, RunHistoryPeriod.ALL, null, now, RunHistoryOutcome.COMPLETED).getEntries().size());
    }

    @Test public void rollingWindowKindAndOutcomeFiltersRecomputeStopCoverageTogether()
    {
        List<RunMetricsSnapshot> entries = List.of(field(86400, true, RunStopOutcomes.of(1, 1, 2)),
                field(86401, true, RunStopOutcomes.of(3, 0, 0)), field(2, false, RunStopOutcomes.of(2, 0, 0)),
                entry(3, RunHistoryStore.Kind.TREE, "OAK", true, RunStopOutcomes.of(3, 1, 0)));
        RunHistorySummary result = RunHistorySummary.create(entries, RunHistoryPeriod.LAST_24_HOURS,
                RunHistoryStore.Kind.FIELD, now, RunHistoryOutcome.COMPLETED);
        assertEquals(1, result.getStopOutcomes().getKnownCount()); assertEquals(1, result.getStopOutcomes().getCompleted());
        assertEquals(2, result.getStopOutcomes().getSkipped());
        assertEquals(0, RunHistorySummary.create(entries, RunHistoryPeriod.LAST_24_HOURS,
                RunHistoryStore.Kind.FIELD, now.plusNanos(1), RunHistoryOutcome.COMPLETED).getStopOutcomes().getKnownCount());
    }

    @Test public void typeAndCropComparisonsKeepIndependentStopCoverage()
    {
        List<RunMetricsSnapshot> entries = List.of(field(1, true, RunStopOutcomes.of(1, 1, 2)),
                field(2, false, RunStopOutcomes.UNKNOWN),
                entry(3, RunHistoryStore.Kind.FIELD, "ONION", false, RunStopOutcomes.of(2, 0, 0)),
                entry(4, RunHistoryStore.Kind.TREE, "OAK", true, RunStopOutcomes.of(3, 0, 0)));
        RunHistoryComparison comparison = RunHistoryComparison.create(entries, RunHistoryPeriod.ALL, now);
        assertEquals(2, comparison.getRows().size());
        for (RunHistoryComparison.Row row : comparison.getRows())
        {
            RunStopOutcomeSummary outcomes = row.getSummary().getStopOutcomes();
            if (row.getKind() == RunHistoryStore.Kind.TREE)
            { assertEquals(1, outcomes.getKnownCount()); assertEquals(0, outcomes.getUnknownCount()); }
            else
            {
                assertEquals(2, outcomes.getKnownCount()); assertEquals(1, outcomes.getUnknownCount());
                assertEquals(3, outcomes.getCompleted());
                for (RunHistoryComparison.CropRow crop : row.getCrops())
                {
                    RunStopOutcomeSummary counts = crop.getStatistics().getSummary().getStopOutcomes();
                    assertEquals(1, counts.getKnownCount());
                    assertEquals("POTATO".equals(crop.getCropKey()) ? 1 : 0, counts.getUnknownCount());
                    assertEquals("POTATO".equals(crop.getCropKey()) ? 1 : 2, counts.getCompleted());
                }
            }
        }
    }
}
