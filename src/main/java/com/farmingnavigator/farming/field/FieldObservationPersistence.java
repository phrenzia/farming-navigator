package com.farmingnavigator.farming.field;

import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import com.farmingnavigator.farming.state.ObservationRecordCodec;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class FieldObservationPersistence
{
    public static final String KEY_PREFIX = "fieldObservationV1_";
    private final ObservationPersistenceBackend backend;
    private String profile;
    public FieldObservationPersistence(ObservationPersistenceBackend backend) { this.backend = backend; }

    public boolean loadProfile(String next, FieldPatchObservationStore store, Instant now)
    {
        if (Objects.equals(profile, next)) { return false; }
        profile = next;
        store.clear();
        if (profile != null)
        {
            for (FieldPatchDefinition patch : FieldPatchDefinition.values())
            {
                ObservationRecordCodec.Value record = ObservationRecordCodec.decode(backend.read(profile, KEY_PREFIX + patch.name()), now);
                if (record != null) { store.observe(patch, record.getRaw(), record.getAt(), record.getRate()); }
            }
        }
        return true;
    }

    public void save(Map<FieldPatchDefinition, FieldPatchObservation> observations)
    {
        if (profile == null) { return; }
        observations.forEach((patch, observation) -> backend.write(profile, KEY_PREFIX + patch.name(),
                ObservationRecordCodec.encode(observation.getState().getRawValue(), observation.getObservedAt(), observation.getGrowthRate())));
    }
    public void detach() { profile = null; }
}
