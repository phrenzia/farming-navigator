package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenObservationTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final GardenPatchObservationStore store = new GardenPatchObservationStore();
    private final GardenPatchObservationSampler sampler = new GardenPatchObservationSampler(store);
    @Test public void allSevenPatchAnchorsBelongToOneRegionAndGuildChannelsAreDistinct()
    {
        Set<GardenPatchDefinition> found = EnumSet.noneOf(GardenPatchDefinition.class);
        for (GardenPatchRegion region : GardenPatchRegion.values())
        {
            Set<Integer> channels = new HashSet<>();
            for (GardenPatchDefinition patch : region.getPatches())
            {
                assertTrue(found.add(patch)); assertTrue(channels.add(patch.getVarbit()));
                assertEquals(region, GardenPatchRegion.forLocation(patch.getLocation()));
                assertNull(GardenPatchRegion.forLocation(new WorldPoint(patch.getLocation().getX(), patch.getLocation().getY(), 1)));
            }
        }
        assertEquals(7, found.size()); assertEquals(EnumSet.allOf(GardenPatchDefinition.class), found);
        assertNull(GardenPatchRegion.forLocation(null)); assertNull(GardenPatchRegion.forLocation(new WorldPoint(0, 0, 0)));
    }
    @Test public void threeDistinctTicksReadOnlyTheCurrentRegionChannels()
    {
        AtomicInteger reads = new AtomicInteger();
        for (int tick = 1; tick <= 3; tick++)
        {
            assertEquals(tick == 3, sampler.sample(GardenPatchDefinition.FARMING_GUILD_BUSH.getLocation(), true, false, false, tick, id -> {
                reads.incrementAndGet();
                assertTrue(id == VarbitID.FARMING_TRANSMIT_B || id == VarbitID.FARMING_TRANSMIT_F);
                return id == VarbitID.FARMING_TRANSMIT_B ? 250 : 58;
            }, now.plusSeconds(tick), FarmingGrowthRate.NORMAL));
        }
        assertEquals(2, reads.get()); assertEquals(2, store.snapshot().size());
        assertEquals(GardenCrop.REDBERRIES, store.snapshot().get(GardenPatchDefinition.FARMING_GUILD_BUSH).getState().getCrop());
        assertEquals(GardenCrop.POTATO_CACTUS, store.snapshot().get(GardenPatchDefinition.FARMING_GUILD_CACTUS).getState().getCrop());
        assertFalse(sampler.sample(GardenPatchDefinition.FARMING_GUILD_BUSH.getLocation(), true, false, false, 3,
                id -> { fail("Repeated tick read"); return 0; }, now.plusSeconds(4), FarmingGrowthRate.NORMAL));
    }
    @Test public void modalInstanceTravelAndTickGapsRestartSettlingBeforeSharedChannelAIsRead()
    {
        WorldPoint cactus = GardenPatchDefinition.AL_KHARID_CACTUS.getLocation();
        for (int tick = 1; tick <= 3; tick++) { sample(cactus, false, false, tick, 31); }
        WorldPoint bush = GardenPatchDefinition.RIMMINGTON_BUSH.getLocation();
        assertFalse(sample(bush, false, false, 4, 250));
        assertFalse(sample(bush, true, false, 5, 250));
        assertFalse(sample(bush, false, true, 6, 250));
        assertFalse(sample(bush, false, false, 7, 250));
        assertFalse(sample(bush, false, false, 9, 250));
        assertFalse(sample(bush, false, false, 10, 250));
        assertTrue(sample(bush, false, false, 11, 250));
        assertEquals(GardenCrop.CACTUS, store.snapshot().get(GardenPatchDefinition.AL_KHARID_CACTUS).getState().getCrop());
        assertEquals(GardenCrop.REDBERRIES, store.snapshot().get(GardenPatchDefinition.RIMMINGTON_BUSH).getState().getCrop());
    }
    @Test public void cachedHistoryThrottlesUnchangedReadingsButRetainsNewUnknownAndModeEvidence()
    {
        GardenPatchDefinition patch = GardenPatchDefinition.FARMING_GUILD_BUSH;
        assertTrue(store.observe(patch, 250, now, FarmingGrowthRate.NORMAL));
        Object earlier = store.snapshot(); assertSame(earlier, store.snapshot());
        assertFalse(store.observe(patch, 250, now.plusSeconds(29), FarmingGrowthRate.NORMAL));
        assertTrue(store.observe(patch, 250, now.plusSeconds(29), FarmingGrowthRate.LEAGUES));
        assertTrue(store.observe(patch, 256, now.plusSeconds(30), FarmingGrowthRate.LEAGUES));
        assertFalse(store.snapshot().get(patch).getState().isKnown());
        assertFalse(store.observe(patch, 250, now, FarmingGrowthRate.NORMAL));
        try { store.snapshot().clear(); fail("Mutable history"); } catch (UnsupportedOperationException expected) { }
    }
    @Test public void logoutClearsHistoryAndArrivalEvidence()
    {
        for (int tick = 1; tick <= 3; tick++) { sample(GardenPatchDefinition.AL_KHARID_CACTUS.getLocation(), false, false, tick, 31); }
        assertFalse(store.isEmpty());
        sampler.sample(null, false, false, false, 4, id -> 0, now, FarmingGrowthRate.NORMAL);
        assertTrue(store.isEmpty());
    }
    private boolean sample(WorldPoint point, boolean modal, boolean instance, int tick, int raw)
    {
        return sampler.sample(point, true, modal, instance, tick, id -> raw, now.plusSeconds(tick), FarmingGrowthRate.NORMAL);
    }
}
