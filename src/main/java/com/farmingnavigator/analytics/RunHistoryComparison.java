package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Descriptive comparison of the same bounded saved sample. No rankings or future performance claims. */
public final class RunHistoryComparison
{
    public static final class CropRow
    {
        private final String cropKey,cropLabel;
        private final Row statistics;
        private final int minimumPlannedStops,maximumPlannedStops;
        private CropRow(String key,String label,Row statistics)
        {
            cropKey=key;cropLabel=label;this.statistics=statistics;int min=Integer.MAX_VALUE,max=0;
            for(RunMetricsSnapshot entry:statistics.getSummary().getEntries())
            {if(entry.getContext().isKnown()){min=Math.min(min,entry.getContext().getPlannedStops());max=Math.max(max,entry.getContext().getPlannedStops());}}
            minimumPlannedStops=max==0?0:min;maximumPlannedStops=max;
        }
        public boolean isKnown(){return !"-".equals(cropKey);}
        public String getCropKey(){return cropKey;}
        public String getCropLabel(){return cropLabel;}
        public Row getStatistics(){return statistics;}
        public int getMinimumPlannedStops(){return minimumPlannedStops;}
        public int getMaximumPlannedStops(){return maximumPlannedStops;}
    }
    public static final class Row
    {
        private final RunHistoryStore.Kind kind;
        private final List<CropRow> crops;
        private final RunHistorySummary summary;
        private final int timedCompletedCount;
        private final Duration mean,minimum,maximum;
        private Row(RunHistoryStore.Kind kind,RunHistorySummary summary,Instant now,boolean includeCrops)
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
            List<CropRow> groups=new ArrayList<>();
            if(includeCrops)
            {
                java.util.Map<String,List<RunMetricsSnapshot>> byCrop=new java.util.LinkedHashMap<>();
                for(RunMetricsSnapshot entry:summary.getEntries())
                {byCrop.computeIfAbsent(entry.getContext().getCropKey(),key -> new ArrayList<>()).add(entry);}
                byCrop.forEach((key,entries) -> {
                    String label=entries.get(0).getContext().isKnown()?entries.get(0).getContext().getCropLabel():"Setup unavailable";
                    // Child statistics do not recurse. The globally bounded parent sample was validated before grouping.
                    groups.add(new CropRow(key,label,new Row(kind,RunHistorySummary.create(entries,RunHistoryPeriod.ALL,kind,now),now,false)));
                });
                groups.sort(java.util.Comparator.comparing(CropRow::isKnown).reversed().thenComparing(CropRow::getCropLabel));
            }
            crops=List.copyOf(groups);
        }
        public List<CropRow> getCrops(){return crops;}
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
            if(!summary.getEntries().isEmpty()) { rows.add(new Row(kind,summary,now,true)); }
        }
        return new RunHistoryComparison(rows);
    }
    public List<Row> getRows() { return rows; }
}
