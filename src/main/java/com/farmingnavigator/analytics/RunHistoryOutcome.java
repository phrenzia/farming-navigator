package com.farmingnavigator.analytics;

/** Filtering a saved sample never changes or deletes the underlying records. */
public enum RunHistoryOutcome
{
    ALL("All outcomes"), COMPLETED("Completed only"), STOPPED("Stopped only");
    private final String label;
    RunHistoryOutcome(String label){this.label=label;}
    public boolean includes(RunMetricsSnapshot entry)
    {return this==ALL || this==COMPLETED && entry.isCompleted() || this==STOPPED && !entry.isCompleted();}
    @Override public String toString(){return label;}
}
