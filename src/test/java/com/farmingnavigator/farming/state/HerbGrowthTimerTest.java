package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import net.runelite.api.WorldType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HerbGrowthTimerTest
{
    private final Instant observedAt = Instant.parse("2026-10-15T12:00:00Z");
    private final HerbGrowthTimer timer = new HerbGrowthTimer();

    @Test
    public void normalHerbStagesUseTwentyMinuteUncertaintyWindow()
    {
        long[] earliest = {60, 40, 20, 0};
        long[] latest = {80, 60, 40, 20};
        for (int stage = 0; stage <= 3; stage++)
        {
            HerbGrowthEstimate estimate = timer.estimate(observation(32 + stage, FarmingGrowthRate.NORMAL), observedAt).get();
            assertEquals(observedAt.plus(Duration.ofMinutes(earliest[stage])), estimate.getEarliest());
            assertEquals(observedAt.plus(Duration.ofMinutes(latest[stage])), estimate.getLatest());
        }
    }

    @Test
    public void leaguesUsesFourMinuteCycles()
    {
        HerbGrowthEstimate estimate = timer.estimate(observation(4, FarmingGrowthRate.LEAGUES), observedAt).get();
        assertEquals(observedAt.plusSeconds(12 * 60), estimate.getEarliest());
        assertEquals(observedAt.plusSeconds(16 * 60), estimate.getLatest());
    }

    @Test
    public void seasonalDeadmanRetainsNormalGrowthRate()
    {
        assertEquals(FarmingGrowthRate.NORMAL, FarmingGrowthRate.fromWorldTypes(Collections.emptySet()));
        assertEquals(FarmingGrowthRate.LEAGUES, FarmingGrowthRate.fromWorldTypes(EnumSet.of(WorldType.SEASONAL)));
        assertEquals(FarmingGrowthRate.NORMAL, FarmingGrowthRate.fromWorldTypes(EnumSet.of(WorldType.SEASONAL, WorldType.DEADMAN)));
        assertEquals(FarmingGrowthRate.UNKNOWN, FarmingGrowthRate.fromWorldTypes(null));
    }

    @Test
    public void unknownRateAndCompatibilityConstructorDoNotInventTimer()
    {
        assertFalse(timer.estimate(observation(32, FarmingGrowthRate.UNKNOWN), observedAt).isPresent());
        HerbPatchObservation legacy = new HerbPatchObservation(new HerbPatchStateDecoder().decode(32), observedAt);
        assertFalse(timer.estimate(legacy, observedAt).isPresent());
    }

    @Test
    public void nonGrowingAndUnsupportedStatesDoNotProduceHarvestTimer()
    {
        for (int raw : new int[] {0, 3, 36, 140, 170, 220})
        {
            assertFalse(timer.estimate(observation(raw, FarmingGrowthRate.NORMAL), observedAt).isPresent());
        }
        assertFalse(timer.estimate(null, observedAt).isPresent());
    }

    @Test
    public void futureObservationDoesNotProduceTimerAfterClockMovesBackwards()
    {
        assertFalse(timer.estimate(observation(32, FarmingGrowthRate.NORMAL), observedAt.minusSeconds(1)).isPresent());
    }

    @Test
    public void huascaAndGoutweedHaveVerifiedHerbGrowthCycles()
    {
        for (int raw : new int[] {60, 192})
        {
            HerbGrowthEstimate estimate = timer.estimate(observation(raw, FarmingGrowthRate.NORMAL), observedAt).get();
            assertEquals(observedAt.plusSeconds(80 * 60), estimate.getLatest());
        }
    }

    @Test
    public void resamplingDiseaseOrHarvestCancelsGrowingPrediction()
    {
        HerbPatchObservationStore store = new HerbPatchObservationStore();
        store.observe(HerbPatchDefinition.FALADOR, 32, observedAt, FarmingGrowthRate.NORMAL);
        assertTrue(timer.estimate(store.snapshot().get(HerbPatchDefinition.FALADOR), observedAt).isPresent());
        store.observe(HerbPatchDefinition.FALADOR, 140, observedAt.plusSeconds(1), FarmingGrowthRate.NORMAL);
        assertFalse(timer.estimate(store.snapshot().get(HerbPatchDefinition.FALADOR), observedAt.plusSeconds(1)).isPresent());
        store.observe(HerbPatchDefinition.FALADOR, 36, observedAt.plusSeconds(2), FarmingGrowthRate.NORMAL);
        assertFalse(timer.estimate(store.snapshot().get(HerbPatchDefinition.FALADOR), observedAt.plusSeconds(2)).isPresent());
    }

    @Test
    public void changedWorldRateIsRecordedWithoutWaitingForTimestampThrottle()
    {
        HerbPatchObservationStore store = new HerbPatchObservationStore();
        store.observe(HerbPatchDefinition.FALADOR, 32, observedAt, FarmingGrowthRate.NORMAL);
        assertTrue(store.observe(HerbPatchDefinition.FALADOR, 32, observedAt.plusSeconds(1), FarmingGrowthRate.LEAGUES));
        assertEquals(FarmingGrowthRate.LEAGUES, store.snapshot().get(HerbPatchDefinition.FALADOR).getGrowthRate());
    }

    private HerbPatchObservation observation(int raw, FarmingGrowthRate rate)
    {
        return new HerbPatchObservation(new HerbPatchStateDecoder().decode(raw), observedAt, rate);
    }
}
