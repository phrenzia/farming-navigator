package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenObservationPersistenceTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final Map<String, String> records = new HashMap<>();
    private final GardenPatchObservationStore store = new GardenPatchObservationStore();
    private final GardenObservationPersistence persistence = new GardenObservationPersistence(new ObservationPersistenceBackend()
    {
        @Override public String read(String profile, String key) { return records.get(profile + "/" + key); }
        @Override public void write(String profile, String key, String value) { records.put(profile + "/" + key, value); }
    });
    @Test public void restartRestoresBothGuildChannelsWithOriginalTimestampAndWorldRate()
    {
        persistence.loadProfile("A-leagues", store, now);
        store.observe(GardenPatchDefinition.FARMING_GUILD_BUSH, 255, now, FarmingGrowthRate.LEAGUES);
        store.observe(GardenPatchDefinition.FARMING_GUILD_CACTUS, 58, now, FarmingGrowthRate.LEAGUES);
        persistence.save(store.snapshot()); persistence.detach(); store.clear();
        persistence.loadProfile("A-leagues", store, now.plusSeconds(600));
        assertEquals(2, store.snapshot().size());
        assertEquals(GardenCrop.POISON_IVY, store.snapshot().get(GardenPatchDefinition.FARMING_GUILD_BUSH).getState().getCrop());
        assertEquals(GardenCrop.POTATO_CACTUS, store.snapshot().get(GardenPatchDefinition.FARMING_GUILD_CACTUS).getState().getCrop());
        assertEquals(now, store.snapshot().get(GardenPatchDefinition.FARMING_GUILD_CACTUS).getObservedAt());
        assertEquals(FarmingGrowthRate.LEAGUES, store.snapshot().get(GardenPatchDefinition.FARMING_GUILD_CACTUS).getGrowthRate());
    }
    @Test public void accountsAndModesCannotReadOrOverwriteEachOthersObservations()
    {
        persistence.loadProfile("A-normal", store, now);
        store.observe(GardenPatchDefinition.RIMMINGTON_BUSH, 250, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); persistence.loadProfile("B-normal", store, now); assertTrue(store.isEmpty());
        store.observe(GardenPatchDefinition.AL_KHARID_CACTUS, 31, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); persistence.loadProfile("A-leagues", store, now); assertTrue(store.isEmpty());
        persistence.loadProfile("A-normal", store, now);
        assertEquals(1, store.snapshot().size()); assertTrue(store.snapshot().containsKey(GardenPatchDefinition.RIMMINGTON_BUSH));
    }
    @Test public void corruptFutureExpiredAndMissingProfileRecordsAreRejected()
    {
        records.put("A/gardenObservationV1_RIMMINGTON_BUSH", "broken");
        records.put("A/gardenObservationV1_ARDOUGNE_BUSH", "1|250|" + now.plusSeconds(1).toEpochMilli() + "|NORMAL");
        records.put("A/gardenObservationV1_ETCETERIA_BUSH", "1|250|" + now.minusSeconds(30L * 86400 + 1).toEpochMilli() + "|NORMAL");
        records.put("A/gardenObservationV1_AL_KHARID_CACTUS", "1|31|" + now.toEpochMilli() + "|NORMAL");
        persistence.loadProfile("A", store, now); assertEquals(1, store.snapshot().size());
        persistence.loadProfile(null, store, now); assertTrue(store.isEmpty());
        store.observe(GardenPatchDefinition.FARMING_GUILD_CACTUS, 58, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); assertEquals(4, records.size());
    }
}
