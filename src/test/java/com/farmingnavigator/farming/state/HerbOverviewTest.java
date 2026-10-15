package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class HerbOverviewTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");

    private HerbPatchStatus status(HerbPatchDefinition patch) { return new HerbPatchStatus(patch, Collections.emptyList()); }
    private HerbPatchObservation observed(int raw, Instant at, FarmingGrowthRate rate)
    {
        return new HerbPatchObservation(new HerbPatchStateDecoder().decode(raw), at, rate);
    }
    private HerbOverview overview(int raw, Instant at, FarmingGrowthRate rate)
    {
        return HerbOverview.create(Collections.singletonList(status(HerbPatchDefinition.FALADOR)),
                EnumSet.of(HerbPatchDefinition.FALADOR), Collections.singletonMap(HerbPatchDefinition.FALADOR, observed(raw, at, rate)), now);
    }

    @Test public void separatesEveryObservedConditionFromEstimates()
    {
        int[] raw = {128, 170, 8, 0, 3, 4, 110};
        HerbOverview.Group[] groups = {HerbOverview.Group.DISEASED, HerbOverview.Group.DEAD, HerbOverview.Group.READY,
                HerbOverview.Group.PLANTING, HerbOverview.Group.PLANTING, HerbOverview.Group.GROWING, HerbOverview.Group.UNKNOWN};
        for (int i = 0; i < raw.length; i++)
        {
            HerbOverview overview = overview(raw[i], now, FarmingGrowthRate.NORMAL);
            assertEquals(1, overview.getCount(groups[i]));
            assertEquals(groups[i], overview.getEntries().get(0).getGroup());
        }
    }

    @Test public void windowBoundaryOnlyCreatesPossibleReadiness()
    {
        assertEquals(1, overview(4, now.minusSeconds(3599), FarmingGrowthRate.NORMAL).getCount(HerbOverview.Group.GROWING));
        HerbOverview boundary = overview(4, now.minusSeconds(3600), FarmingGrowthRate.NORMAL);
        assertEquals(1, boundary.getCount(HerbOverview.Group.POSSIBLY_READY));
        assertEquals(0, boundary.getCount(HerbOverview.Group.READY));
        assertEquals(1, overview(4, now.minusSeconds(7200), FarmingGrowthRate.NORMAL).getCount(HerbOverview.Group.POSSIBLY_READY));
    }

    @Test public void leaguesAndUnknownRatesRemainDistinct()
    {
        assertEquals(1, overview(4, now.minusSeconds(720), FarmingGrowthRate.LEAGUES).getCount(HerbOverview.Group.POSSIBLY_READY));
        assertEquals(1, overview(4, now.minusSeconds(720), FarmingGrowthRate.NORMAL).getCount(HerbOverview.Group.GROWING));
        assertEquals(1, overview(4, now.minusSeconds(99999), FarmingGrowthRate.UNKNOWN).getCount(HerbOverview.Group.GROWING));
    }

    @Test public void futureAndMissingObservationsAreUnknown()
    {
        HerbOverview future = overview(8, now.plusSeconds(1), FarmingGrowthRate.NORMAL);
        assertEquals(1, future.getCount(HerbOverview.Group.UNKNOWN));
        assertNull(future.getEntries().get(0).getObservation());
        HerbOverview missing = HerbOverview.create(Collections.singletonList(status(HerbPatchDefinition.FALADOR)),
                EnumSet.of(HerbPatchDefinition.FALADOR), Collections.emptyMap(), now);
        assertEquals(1, missing.getCount(HerbOverview.Group.UNKNOWN));
    }

    @Test public void selectedLockedExcludedAndDuplicateStatusesDoNotInflateCounts()
    {
        HerbOverview result = HerbOverview.create(Arrays.asList(status(HerbPatchDefinition.FALADOR), status(HerbPatchDefinition.FALADOR),
                status(HerbPatchDefinition.CATHERBY), new HerbPatchStatus(HerbPatchDefinition.WEISS, Collections.singletonList("Fire required"))),
                EnumSet.of(HerbPatchDefinition.FALADOR, HerbPatchDefinition.WEISS), Collections.emptyMap(), now);
        assertEquals(1, result.getLockedCount());
        assertEquals(1, result.getEntries().size());
        assertEquals(HerbPatchDefinition.FALADOR, result.getEntries().get(0).getPatch());
    }

    @Test public void problemPatchesSortBeforeReadyAndUnknownInDeterministicOrder()
    {
        HerbOverview result = HerbOverview.create(Arrays.asList(status(HerbPatchDefinition.CATHERBY),
                status(HerbPatchDefinition.ARDOUGNE), status(HerbPatchDefinition.FALADOR)), EnumSet.allOf(HerbPatchDefinition.class),
                Map.of(HerbPatchDefinition.FALADOR, observed(8, now, FarmingGrowthRate.NORMAL),
                        HerbPatchDefinition.CATHERBY, observed(170, now, FarmingGrowthRate.NORMAL),
                        HerbPatchDefinition.ARDOUGNE, observed(128, now, FarmingGrowthRate.NORMAL)), now);
        assertEquals(HerbPatchDefinition.ARDOUGNE, result.getEntries().get(0).getPatch());
        assertEquals(HerbPatchDefinition.CATHERBY, result.getEntries().get(1).getPatch());
        assertEquals(HerbPatchDefinition.FALADOR, result.getEntries().get(2).getPatch());
    }

    @Test public void historicReadyRemainsAnObservationWithItsOriginalAge()
    {
        Instant earlier = now.minusSeconds(86400);
        HerbOverview result = overview(8, earlier, FarmingGrowthRate.NORMAL);
        assertEquals(earlier, result.getEntries().get(0).getObservation().getObservedAt());
        assertTrue(result.getEntries().get(0).getGroup().getLabel().startsWith("Last observed"));
    }

    @Test(expected = UnsupportedOperationException.class) public void entriesCannotBeMutated()
    {
        overview(8, now, FarmingGrowthRate.NORMAL).getEntries().clear();
    }
}
