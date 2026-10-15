package com.farmingnavigator.farming.garden;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.function.IntUnaryOperator;
import net.runelite.api.coords.WorldPoint;

/** Reads only the transmit channels belonging to the settled region, never scene objects. */
public final class GardenPatchObservationSampler
{
    private final GardenPatchObservationStore store;
    private GardenPatchRegion candidate;
    private int stableTicks;
    private int lastTick = -1;
    public GardenPatchObservationSampler(GardenPatchObservationStore store) { this.store = store; }

    public boolean sample(WorldPoint location, boolean loggedIn, boolean modal, boolean instance, int tick,
            IntUnaryOperator read, Instant now, FarmingGrowthRate rate)
    {
        if (!loggedIn) { reset(); return false; }
        if (modal || instance) { invalidateRegion(); return false; }
        if (tick == lastTick) { return false; }
        GardenPatchRegion region = GardenPatchRegion.forLocation(location);
        if (region == null || region != candidate || tick != lastTick + 1) { candidate = region; stableTicks = 0; }
        lastTick = tick;
        if (region == null || ++stableTicks < 3) { return false; }
        boolean changed = false;
        for (GardenPatchDefinition patch : region.getPatches())
        {
            changed |= store.observe(patch, read.applyAsInt(patch.getVarbit()), now, rate);
        }
        return changed;
    }
    public void reset() { invalidateRegion(); store.clear(); }
    public void invalidateRegion() { candidate = null; stableTicks = 0; lastTick = -1; }
}
