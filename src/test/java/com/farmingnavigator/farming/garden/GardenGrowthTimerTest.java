package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import net.runelite.client.plugins.timetracking.farming.Produce;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenGrowthTimerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private GardenPatchObservation observation(GardenFamily family, int raw, FarmingGrowthRate rate)
    {
        return new GardenPatchObservation(new GardenPatchStateDecoder().decode(family, raw), now, rate);
    }
    @Test public void cropMetadataMatchesPublicRuneLiteProduceForAllEightCrops()
    {
        int count = 0;
        for (GardenCrop crop : GardenCrop.values())
        {
            if (crop == GardenCrop.UNKNOWN) { continue; }
            Produce source = Produce.valueOf(crop.name());
            assertEquals(source.getPatchImplementation().name(), crop.getFamily().name());
            assertEquals(source.getStages(), crop.getStages()); assertEquals(source.getTickrate(), crop.getGrowthCycleMinutes());
            count++;
        }
        assertEquals(8, count);
        assertFalse(new GardenPatchStateDecoder().decode(null, 250).isKnown());
    }
    @Test public void bushAndCactusGrowthHaveDifferentConditionalWindows()
    {
        GardenGrowthTimer timer = new GardenGrowthTimer();
        GardenGrowthWindow bush = timer.estimate(observation(GardenFamily.BUSH, 5, FarmingGrowthRate.NORMAL), now).get();
        assertEquals(now.plusSeconds(80 * 60), bush.getEarliest()); assertEquals(now.plusSeconds(100 * 60), bush.getLatest());
        GardenGrowthWindow cactus = timer.estimate(observation(GardenFamily.CACTUS, 8, FarmingGrowthRate.NORMAL), now).get();
        assertEquals(now.plusSeconds(480 * 60), cactus.getEarliest()); assertEquals(now.plusSeconds(560 * 60), cactus.getLatest());
        GardenGrowthWindow potato = timer.estimate(observation(GardenFamily.CACTUS, 32, FarmingGrowthRate.NORMAL), now).get();
        assertEquals(now.plusSeconds(60 * 60), potato.getEarliest()); assertEquals(now.plusSeconds(70 * 60), potato.getLatest());
        assertEquals(now.plusSeconds(14 * 60), timer.estimate(observation(GardenFamily.CACTUS, 32, FarmingGrowthRate.LEAGUES), now).get().getLatest());
    }
    @Test public void healthChecksMaturePlantsDiseaseDeathAndUncertainEvidenceHaveNoGrowthTimer()
    {
        GardenGrowthTimer timer = new GardenGrowthTimer();
        for (int raw : new int[]{0, 1, 15, 18, 19, 25, 31, 39, 58, -1})
        {
            assertFalse(timer.estimate(observation(GardenFamily.CACTUS, raw, FarmingGrowthRate.NORMAL), now).isPresent());
        }
        assertFalse(timer.estimate(observation(GardenFamily.BUSH, 5, FarmingGrowthRate.UNKNOWN), now).isPresent());
        assertFalse(timer.estimate(observation(GardenFamily.BUSH, 5, FarmingGrowthRate.NORMAL), now.minusSeconds(1)).isPresent());
    }
}
