package com.farmingnavigator.farming.tree;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeGrowthTimerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private Optional<TreeGrowthWindow> estimate(TreeFamily family, int raw, FarmingGrowthRate rate, Instant observed)
    {
        return new TreeGrowthTimer().estimate(new TreePatchObservation(new TreePatchStateDecoder().decode(family, raw), observed, rate), now);
    }

    @Test public void familyWindowsUseVerifiedCycleAndStageCounts()
    {
        TreeGrowthWindow oak = estimate(TreeFamily.TREE, 8, FarmingGrowthRate.NORMAL, now).get();
        assertEquals(now.plusSeconds(120 * 60), oak.getEarliest());
        assertEquals(now.plusSeconds(160 * 60), oak.getLatest());
        TreeGrowthWindow apple = estimate(TreeFamily.FRUIT_TREE, 8, FarmingGrowthRate.NORMAL, now).get();
        assertEquals(now.plusSeconds(800 * 60), apple.getEarliest());
        assertEquals(now.plusSeconds(960 * 60), apple.getLatest());
        TreeGrowthWindow teak = estimate(TreeFamily.HARDWOOD_TREE, 8, FarmingGrowthRate.NORMAL, now).get();
        assertEquals(now.plusSeconds(3840 * 60), teak.getEarliest());
        assertEquals(now.plusSeconds(4480 * 60), teak.getLatest());
    }

    @Test public void seasonalCyclesAreFiveTimesFasterAndUnknownRateHasNoEstimate()
    {
        assertEquals(now.plusSeconds(32 * 60), estimate(TreeFamily.TREE, 8, FarmingGrowthRate.LEAGUES, now).get().getLatest());
        assertFalse(estimate(TreeFamily.TREE, 8, FarmingGrowthRate.UNKNOWN, now).isPresent());
    }

    @Test public void specialTreesUseTheirOwnCyclesAndStopEstimatingAfterHealthCheck()
    {
        Object[][] cases = {{TreeFamily.CALQUAT, 4, 34, 1120, 1280},
                {TreeFamily.CELASTRUS, 8, 13, 640, 800},
                {TreeFamily.REDWOOD, 8, 37, 5760, 6400},
                {TreeFamily.CRYSTAL_TREE, 8, 14, 400, 480}};
        for (Object[] row : cases)
        {
            TreeFamily family = (TreeFamily) row[0];
            TreeGrowthWindow window = estimate(family, (int) row[1], FarmingGrowthRate.NORMAL, now).get();
            assertEquals(family.name(), now.plusSeconds((int) row[3] * 60L), window.getEarliest());
            assertEquals(family.name(), now.plusSeconds((int) row[4] * 60L), window.getLatest());
            assertFalse(estimate(family, (int) row[2], FarmingGrowthRate.NORMAL, now).isPresent());
        }
        for (int raw : new int[]{14, 16, 17, 28})
        {
            assertFalse(estimate(TreeFamily.CELASTRUS, raw, FarmingGrowthRate.NORMAL, now).isPresent());
        }
    }

    @Test public void finalGrowingStageHasWindowStartingNowWithoutBecomingObservedReady()
    {
        TreeGrowthWindow finalStage = estimate(TreeFamily.TREE, 11, FarmingGrowthRate.NORMAL, now).get();
        assertEquals(now, finalStage.getEarliest());
        assertEquals(now.plusSeconds(2400), finalStage.getLatest());
    }

    @Test public void checkHealthCheckedStumpsDiseaseDeathAndFutureDataHaveNoGrowthEstimate()
    {
        for (int raw : new int[]{0, 1, 12, 13, 14, 73, 137, 76})
        {
            assertFalse(Integer.toString(raw), estimate(TreeFamily.TREE, raw, FarmingGrowthRate.NORMAL, now).isPresent());
        }
        assertFalse(estimate(TreeFamily.TREE, 8, FarmingGrowthRate.NORMAL, now.plusSeconds(1)).isPresent());
    }
}
