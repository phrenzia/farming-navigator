package com.farmingnavigator.farming.field;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldObservationPersistenceTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final Map<String, String> records = new HashMap<>();
    private final FieldPatchObservationStore store = new FieldPatchObservationStore();
    private final FieldObservationPersistence persistence = new FieldObservationPersistence(new ObservationPersistenceBackend()
    {
        @Override public String read(String profile, String key) { return records.get(profile + "/" + key); }
        @Override public void write(String profile, String key, String value) { records.put(profile + "/" + key, value); }
    });
    @Test public void restartRestoresDistinctGuildChannelsWithOriginalTimestampAndWorldRate()
    {
        persistence.loadProfile("A-leagues", store, now);
        store.observe(FieldPatchDefinition.FARMING_GUILD_NORTH, 138, now, FarmingGrowthRate.LEAGUES);
        store.observe(FieldPatchDefinition.FARMING_GUILD_FLOWER, 41, now, FarmingGrowthRate.LEAGUES);
        persistence.save(store.snapshot()); persistence.detach(); store.clear();
        persistence.loadProfile("A-leagues", store, now.plusSeconds(600));
        assertEquals(2, store.snapshot().size());
        assertEquals(FieldCrop.SNAPE_GRASS, store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_NORTH).getState().getCrop());
        assertEquals(FieldCrop.WHITE_LILY, store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_FLOWER).getState().getCrop());
        assertEquals(now, store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_FLOWER).getObservedAt());
        assertEquals(FarmingGrowthRate.LEAGUES, store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_FLOWER).getGrowthRate());
    }
    @Test public void accountsAndModesCannotReadOrOverwriteEachOthersObservations()
    {
        persistence.loadProfile("A-normal", store, now);
        store.observe(FieldPatchDefinition.FALADOR_NORTH, 10, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); persistence.loadProfile("B-normal", store, now); assertTrue(store.isEmpty());
        store.observe(FieldPatchDefinition.PRIFDDINAS_FLOWER, 12, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); persistence.loadProfile("A-leagues", store, now); assertTrue(store.isEmpty());
        persistence.loadProfile("A-normal", store, now);
        assertEquals(1, store.snapshot().size()); assertTrue(store.snapshot().containsKey(FieldPatchDefinition.FALADOR_NORTH));
    }
    @Test public void corruptFutureExpiredAndMissingProfileRecordsAreRejected()
    {
        records.put("A/fieldObservationV1_FALADOR_NORTH", "broken");
        records.put("A/fieldObservationV1_ARDOUGNE_NORTH", "1|10|" + now.plusSeconds(1).toEpochMilli() + "|NORMAL");
        records.put("A/fieldObservationV1_HARMONY_ISLAND_ALLOTMENT", "1|10|" + now.minusSeconds(30L * 86400 + 1).toEpochMilli() + "|NORMAL");
        records.put("A/fieldObservationV1_PRIFDDINAS_FLOWER", "1|12|" + now.toEpochMilli() + "|NORMAL");
        persistence.loadProfile("A", store, now); assertEquals(1, store.snapshot().size());
        persistence.loadProfile(null, store, now); assertTrue(store.isEmpty());
        store.observe(FieldPatchDefinition.FARMING_GUILD_FLOWER, 41, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); assertEquals(4, records.size());
    }
    @Test public void hopsFibreHistoryRestoresWithoutInventingAccess()
    {
        persistence.loadProfile("A",store,now);
        store.observe(FieldPatchDefinition.ALDARIN_HOPS,119,now,FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot()); persistence.detach(); store.clear(); persistence.loadProfile("A",store,now.plusSeconds(10));
        assertEquals(FieldCrop.COTTON,store.snapshot().get(FieldPatchDefinition.ALDARIN_HOPS).getState().getCrop());
        assertEquals(now,store.snapshot().get(FieldPatchDefinition.ALDARIN_HOPS).getObservedAt());
    }

}
