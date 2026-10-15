package com.farmingnavigator.farming.state;

import java.time.Duration;
import java.time.Instant;

/** Shared V1 raw/timestamp/rate format. Old timestamps are never refreshed by loading. */
public final class ObservationRecordCodec
{
    private ObservationRecordCodec() { }
    public static final class Value
    {
        private final int raw;
        private final Instant at;
        private final FarmingGrowthRate rate;
        private Value(int raw, Instant at, FarmingGrowthRate rate) { this.raw = raw; this.at = at; this.rate = rate; }
        public int getRaw() { return raw; }
        public Instant getAt() { return at; }
        public FarmingGrowthRate getRate() { return rate; }
    }

    public static String encode(int raw, Instant at, FarmingGrowthRate rate)
    {
        return "1|" + raw + "|" + at.toEpochMilli() + "|" + rate.name();
    }

    public static Value decode(String text, Instant now)
    {
        if (text == null || text.length() > 100) { return null; }
        try
        {
            String[] fields = text.split("\\|", -1);
            if (fields.length != 4 || !"1".equals(fields[0])) { return null; }
            int raw = Integer.parseInt(fields[1]);
            Instant at = Instant.ofEpochMilli(Long.parseLong(fields[2]));
            if (raw < 0 || raw > 255 || at.isAfter(now) || Duration.between(at, now).compareTo(Duration.ofDays(30)) > 0) { return null; }
            return new Value(raw, at, FarmingGrowthRate.valueOf(fields[3]));
        }
        catch (IllegalArgumentException ex) { return null; }
    }
}
