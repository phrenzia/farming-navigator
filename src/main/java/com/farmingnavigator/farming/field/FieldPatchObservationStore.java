package com.farmingnavigator.farming.field;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class FieldPatchObservationStore
{
    private final Map<FieldPatchDefinition, FieldPatchObservation> observations = new EnumMap<>(FieldPatchDefinition.class);
    private Map<FieldPatchDefinition, FieldPatchObservation> cached = Collections.emptyMap();
    private final FieldPatchStateDecoder decoder = new FieldPatchStateDecoder();

    public boolean observe(FieldPatchDefinition patch, int raw, Instant at, FarmingGrowthRate rate)
    {
        Objects.requireNonNull(patch);
        Objects.requireNonNull(at);
        Objects.requireNonNull(rate);
        FieldPatchObservation previous = observations.get(patch);
        if (previous != null)
        {
            if (at.isBefore(previous.getObservedAt())) { return false; }
            if (previous.getState().getRawValue() == raw && previous.getGrowthRate() == rate
                    && Duration.between(previous.getObservedAt(), at).getSeconds() < 30) { return false; }
        }
        observations.put(patch, new FieldPatchObservation(decoder.decode(patch.getFamily(), raw), at, rate));
        cached = null;
        return true;
    }

    public Map<FieldPatchDefinition, FieldPatchObservation> snapshot()
    {
        if (cached == null) { cached = Collections.unmodifiableMap(new EnumMap<>(observations)); }
        return cached;
    }
    public void clear() { observations.clear(); cached = Collections.emptyMap(); }
    public boolean isEmpty() { return observations.isEmpty(); }
}
