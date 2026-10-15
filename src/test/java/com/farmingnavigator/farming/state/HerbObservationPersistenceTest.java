package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class HerbObservationPersistenceTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final Map<String, String> records = new HashMap<>();
    private final HerbObservationPersistence persistence = new HerbObservationPersistence(
            new HerbObservationPersistence.Backend()
            {
                @Override public String read(String profile, String key) { return records.get(profile + "/" + key); }
                @Override public void write(String profile, String key, String value)
                {
                    records.put(profile + "/" + key, value);
                }
            });
    private final HerbPatchObservationStore store = new HerbPatchObservationStore();

    @Test public void restoresExactTimestampRawValueAndGrowthModeAfterRestart()
    {
        persistence.loadProfile("characterA", store, now);
        store.observe(HerbPatchDefinition.FALADOR, 4, now, FarmingGrowthRate.NORMAL);
        store.observe(HerbPatchDefinition.CATHERBY, 110, now, FarmingGrowthRate.UNKNOWN);
        persistence.save(store.snapshot());
        persistence.detach();
        store.clear();
        assertTrue(persistence.loadProfile("characterA", store, now.plusSeconds(300)));
        assertEquals(2, store.snapshot().size());
        HerbPatchObservation restored = store.snapshot().get(HerbPatchDefinition.FALADOR);
        assertEquals(now, restored.getObservedAt());
        assertEquals(4, restored.getState().getRawValue());
        assertEquals(FarmingGrowthRate.NORMAL, restored.getGrowthRate());
        assertEquals(PatchCondition.UNKNOWN, store.snapshot().get(HerbPatchDefinition.CATHERBY).getState().getCondition());
    }

    @Test public void switchesAccountsWithoutLeakingHistoryOrWritingToPreviousCharacter()
    {
        persistence.loadProfile("A", store, now);
        store.observe(HerbPatchDefinition.FALADOR, 4, now, FarmingGrowthRate.NORMAL);
        persistence.save(store.snapshot());
        persistence.loadProfile("B", store, now);
        assertTrue(store.isEmpty());
        store.observe(HerbPatchDefinition.CATHERBY, 5, now, FarmingGrowthRate.LEAGUES);
        persistence.save(store.snapshot());
        persistence.loadProfile("A", store, now);
        assertEquals(1, store.snapshot().size());
        assertNotNull(store.snapshot().get(HerbPatchDefinition.FALADOR));
        assertNull(store.snapshot().get(HerbPatchDefinition.CATHERBY));
        persistence.loadProfile("B", store, now);
        assertEquals(FarmingGrowthRate.LEAGUES, store.snapshot().get(HerbPatchDefinition.CATHERBY).getGrowthRate());
    }

    @Test public void missingProfileClearsMemoryAndCannotWriteUnscopedRecords()
    {
        persistence.loadProfile("A", store, now);
        store.observe(HerbPatchDefinition.FALADOR, 4, now);
        persistence.loadProfile(null, store, now);
        assertTrue(store.isEmpty());
        store.observe(HerbPatchDefinition.FALADOR, 4, now);
        persistence.save(store.snapshot());
        assertTrue(records.isEmpty());
    }

    @Test public void sameProfileDoesNotOverwriteNewObservationsWithSavedOnes()
    {
        persistence.loadProfile("A", store, now);
        store.observe(HerbPatchDefinition.FALADOR, 4, now);
        assertFalse(persistence.loadProfile("A", store, now.plusSeconds(10)));
        assertFalse(store.isEmpty());
    }

    @Test public void rejectsCorruptionUnsupportedVersionsAndInvalidFields()
    {
        String timestamp = Long.toString(now.toEpochMilli());
        for (String value : new String[]{null, "", "garbage", "2|4|" + timestamp + "|NORMAL",
                "1|256|" + timestamp + "|NORMAL", "1|-1|" + timestamp + "|NORMAL",
                "1|4|broken|NORMAL", "1|4|" + timestamp + "|FUTURE", "1|4|" + timestamp + "|NORMAL|extra"})
        {
            assertNull(value, HerbObservationPersistence.decode(value, now));
        }
    }

    @Test public void rejectsFutureAndOverThirtyDayObservationsWithoutRefreshingTheirAge()
    {
        assertNull(HerbObservationPersistence.decode("1|4|" + now.plusMillis(1).toEpochMilli() + "|NORMAL", now));
        Instant boundary = now.minus(Duration.ofDays(30));
        assertNotNull(HerbObservationPersistence.decode("1|4|" + boundary.toEpochMilli() + "|NORMAL", now));
        assertNull(HerbObservationPersistence.decode("1|4|" + boundary.minusMillis(1).toEpochMilli() + "|NORMAL", now));
    }

    @Test public void damagedPatchDoesNotDiscardOtherValidPatchRecords()
    {
        records.put("A/herbObservationV1_FALADOR", "damaged");
        records.put("A/herbObservationV1_CATHERBY", "1|4|" + now.toEpochMilli() + "|NORMAL");
        persistence.loadProfile("A", store, now);
        assertEquals(1, store.snapshot().size());
        assertNotNull(store.snapshot().get(HerbPatchDefinition.CATHERBY));
    }
}
