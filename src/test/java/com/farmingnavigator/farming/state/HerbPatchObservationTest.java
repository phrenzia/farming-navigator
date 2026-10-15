package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HerbPatchObservationTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final WorldPoint falador = new WorldPoint(3056, 3312, 0);

    @Test
    public void mapsEveryHerbPatchToItsOwnTransmitChannel()
    {
        assertRegion(HerbPatchRegion.FALADOR, 3056, 3312, VarbitID.FARMING_TRANSMIT_D);
        assertRegion(HerbPatchRegion.CATHERBY, 2800, 3480, VarbitID.FARMING_TRANSMIT_D);
        assertRegion(HerbPatchRegion.ARDOUGNE, 2660, 3370, VarbitID.FARMING_TRANSMIT_D);
        assertRegion(HerbPatchRegion.MORYTANIA, 3600, 3520, VarbitID.FARMING_TRANSMIT_D);
        assertRegion(HerbPatchRegion.HOSIDIUS, 1750, 3550, VarbitID.FARMING_TRANSMIT_D);
        assertRegion(HerbPatchRegion.TROLL_STRONGHOLD, 2820, 3680, VarbitID.FARMING_TRANSMIT_A);
        assertRegion(HerbPatchRegion.WEISS, 2820, 3920, VarbitID.FARMING_TRANSMIT_A);
        assertRegion(HerbPatchRegion.FARMING_GUILD, 1248, 3725, VarbitID.FARMING_TRANSMIT_E);
        assertRegion(HerbPatchRegion.HARMONY_ISLAND, 3780, 2830, VarbitID.FARMING_TRANSMIT_B);
        assertRegion(HerbPatchRegion.ORTUS_FARM, 1570, 3090, VarbitID.FARMING_TRANSMIT_D);
    }

    @Test
    public void rejectsOtherLocationsFloorsAndPortSarimBoundary()
    {
        assertNull(HerbPatchRegion.forLocation(null));
        assertNull(HerbPatchRegion.forLocation(new WorldPoint(3200, 3200, 0)));
        assertNull(HerbPatchRegion.forLocation(new WorldPoint(3056, 3312, 1)));
        assertNull(HerbPatchRegion.forLocation(new WorldPoint(3056, 3271, 0)));
        assertEquals(HerbPatchRegion.FALADOR, HerbPatchRegion.forLocation(new WorldPoint(3056, 3272, 0)));
    }

    @Test
    public void respectsCatherbyFruitTreeTransmitBoundary()
    {
        assertEquals(HerbPatchRegion.CATHERBY, HerbPatchRegion.forLocation(new WorldPoint(2839, 3440, 0)));
        assertNull(HerbPatchRegion.forLocation(new WorldPoint(2840, 3440, 0)));
        assertNull(HerbPatchRegion.forLocation(new WorldPoint(2839, 3439, 0)));
        assertNull(HerbPatchRegion.forLocation(new WorldPoint(2839, 3440, 1)));
    }

    @Test
    public void snapshotsAreImmutableAndIndependentAndUnknownReplacesKnown()
    {
        HerbPatchObservationStore store = new HerbPatchObservationStore();
        assertTrue(store.snapshot().isEmpty());
        store.observe(HerbPatchDefinition.FALADOR, 36, now);
        Map<HerbPatchDefinition, HerbPatchObservation> snapshot = store.snapshot();
        store.observe(HerbPatchDefinition.FALADOR, 220, now.plusSeconds(1));
        assertEquals(PatchCondition.HARVESTABLE, snapshot.get(HerbPatchDefinition.FALADOR).getState().getCondition());
        assertFalse(store.snapshot().get(HerbPatchDefinition.FALADOR).getState().isKnown());
        try
        {
            snapshot.clear();
            throw new AssertionError("Snapshot must be immutable");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected immutable snapshot.
        }
        store.clear();
        assertTrue(store.snapshot().isEmpty());
        assertEquals(1, snapshot.size());
    }

    @Test
    public void throttlesUnchangedObservationsButRecordsStateChangesImmediately()
    {
        HerbPatchObservationStore store = new HerbPatchObservationStore();
        assertTrue(store.observe(HerbPatchDefinition.FALADOR, 32, now));
        assertFalse(store.observe(HerbPatchDefinition.FALADOR, 32, now.plusSeconds(29)));
        assertTrue(store.observe(HerbPatchDefinition.FALADOR, 32, now.plusSeconds(30)));
        assertFalse(store.observe(HerbPatchDefinition.FALADOR, 36, now));
        assertTrue(store.observe(HerbPatchDefinition.FALADOR, 36, now.plusSeconds(31)));
        assertEquals(now.plusSeconds(31), store.snapshot().get(HerbPatchDefinition.FALADOR).getObservedAt());
    }

    @Test
    public void waitsForStableRegionBeforeReadingReusedVarbit()
    {
        HerbPatchObservationStore store = new HerbPatchObservationStore();
        HerbPatchObservationSampler sampler = new HerbPatchObservationSampler(store);
        AtomicInteger reads = new AtomicInteger();
        for (int tick = 1; tick <= 3; tick++)
        {
            boolean changed = sampler.sample(falador, true, false, false, tick,
                    varbit -> { assertEquals(VarbitID.FARMING_TRANSMIT_D, varbit); reads.incrementAndGet(); return 32; }, now);
            assertEquals(tick == 3, changed);
        }
        assertEquals(1, reads.get());
        assertEquals(PatchCondition.GROWING, store.snapshot().get(HerbPatchDefinition.FALADOR).getState().getCondition());
        sampler.sample(new WorldPoint(2660, 3370, 0), true, false, false, 4,
                id -> { throw new AssertionError("Travel must wait for a fresh region"); }, now);
        assertFalse(store.snapshot().containsKey(HerbPatchDefinition.ARDOUGNE));
    }

    @Test
    public void modalInstancesAndSkippedTicksResetSettlingPeriod()
    {
        HerbPatchObservationStore store = new HerbPatchObservationStore();
        HerbPatchObservationSampler sampler = new HerbPatchObservationSampler(store);
        assertFalse(sampler.sample(falador, true, true, false, 1, id -> 36, now));
        assertFalse(sampler.sample(falador, true, false, true, 2, id -> 36, now));
        assertFalse(sampler.sample(falador, true, false, false, 3, id -> 36, now));
        assertFalse(sampler.sample(falador, true, false, false, 4, id -> 36, now));
        assertFalse(sampler.sample(falador, true, false, false, 6, id -> 36, now));
        assertFalse(sampler.sample(falador, true, false, false, 7, id -> 36, now));
        assertTrue(sampler.sample(falador, true, false, false, 8, id -> 36, now));
        sampler.sample(falador, true, false, false, 8,
                id -> { throw new AssertionError("Duplicate ticks must not resample"); }, now);
        assertFalse(sampler.sample(falador, true, true, false, 9, id -> 36, now));
        assertFalse(sampler.sample(falador, true, false, false, 10, id -> 36, now));
        assertFalse(sampler.sample(falador, true, false, false, 11, id -> 36, now));
        assertTrue(sampler.sample(falador, true, false, false, 12, id -> 32, now.plusSeconds(1)));
    }

    @Test
    public void mapLoadingKeepsHistoryButRequiresSettlingAgain()
    {
        HerbPatchObservationStore store = new HerbPatchObservationStore();
        HerbPatchObservationSampler sampler = new HerbPatchObservationSampler(store);
        store.observe(HerbPatchDefinition.WEISS, 36, now);
        sampler.sample(falador, true, false, false, 1, id -> 32, now);
        sampler.sample(falador, true, false, false, 2, id -> 32, now);
        sampler.invalidateRegion();
        assertTrue(store.snapshot().containsKey(HerbPatchDefinition.WEISS));
        assertFalse(sampler.sample(falador, true, false, false, 3, id -> 32, now));
        assertFalse(sampler.sample(falador, true, false, false, 4, id -> 32, now));
        assertTrue(sampler.sample(falador, true, false, false, 5, id -> 32, now));
    }

    @Test
    public void logoutAndExplicitResetClearSessionObservations()
    {
        HerbPatchObservationStore store = new HerbPatchObservationStore();
        HerbPatchObservationSampler sampler = new HerbPatchObservationSampler(store);
        store.observe(HerbPatchDefinition.WEISS, 36, now);
        sampler.sample(falador, false, false, false, 1, id -> 36, now);
        assertTrue(store.snapshot().isEmpty());
        store.observe(HerbPatchDefinition.WEISS, 36, now);
        sampler.reset();
        assertTrue(store.snapshot().isEmpty());
    }

    private void assertRegion(HerbPatchRegion expected, int x, int y, int varbit)
    {
        HerbPatchRegion region = HerbPatchRegion.forLocation(new WorldPoint(x, y, 0));
        assertEquals(expected, region);
        assertEquals(varbit, region.getVarbit());
        assertEquals(HerbPatchDefinition.valueOf(expected.name()), region.getPatch());
    }
}
