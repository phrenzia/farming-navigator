package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Small versioned records; profile keys are captured explicitly to prevent cross-account writes. */
public final class HerbObservationPersistence
{
    public static final String KEY_PREFIX = "herbObservationV1_";
    public interface Backend extends ObservationPersistenceBackend { }

    private final Backend backend;
    private String profile;

    public HerbObservationPersistence(Backend backend)
    {
        this.backend = backend;
    }

    public boolean loadProfile(String nextProfile, HerbPatchObservationStore store, Instant now)
    {
        if (Objects.equals(profile, nextProfile))
        {
            return false;
        }
        store.clear();
        profile = nextProfile;
        if (profile != null)
        {
            for (HerbPatchDefinition patch : HerbPatchDefinition.values())
            {
                HerbPatchObservation observation = decode(backend.read(profile, key(patch)), now);
                if (observation != null)
                {
                    store.observe(patch, observation.getState().getRawValue(), observation.getObservedAt(),
                            observation.getGrowthRate());
                }
            }
        }
        return true;
    }

    public void save(Map<HerbPatchDefinition, HerbPatchObservation> observations)
    {
        if (profile == null)
        {
            return;
        }
        observations.forEach((patch, observation) -> backend.write(profile, key(patch), encode(observation)));
    }

    public void detach()
    {
        profile = null;
    }

    private static String key(HerbPatchDefinition patch)
    {
        return KEY_PREFIX + patch.name();
    }

    static String encode(HerbPatchObservation observation)
    {
        return ObservationRecordCodec.encode(observation.getState().getRawValue(), observation.getObservedAt(), observation.getGrowthRate());
    }

    static HerbPatchObservation decode(String value, Instant now)
    {
        ObservationRecordCodec.Value record = ObservationRecordCodec.decode(value, now);
        return record == null ? null : new HerbPatchObservation(new HerbPatchStateDecoder().decode(record.getRaw()),
                record.getAt(), record.getRate());
    }
}
