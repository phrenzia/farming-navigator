package com.farmingnavigator.farming.tree;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeObservationPersistenceTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final Map<String, String> records = new HashMap<>();
    private final TreePatchObservationStore store = new TreePatchObservationStore();
    private final TreeObservationPersistence persistence = new TreeObservationPersistence(new ObservationPersistenceBackend()
    {
        @Override public String read(String profile, String key) { return records.get(profile + "/" + key); }
        @Override public void write(String profile, String key, String value) { records.put(profile + "/" + key, value); }
    });

    @Test public void restartRestoresTheOriginalTimestampAndMode()
    {
        persistence.loadProfile("A-leagues", store, now);
        store.observe(TreePatchDefinition.VARROCK_TREE, 12, now, FarmingGrowthRate.LEAGUES);
        store.observe(TreePatchDefinition.CATHERBY_FRUIT, 226, now, FarmingGrowthRate.LEAGUES);
        persistence.save(store.snapshot());
        persistence.detach();
        store.clear();
        persistence.loadProfile("A-leagues", store, now.plusSeconds(600));
        assertEquals(2, store.snapshot().size());
        assertEquals(now, store.snapshot().get(TreePatchDefinition.CATHERBY_FRUIT).getObservedAt());
        assertEquals(TreeCrop.PALM, store.snapshot().get(TreePatchDefinition.CATHERBY_FRUIT).getState().getCrop());
        assertEquals(FarmingGrowthRate.LEAGUES, store.snapshot().get(TreePatchDefinition.VARROCK_TREE).getGrowthRate());
    }

    @Test public void characterAndModeProfilesDoNotShareHistoryOrWrites()
    {
        persistence.loadProfile("A-normal", store, now);
        store.observe(TreePatchDefinition.VARROCK_TREE, 12, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot());
        persistence.loadProfile("B-normal", store, now);
        assertTrue(store.isEmpty());
        store.observe(TreePatchDefinition.CATHERBY_FRUIT, 34, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot());
        persistence.loadProfile("A-leagues", store, now);
        assertTrue(store.isEmpty());
        persistence.loadProfile("A-normal", store, now);
        assertEquals(1, store.snapshot().size());
        assertTrue(store.snapshot().containsKey(TreePatchDefinition.VARROCK_TREE));
        assertFalse(store.snapshot().containsKey(TreePatchDefinition.CATHERBY_FRUIT));
    }

    @Test public void malformedFutureAndExpiredRecordsDoNotDisplaceValidRecords()
    {
        records.put("A/treeObservationV1_VARROCK_TREE", "broken");
        records.put("A/treeObservationV1_CATHERBY_FRUIT", "1|34|" + now.plusSeconds(1).toEpochMilli() + "|NORMAL");
        records.put("A/treeObservationV1_FALADOR_TREE", "1|12|" + now.minusSeconds(30L * 86400 + 1).toEpochMilli() + "|NORMAL");
        records.put("A/treeObservationV1_TAVERLEY_TREE", "1|12|" + now.toEpochMilli() + "|NORMAL");
        persistence.loadProfile("A", store, now);
        assertEquals(1, store.snapshot().size());
        assertTrue(store.snapshot().containsKey(TreePatchDefinition.TAVERLEY_TREE));
    }

    @Test public void missingProfileCannotReadOrWriteGlobalData()
    {
        persistence.loadProfile("A", store, now);
        store.observe(TreePatchDefinition.VARROCK_TREE, 12, now, FarmingGrowthRate.NORMAL);
        persistence.loadProfile(null, store, now);
        assertTrue(store.isEmpty());
        store.observe(TreePatchDefinition.VARROCK_TREE, 12, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot());
        assertTrue(records.isEmpty());
    }

    @Test public void sameProfileDoesNotReloadOlderSavedObservations()
    {
        persistence.loadProfile("A", store, now);
        store.observe(TreePatchDefinition.VARROCK_TREE, 12, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot());
        store.observe(TreePatchDefinition.VARROCK_TREE, 13, now.plusSeconds(1), FarmingGrowthRate.NORMAL);
        assertFalse(persistence.loadProfile("A", store, now.plusSeconds(2)));
        assertEquals(13, store.snapshot().get(TreePatchDefinition.VARROCK_TREE).getState().getRawValue());
    }
}
