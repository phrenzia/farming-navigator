package com.farmingnavigator.farming.field;

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

public class FieldObservationTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final FieldPatchObservationStore store = new FieldPatchObservationStore();
    private final FieldPatchObservationSampler sampler = new FieldPatchObservationSampler(store);
    @Test public void allThirtyOnePatchAnchorsBelongToOneRegionAndGuildChannelsAreDistinct()
    {
        Set<FieldPatchDefinition> found = EnumSet.noneOf(FieldPatchDefinition.class);
        for (FieldPatchRegion region : FieldPatchRegion.values())
        {
            Set<Integer> channels = new HashSet<>();
            for (FieldPatchDefinition patch : region.getPatches())
            {
                assertTrue(found.add(patch)); assertTrue(channels.add(patch.getVarbit()));
                assertEquals(region, FieldPatchRegion.forLocation(patch.getLocation()));
                assertNull(FieldPatchRegion.forLocation(new WorldPoint(patch.getLocation().getX(), patch.getLocation().getY(), 1)));
            }
        }
        assertEquals(31, found.size()); assertEquals(EnumSet.allOf(FieldPatchDefinition.class), found);
        assertNull(FieldPatchRegion.forLocation(null)); assertNull(FieldPatchRegion.forLocation(new WorldPoint(0, 0, 0)));
    }
    @Test public void threeDistinctTicksReadOnlyTheCurrentRegionChannels()
    {
        AtomicInteger reads = new AtomicInteger();
        for (int tick = 1; tick <= 3; tick++)
        {
            assertEquals(tick == 3, sampler.sample(FieldPatchDefinition.FARMING_GUILD_NORTH.getLocation(), true, false, false, tick, id -> {
                reads.incrementAndGet();
                assertTrue(id == VarbitID.FARMING_TRANSMIT_C || id == VarbitID.FARMING_TRANSMIT_D || id == VarbitID.FARMING_TRANSMIT_H);
                return id == VarbitID.FARMING_TRANSMIT_H ? 41 : id == VarbitID.FARMING_TRANSMIT_D ? 17 : 10;
            }, now.plusSeconds(tick), FarmingGrowthRate.NORMAL));
        }
        assertEquals(3, reads.get()); assertEquals(3, store.snapshot().size());
        assertEquals(FieldCrop.POTATO, store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_NORTH).getState().getCrop());
        assertEquals(FieldCrop.WHITE_LILY, store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_FLOWER).getState().getCrop());
        assertEquals(FieldCrop.ONION, store.snapshot().get(FieldPatchDefinition.FARMING_GUILD_SOUTH).getState().getCrop());
        assertFalse(sampler.sample(FieldPatchDefinition.FARMING_GUILD_NORTH.getLocation(), true, false, false, 3,
                id -> { fail("Repeated tick read"); return 0; }, now.plusSeconds(4), FarmingGrowthRate.NORMAL));
    }
    @Test public void modalInstanceTravelAndTickGapsRestartSettlingBeforeSharedChannelAIsRead()
    {
        WorldPoint prif = FieldPatchDefinition.PRIFDDINAS_FLOWER.getLocation();
        for (int tick = 1; tick <= 3; tick++) { sample(prif, false, false, tick, 12); }
        WorldPoint falador = FieldPatchDefinition.FALADOR_NORTH.getLocation();
        assertFalse(sample(falador, false, false, 4, 10));
        assertFalse(sample(falador, true, false, 5, 10));
        assertFalse(sample(falador, false, true, 6, 10));
        assertFalse(sample(falador, false, false, 7, 10));
        assertFalse(sample(falador, false, false, 9, 10));
        assertFalse(sample(falador, false, false, 10, 10));
        assertTrue(sample(falador, false, false, 11, 10));
        assertEquals(FieldCrop.MARIGOLD, store.snapshot().get(FieldPatchDefinition.PRIFDDINAS_FLOWER).getState().getCrop());
        assertEquals(FieldCrop.POTATO, store.snapshot().get(FieldPatchDefinition.FALADOR_NORTH).getState().getCrop());
    }
    @Test public void cachedHistoryThrottlesUnchangedReadingsButRetainsNewUnknownAndModeEvidence()
    {
        FieldPatchDefinition patch = FieldPatchDefinition.FARMING_GUILD_NORTH;
        assertTrue(store.observe(patch, 10, now, FarmingGrowthRate.NORMAL));
        Object earlier = store.snapshot(); assertSame(earlier, store.snapshot());
        assertFalse(store.observe(patch, 10, now.plusSeconds(29), FarmingGrowthRate.NORMAL));
        assertTrue(store.observe(patch, 10, now.plusSeconds(29), FarmingGrowthRate.LEAGUES));
        assertTrue(store.observe(patch, 256, now.plusSeconds(30), FarmingGrowthRate.LEAGUES));
        assertFalse(store.snapshot().get(patch).getState().isKnown());
        assertFalse(store.observe(patch, 10, now, FarmingGrowthRate.NORMAL));
        try { store.snapshot().clear(); fail("Mutable history"); } catch (UnsupportedOperationException expected) { }
    }
    @Test public void logoutClearsHistoryAndArrivalEvidence()
    {
        for (int tick = 1; tick <= 3; tick++) { sample(FieldPatchDefinition.PRIFDDINAS_FLOWER.getLocation(), false, false, tick, 12); }
        assertFalse(store.isEmpty());
        sampler.sample(null, false, false, false, 4, id -> 0, now, FarmingGrowthRate.NORMAL);
        assertTrue(store.isEmpty());
    }
    @Test public void overlappingCatherbyAndPortSarimChannelsAreExcluded()
    {
        assertNull(FieldPatchRegion.forLocation(new WorldPoint(3052, 3271, 0)));
        assertEquals(FieldPatchRegion.FALADOR, FieldPatchRegion.forLocation(new WorldPoint(3052, 3272, 0)));
        assertNull(FieldPatchRegion.forLocation(new WorldPoint(2840, 3440, 0)));
        assertEquals(FieldPatchRegion.CATHERBY, FieldPatchRegion.forLocation(new WorldPoint(2839, 3440, 0)));
        assertNull(FieldPatchRegion.forLocation(new WorldPoint(2839, 3439, 0)));
    }
    @Test public void hopsChannelARequiresResettlingWhenMovingBetweenRegions()
    {
        for(int tick=1;tick<=3;tick++) { sample(FieldPatchDefinition.YANILLE_HOPS.getLocation(),false,false,tick,4); }
        assertEquals(FieldCrop.HAMMERSTONE,store.snapshot().get(FieldPatchDefinition.YANILLE_HOPS).getState().getCrop());
        assertFalse(sample(FieldPatchDefinition.LUMBRIDGE_HOPS.getLocation(),false,false,4,74));
        assertFalse(sample(FieldPatchDefinition.LUMBRIDGE_HOPS.getLocation(),false,false,5,74));
        assertTrue(sample(FieldPatchDefinition.LUMBRIDGE_HOPS.getLocation(),false,false,6,74));
        assertEquals(FieldCrop.BARLEY,store.snapshot().get(FieldPatchDefinition.LUMBRIDGE_HOPS).getState().getCrop());
        assertEquals(2,store.snapshot().size());
    }
    private boolean sample(WorldPoint point, boolean modal, boolean instance, int tick, int raw)
    {
        return sampler.sample(point, true, modal, instance, tick, id -> raw, now.plusSeconds(tick), FarmingGrowthRate.NORMAL);
    }
}
