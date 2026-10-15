package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import com.farmingnavigator.farming.state.ObservationRecordCodec;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class GardenObservationPersistence
{
    public static final String KEY_PREFIX = "gardenObservationV1_";
    private final ObservationPersistenceBackend backend;
    private String profile;
    public GardenObservationPersistence(ObservationPersistenceBackend backend) { this.backend = backend; }

    public boolean loadProfile(String next, GardenPatchObservationStore store, Instant now)
    {
        if (Objects.equals(profile, next)) { return false; }
        profile = next;
        store.clear();
        if (profile != null)
        {
            for (GardenPatchDefinition patch : GardenPatchDefinition.values())
            {
                ObservationRecordCodec.Value record = ObservationRecordCodec.decode(backend.read(profile, KEY_PREFIX + patch.name()), now);
                if (record != null) { store.observe(patch, record.getRaw(), record.getAt(), record.getRate()); }
            }
        }
        return true;
    }

    public void save(Map<GardenPatchDefinition, GardenPatchObservation> observations)
    {
        if (profile == null) { return; }
        observations.forEach((patch, observation) -> backend.write(profile, KEY_PREFIX + patch.name(),
                ObservationRecordCodec.encode(observation.getState().getRawValue(), observation.getObservedAt(), observation.getGrowthRate())));
    }
    public void detach() { profile = null; }
}
