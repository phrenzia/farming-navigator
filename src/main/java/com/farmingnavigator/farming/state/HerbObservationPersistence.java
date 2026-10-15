package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Small versioned records; profile keys are captured explicitly to prevent cross-account writes. */
public final class HerbObservationPersistence
{
    public static final String KEY_PREFIX = "herbObservationV1_";
    public interface Backend
    {
        String read(String profile, String key);
        void write(String profile, String key, String value);
    }

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
        return "1|" + observation.getState().getRawValue() + "|" + observation.getObservedAt().toEpochMilli()
                + "|" + observation.getGrowthRate().name();
    }

    static HerbPatchObservation decode(String value, Instant now)
    {
        if (value == null || value.length() > 100)
        {
            return null;
        }
        try
        {
            String[] fields = value.split("\\|", -1);
            if (fields.length != 4 || !"1".equals(fields[0]))
            {
                return null;
            }
            int raw = Integer.parseInt(fields[1]);
            Instant observedAt = Instant.ofEpochMilli(Long.parseLong(fields[2]));
            if (raw < 0 || raw > 255 || observedAt.isAfter(now)
                    || Duration.between(observedAt, now).compareTo(Duration.ofDays(30)) > 0)
            {
                return null;
            }
            return new HerbPatchObservation(new HerbPatchStateDecoder().decode(raw), observedAt,
                    FarmingGrowthRate.valueOf(fields[3]));
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }
}
