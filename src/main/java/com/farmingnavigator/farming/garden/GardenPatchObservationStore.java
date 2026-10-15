package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class GardenPatchObservationStore
{
    private final Map<GardenPatchDefinition, GardenPatchObservation> observations = new EnumMap<>(GardenPatchDefinition.class);
    private Map<GardenPatchDefinition, GardenPatchObservation> cached = Collections.emptyMap();
    private final GardenPatchStateDecoder decoder = new GardenPatchStateDecoder();

    public boolean observe(GardenPatchDefinition patch, int raw, Instant at, FarmingGrowthRate rate)
    {
        Objects.requireNonNull(patch);
        Objects.requireNonNull(at);
        Objects.requireNonNull(rate);
        GardenPatchObservation previous = observations.get(patch);
        if (previous != null)
        {
            if (at.isBefore(previous.getObservedAt())) { return false; }
            if (previous.getState().getRawValue() == raw && previous.getGrowthRate() == rate
                    && Duration.between(previous.getObservedAt(), at).getSeconds() < 30) { return false; }
        }
        observations.put(patch, new GardenPatchObservation(decoder.decode(patch.getFamily(), raw), at, rate));
        cached = null;
        return true;
    }

    public Map<GardenPatchDefinition, GardenPatchObservation> snapshot()
    {
        if (cached == null) { cached = Collections.unmodifiableMap(new EnumMap<>(observations)); }
        return cached;
    }
    public void clear() { observations.clear(); cached = Collections.emptyMap(); }
    public boolean isEmpty() { return observations.isEmpty(); }
}
