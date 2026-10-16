package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Descriptive comparison of the same bounded saved sample. No rankings or future performance claims. */
public final class RunHistoryComparison
{
    public static final class Row
    {
        private final RunHistoryStore.Kind kind;
        private final RunHistorySummary summary;
        private final int timedCompletedCount;
        private final Duration mean,minimum,maximum;
        private Row(RunHistoryStore.Kind kind,RunHistorySummary summary)
        {
            this.kind=kind; this.summary=summary;
            int count=0; Duration total=Duration.ZERO,min=null,max=null;
            for(RunMetricsSnapshot entry:summary.getEntries())
            {
                if(!entry.isCompleted() || entry.getElapsed().isZero()) { continue; }
                Duration elapsed=entry.getElapsed(); total=total.plus(elapsed); count++;
                if(min==null || elapsed.compareTo(min)<0) { min=elapsed; }
                if(max==null || elapsed.compareTo(max)>0) { max=elapsed; }
            }
            timedCompletedCount=count; mean=count==0?null:total.dividedBy(count); minimum=min; maximum=max;
        }
        public RunHistoryStore.Kind getKind() { return kind; }
        public RunHistorySummary getSummary() { return summary; }
        public int getTimedCompletedCount() { return timedCompletedCount; }
        public Optional<Duration> getMeanCompletedTime() { return Optional.ofNullable(mean); }
        public Optional<Duration> getMinimumCompletedTime() { return Optional.ofNullable(minimum); }
        public Optional<Duration> getMaximumCompletedTime() { return Optional.ofNullable(maximum); }
    }
    private final List<Row> rows;
    private RunHistoryComparison(List<Row> rows) { this.rows=List.copyOf(rows); }
    public static RunHistoryComparison create(List<RunMetricsSnapshot> history,RunHistoryPeriod period,Instant now)
    {
        // Validate, deduplicate and cap the entire sample before splitting by kind.
        List<RunMetricsSnapshot> bounded=RunHistorySummary.create(history,period,null,now).getEntries();
        List<Row> rows=new ArrayList<>();
        for(RunHistoryStore.Kind kind:RunHistoryStore.Kind.values())
        {
            RunHistorySummary summary=RunHistorySummary.create(bounded,RunHistoryPeriod.ALL,kind,now);
            if(!summary.getEntries().isEmpty()) { rows.add(new Row(kind,summary)); }
        }
        return new RunHistoryComparison(rows);
    }
    public List<Row> getRows() { return rows; }
}
