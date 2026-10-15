package com.farmingnavigator.farming.tree;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreePatchObservationTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final TreePatchObservationStore store = new TreePatchObservationStore();
    private final TreePatchObservationSampler sampler = new TreePatchObservationSampler(store);
    private final WorldPoint varrock = new WorldPoint(3226, 3457, 0);

    @Test public void everyPatchBelongsToOneRegionAndChannelsAreDistinctWithinRegion()
    {
        Set<TreePatchDefinition> found = EnumSet.noneOf(TreePatchDefinition.class);
        for (TreePatchRegion region : TreePatchRegion.values())
        {
            Set<Integer> channels = new HashSet<>();
            for (TreePatchDefinition patch : region.getPatches())
            {
                assertTrue(found.add(patch));
                assertTrue(channels.add(patch.getVarbit()));
            }
        }
        assertEquals(19, found.size());
        assertEquals(EnumSet.allOf(TreePatchDefinition.class), found);
    }

    @Test public void recordedAnchorsMapToTheirExpectedRegions()
    {
        Object[][] cases = {
                {TreePatchRegion.VARROCK, 3226, 3457}, {TreePatchRegion.TAVERLEY, 2933, 3436},
                {TreePatchRegion.FALADOR, 3005, 3375}, {TreePatchRegion.LUMBRIDGE, 3189, 3233},
                {TreePatchRegion.GNOME_STRONGHOLD, 2434, 3418}, {TreePatchRegion.GNOME_STRONGHOLD, 2472, 3445},
                {TreePatchRegion.FARMING_GUILD, 1234, 3736}, {TreePatchRegion.FARMING_GUILD, 1242, 3755},
                {TreePatchRegion.AUBURNVALE, 1366, 3318}, {TreePatchRegion.KASTORI, 1347, 3058},
                {TreePatchRegion.BRIMHAVEN, 2765, 3211}, {TreePatchRegion.CATHERBY, 2858, 3432},
                {TreePatchRegion.LLETYA, 2343, 3160}, {TreePatchRegion.TREE_GNOME_VILLAGE, 2487, 3181},
                {TreePatchRegion.FOSSIL_ISLAND, 3707, 3838}, {TreePatchRegion.AVIUM_SAVANNAH, 1683, 2971},
                {TreePatchRegion.ANGLERS_RETREAT, 2472, 2705}};
        for (Object[] row : cases)
        {
            assertEquals(row[0], TreePatchRegion.forLocation(new WorldPoint((int) row[1], (int) row[2], 0)));
            assertNull(TreePatchRegion.forLocation(new WorldPoint((int) row[1], (int) row[2], 1)));
        }
    }

    @Test public void catherbyAndFossilIslandBoundsRejectReusedChannelsAtUnsafeLocations()
    {
        assertNull(TreePatchRegion.forLocation(new WorldPoint(2839, 3440, 0)));
        assertEquals(TreePatchRegion.CATHERBY, TreePatchRegion.forLocation(new WorldPoint(2840, 3440, 0)));
        assertEquals(TreePatchRegion.CATHERBY, TreePatchRegion.forLocation(new WorldPoint(2839, 3439, 0)));
        assertNull(TreePatchRegion.forLocation(new WorldPoint(3753, 3868, 0)));
        assertNull(TreePatchRegion.forLocation(new WorldPoint(3729, 3831, 0)));
        assertEquals(TreePatchRegion.FOSSIL_ISLAND, TreePatchRegion.forLocation(new WorldPoint(3752, 3868, 0)));
        assertNull(TreePatchRegion.forLocation(new WorldPoint(0, 0, 0)));
    }

    @Test public void waitsThreeDistinctSettledTicksAndRecordsOnlyTheRegionChannel()
    {
        AtomicInteger reads = new AtomicInteger();
        for (int tick = 1; tick <= 3; tick++)
        {
            boolean changed = sampler.sample(varrock, true, false, false, tick, id -> {
                assertEquals(VarbitID.FARMING_TRANSMIT_A, id);
                reads.incrementAndGet(); return 12;
            }, now.plusSeconds(tick), FarmingGrowthRate.NORMAL);
            assertEquals(tick == 3, changed);
            if (tick == 1) { assertFalse(sample(varrock, 1, false, false)); }
        }
        assertEquals(1, reads.get());
        assertEquals(TreePatchCondition.CHECK_HEALTH, store.snapshot().get(TreePatchDefinition.VARROCK_TREE).getState().getCondition());
    }

    @Test public void samplesAllThreeFossilPatchesWithoutSharingTheirRawStates()
    {
        WorldPoint fossil = new WorldPoint(3707, 3838, 0);
        for (int tick = 1; tick <= 3; tick++)
        {
            sampler.sample(fossil, true, false, false, tick,
                    id -> id == VarbitID.FARMING_TRANSMIT_A ? 15 : id == VarbitID.FARMING_TRANSMIT_B ? 38 : 114,
                    now.plusSeconds(tick), FarmingGrowthRate.NORMAL);
        }
        assertEquals(3, store.snapshot().size());
        assertEquals(TreeCrop.TEAK, store.snapshot().get(TreePatchDefinition.FOSSIL_ISLAND_EAST).getState().getCrop());
        assertEquals(TreeCrop.MAHOGANY, store.snapshot().get(TreePatchDefinition.FOSSIL_ISLAND_MIDDLE).getState().getCrop());
        assertEquals(TreeCrop.ROSEWOOD, store.snapshot().get(TreePatchDefinition.FOSSIL_ISLAND_WEST).getState().getCrop());
    }

    @Test public void guildReadsTreeGAndFruitKInsteadOfNearbyHerbE()
    {
        for (int tick = 1; tick <= 3; tick++)
        {
            sampler.sample(new WorldPoint(1234, 3736, 0), true, false, false, tick, id -> {
                assertTrue(id == VarbitID.FARMING_TRANSMIT_G || id == VarbitID.FARMING_TRANSMIT_K);
                return id == VarbitID.FARMING_TRANSMIT_G ? 12 : 226;
            }, now.plusSeconds(tick), FarmingGrowthRate.NORMAL);
        }
        assertEquals(TreeCrop.OAK, store.snapshot().get(TreePatchDefinition.FARMING_GUILD_TREE).getState().getCrop());
        assertEquals(TreeCrop.PALM, store.snapshot().get(TreePatchDefinition.FARMING_GUILD_FRUIT).getState().getCrop());
    }

    @Test public void travelModalInstanceAndTickGapRestartSettling()
    {
        sample(varrock, 1, false, false);
        sample(varrock, 2, false, false);
        assertFalse(sample(new WorldPoint(3005, 3375, 0), 3, false, false));
        assertTrue(store.isEmpty());
        assertFalse(sample(varrock, 4, true, false));
        assertFalse(sample(varrock, 5, false, true));
        assertFalse(sample(varrock, 6, false, false));
        assertFalse(sample(varrock, 8, false, false));
        assertFalse(sample(varrock, 9, false, false));
        assertTrue(sample(varrock, 10, false, false));
        sampler.invalidateRegion();
        assertFalse(sample(varrock, 11, false, false));
        assertFalse(sample(varrock, 12, false, false));
    }

    @Test public void snapshotCachingThrottlesUnchangedValuesButReplacesOldCertaintyWithUnknown()
    {
        assertTrue(store.observe(TreePatchDefinition.VARROCK_TREE, 12, now, FarmingGrowthRate.NORMAL));
        Map<TreePatchDefinition, TreePatchObservation> earlier = store.snapshot();
        assertSame(earlier, store.snapshot());
        assertFalse(store.observe(TreePatchDefinition.VARROCK_TREE, 12, now.plusSeconds(29), FarmingGrowthRate.NORMAL));
        assertTrue(store.observe(TreePatchDefinition.VARROCK_TREE, 12, now.plusSeconds(30), FarmingGrowthRate.NORMAL));
        assertTrue(store.observe(TreePatchDefinition.VARROCK_TREE, 76, now.plusSeconds(31), FarmingGrowthRate.NORMAL));
        assertFalse(store.snapshot().get(TreePatchDefinition.VARROCK_TREE).getState().isKnown());
        assertTrue(earlier.get(TreePatchDefinition.VARROCK_TREE).getState().isKnown());
        assertFalse(store.observe(TreePatchDefinition.VARROCK_TREE, 12, now, FarmingGrowthRate.NORMAL));
    }

    @Test public void logoutClearsSessionData()
    {
        sample(varrock, 1, false, false); sample(varrock, 2, false, false); sample(varrock, 3, false, false);
        assertFalse(store.isEmpty());
        sampler.sample(varrock, false, false, false, 4, id -> 12, now, FarmingGrowthRate.NORMAL);
        assertTrue(store.isEmpty());
    }

    private boolean sample(WorldPoint location, int tick, boolean modal, boolean instance)
    {
        return sampler.sample(location, true, modal, instance, tick, id -> 12, now.plusSeconds(tick), FarmingGrowthRate.NORMAL);
    }
}
