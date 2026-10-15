package com.farmingnavigator.farming.tree;

import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.function.IntUnaryOperator;
import net.runelite.api.coords.WorldPoint;

/** Reads only the transmit channels belonging to the settled region, never scene objects. */
public final class TreePatchObservationSampler
{
    private final TreePatchObservationStore store;
    private TreePatchRegion candidate;
    private int stableTicks;
    private int lastTick = -1;
    public TreePatchObservationSampler(TreePatchObservationStore store) { this.store = store; }

    public boolean sample(WorldPoint location, boolean loggedIn, boolean modal, boolean instance, int tick,
            IntUnaryOperator read, Instant now, FarmingGrowthRate rate)
    {
        if (!loggedIn) { reset(); return false; }
        if (modal || instance) { invalidateRegion(); return false; }
        if (tick == lastTick) { return false; }
        TreePatchRegion region = TreePatchRegion.forLocation(location);
        if (region == null || region != candidate || tick != lastTick + 1) { candidate = region; stableTicks = 0; }
        lastTick = tick;
        if (region == null || ++stableTicks < 3) { return false; }
        boolean changed = false;
        for (TreePatchDefinition patch : region.getPatches())
        {
            changed |= store.observe(patch, read.applyAsInt(patch.getVarbit()), now, rate);
        }
        return changed;
    }
    public void reset() { invalidateRegion(); store.clear(); }
    public void invalidateRegion() { candidate = null; stableTicks = 0; lastTick = -1; }
}
