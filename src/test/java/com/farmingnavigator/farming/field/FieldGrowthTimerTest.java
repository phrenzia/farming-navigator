package com.farmingnavigator.farming.field;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import net.runelite.client.plugins.timetracking.farming.Produce;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldGrowthTimerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final FieldGrowthTimer timer = new FieldGrowthTimer();
    private FieldPatchObservation observation(FieldFamily family, int raw, FarmingGrowthRate rate)
    {
        return new FieldPatchObservation(new FieldPatchStateDecoder().decode(family, raw), now, rate);
    }
    @Test public void allTwentySevenCropsMatchPublicRuneLiteMetadata()
    {
        int count = 0;
        for (FieldCrop crop : FieldCrop.values())
        {
            if (crop == FieldCrop.UNKNOWN) { continue; }
            Produce source = Produce.valueOf(crop.name());
            assertEquals(source.getPatchImplementation().name(), crop.getFamily().name());
            assertEquals(source.getStages(), crop.getStages()); assertEquals(source.getTickrate(), crop.getGrowthCycleMinutes());
            count++;
        }
        assertEquals(27, count);
    }
    @Test public void cropsUseDifferentWindowsAndLeaguesScaling()
    {
        FieldGrowthWindow potato = timer.estimate(observation(FieldFamily.ALLOTMENT, 6, FarmingGrowthRate.NORMAL), now).get();
        assertEquals(now.plusSeconds(1800), potato.getEarliest()); assertEquals(now.plusSeconds(2400), potato.getLatest());
        FieldGrowthWindow watermelon = timer.estimate(observation(FieldFamily.ALLOTMENT, 52, FarmingGrowthRate.NORMAL), now).get();
        assertEquals(now.plusSeconds(4200), watermelon.getEarliest()); assertEquals(now.plusSeconds(4800), watermelon.getLatest());
        FieldGrowthWindow flower = timer.estimate(observation(FieldFamily.FLOWER, 37, FarmingGrowthRate.NORMAL), now).get();
        assertEquals(now.plusSeconds(900), flower.getEarliest()); assertEquals(now.plusSeconds(1200), flower.getLatest());
        assertEquals(now.plusSeconds(240), timer.estimate(observation(FieldFamily.FLOWER, 37, FarmingGrowthRate.LEAGUES), now).get().getLatest());
    }
    @Test public void alternateGrowthBytesHaveSameWindow()
    {
        for (int[] pair : new int[][]{{6,70},{13,77},{20,84},{27,91},{34,98},{43,107},{52,116},{63,128}})
        {
            assertEquals(timer.estimate(observation(FieldFamily.ALLOTMENT, pair[0], FarmingGrowthRate.NORMAL), now).get().getLatest(),
                    timer.estimate(observation(FieldFamily.ALLOTMENT, pair[1], FarmingGrowthRate.NORMAL), now).get().getLatest());
        }
    }
    @Test public void scarecrowsWeedsHarvestDiseaseAndDeadStatesNeverPredictHarvest()
    {
        for (FieldFamily family : FieldFamily.values())
        {
            for (int raw = -1; raw <= 256; raw++)
            {
                FieldPatchObservation observation = observation(family, raw, FarmingGrowthRate.NORMAL);
                if (observation.getState().getCondition() != FieldPatchCondition.GROWING)
                {
                    assertFalse(timer.estimate(observation, now).isPresent());
                }
            }
        }
        assertFalse(timer.estimate(null, now).isPresent());
        assertFalse(timer.estimate(observation(FieldFamily.ALLOTMENT, 6, FarmingGrowthRate.UNKNOWN), now).isPresent());
        assertFalse(timer.estimate(observation(FieldFamily.FLOWER, 8, FarmingGrowthRate.NORMAL), now.minusSeconds(1)).isPresent());
        assertFalse(new FieldPatchStateDecoder().decode(null, 6).isKnown());
    }
    @Test public void scarecrowAndHarvestStagesDoNotClaimYieldOrFlowerPlanting()
    {
        for (int raw = 33; raw <= 36; raw++)
        {
            FieldPatchState scarecrow = new FieldPatchStateDecoder().decode(FieldFamily.FLOWER, raw);
            assertEquals(FieldCrop.UNKNOWN, scarecrow.getCrop()); assertEquals(FieldPatchCondition.SCARECROW, scarecrow.getCondition());
        }
        for (int raw = 138; raw <= 140; raw++)
        {
            FieldPatchState snape = new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT, raw);
            assertEquals(FieldCrop.SNAPE_GRASS, snape.getCrop()); assertEquals(FieldPatchCondition.HARVESTABLE, snape.getCondition());
        }
    }
    @Test public void hopsAndFibreGrowthUsePinnedRatesAndReversedHarvestStages()
    {
        assertEquals(now.plusSeconds(2400), timer.estimate(observation(FieldFamily.HOPS,4,FarmingGrowthRate.NORMAL),now).get().getLatest());
        assertEquals(now.plusSeconds(3600), timer.estimate(observation(FieldFamily.HOPS,96,FarmingGrowthRate.NORMAL),now).get().getLatest());
        assertEquals(now.plusSeconds(720), timer.estimate(observation(FieldFamily.HOPS,96,FarmingGrowthRate.LEAGUES),now).get().getLatest());
        for(int raw=8;raw<=10;raw++)
        {
            FieldPatchState state = new FieldPatchStateDecoder().decode(FieldFamily.HOPS,raw);
            assertEquals(FieldCrop.HAMMERSTONE,state.getCrop()); assertEquals(10-raw,state.getStage());
            assertEquals(FieldPatchCondition.HARVESTABLE,state.getCondition());
        }
        assertEquals(FieldCrop.HEMP,new FieldPatchStateDecoder().decode(FieldFamily.HOPS,241).getCrop());
        assertEquals(FieldPatchCondition.DEAD,new FieldPatchStateDecoder().decode(FieldFamily.HOPS,241).getCondition());
    }

}
