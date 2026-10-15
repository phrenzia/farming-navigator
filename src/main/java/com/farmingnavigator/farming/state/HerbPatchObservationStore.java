package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Observations owned by the client thread. Clear before restoring another account's history. */
public class HerbPatchObservationStore
{
    private final Map<HerbPatchDefinition, HerbPatchObservation> observations =
            new EnumMap<>(HerbPatchDefinition.class);
    private final HerbPatchStateDecoder decoder = new HerbPatchStateDecoder();
    private Map<HerbPatchDefinition, HerbPatchObservation> cachedSnapshot = Collections.emptyMap();

    public boolean observe(HerbPatchDefinition patch, int raw, Instant observedAt)
    {
        return observe(patch, raw, observedAt, FarmingGrowthRate.UNKNOWN);
    }

    public boolean observe(HerbPatchDefinition patch, int raw, Instant observedAt, FarmingGrowthRate growthRate)
    {
        Objects.requireNonNull(patch);
        Objects.requireNonNull(observedAt);
        HerbPatchObservation previous = observations.get(patch);
        if (previous != null)
        {
            if (observedAt.isBefore(previous.getObservedAt()))
            {
                return false;
            }
            if (raw == previous.getState().getRawValue() && growthRate == previous.getGrowthRate()
                    && Duration.between(previous.getObservedAt(), observedAt).getSeconds() < 30)
            {
                return false;
            }
        }
        // Unknown raw values replace old known data rather than leaving stale certainty.
        observations.put(patch, new HerbPatchObservation(decoder.decode(raw), observedAt, growthRate));
        cachedSnapshot = null;
        return true;
    }

    public Map<HerbPatchDefinition, HerbPatchObservation> snapshot()
    {
        if (cachedSnapshot == null)
        {
            cachedSnapshot = Collections.unmodifiableMap(new EnumMap<>(observations));
        }
        return cachedSnapshot;
    }

    public void clear()
    {
        observations.clear();
        cachedSnapshot = Collections.emptyMap();
    }

    public boolean isEmpty()
    {
        return observations.isEmpty();
    }
}
