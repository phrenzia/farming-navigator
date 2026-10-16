package com.farmingnavigator.analytics;

import java.time.Duration;
import java.time.Instant;

public enum RunHistoryPeriod
{
    ALL("All stored (last 20)",null), LAST_24_HOURS("Ended in past 24 hours",Duration.ofHours(24)), LAST_7_DAYS("Ended in past 7 days",Duration.ofDays(7));
    private final String label;
    private final Duration window;
    RunHistoryPeriod(String label,Duration window) { this.label = label; this.window = window; }
    boolean includes(Instant ended,Instant now) { return window == null || !ended.isBefore(now.minus(window)); }
    @Override public String toString() { return label; }
}
