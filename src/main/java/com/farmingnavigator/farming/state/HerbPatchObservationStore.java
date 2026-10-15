package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Session-only observations, owned by the client thread. Clear on account/session changes. */
public class HerbPatchObservationStore
{
    private final Map<HerbPatchDefinition, HerbPatchObservation> observations =
            new EnumMap<>(HerbPatchDefinition.class);
    private final HerbPatchStateDecoder decoder = new HerbPatchStateDecoder();

    public boolean observe(HerbPatchDefinition patch, int raw, Instant observedAt)
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
            if (raw == previous.getState().getRawValue()
                    && Duration.between(previous.getObservedAt(), observedAt).getSeconds() < 30)
            {
                return false;
            }
        }
        // Unknown raw values replace old known data rather than leaving stale certainty.
        observations.put(patch, new HerbPatchObservation(decoder.decode(raw), observedAt));
        return true;
    }

    public Map<HerbPatchDefinition, HerbPatchObservation> snapshot()
    {
        return Collections.unmodifiableMap(new EnumMap<>(observations));
    }

    public void clear()
    {
        observations.clear();
    }
}
