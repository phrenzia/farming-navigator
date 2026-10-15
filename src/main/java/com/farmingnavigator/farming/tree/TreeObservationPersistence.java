package com.farmingnavigator.farming.tree;

import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import com.farmingnavigator.farming.state.ObservationRecordCodec;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class TreeObservationPersistence
{
    public static final String KEY_PREFIX = "treeObservationV1_";
    private final ObservationPersistenceBackend backend;
    private String profile;
    public TreeObservationPersistence(ObservationPersistenceBackend backend) { this.backend = backend; }

    public boolean loadProfile(String next, TreePatchObservationStore store, Instant now)
    {
        if (Objects.equals(profile, next)) { return false; }
        profile = next;
        store.clear();
        if (profile != null)
        {
            for (TreePatchDefinition patch : TreePatchDefinition.values())
            {
                ObservationRecordCodec.Value record = ObservationRecordCodec.decode(backend.read(profile, KEY_PREFIX + patch.name()), now);
                if (record != null) { store.observe(patch, record.getRaw(), record.getAt(), record.getRate()); }
            }
        }
        return true;
    }

    public void save(Map<TreePatchDefinition, TreePatchObservation> observations)
    {
        if (profile == null) { return; }
        observations.forEach((patch, observation) -> backend.write(profile, KEY_PREFIX + patch.name(),
                ObservationRecordCodec.encode(observation.getState().getRawValue(), observation.getObservedAt(), observation.getGrowthRate())));
    }
    public void detach() { profile = null; }
}
