package com.farmingnavigator.farming.state;

import java.time.Instant;
import java.util.function.IntUnaryOperator;
import net.runelite.api.coords.WorldPoint;

/** Avoids recording reused varbits immediately after travel, login, or modal closure. */
public class HerbPatchObservationSampler
{
    private final HerbPatchObservationStore store;
    private HerbPatchRegion candidate;
    private int stableTicks;
    private int lastTick = -1;

    public HerbPatchObservationSampler(HerbPatchObservationStore store)
    {
        this.store = store;
    }

    public boolean sample(WorldPoint location, boolean loggedIn, boolean modalOpen,
            boolean instanced, int tick, IntUnaryOperator readVarbit, Instant now)
    {
        if (!loggedIn)
        {
            reset();
            return false;
        }
        if (modalOpen || instanced)
        {
            resetCandidate();
            return false;
        }
        if (tick == lastTick)
        {
            return false;
        }
        HerbPatchRegion region = HerbPatchRegion.forLocation(location);
        if (region == null || region != candidate || tick != lastTick + 1)
        {
            candidate = region;
            stableTicks = 0;
        }
        lastTick = tick;
        if (region == null || ++stableTicks < 3)
        {
            return false;
        }
        return store.observe(region.getPatch(), readVarbit.applyAsInt(region.getVarbit()), now);
    }

    public void reset()
    {
        resetCandidate();
        store.clear();
    }

    public void invalidateRegion()
    {
        resetCandidate();
    }

    private void resetCandidate()
    {
        candidate = null;
        stableTicks = 0;
        lastTick = -1;
    }
}
