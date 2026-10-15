/*
 * Copyright (c) 2018, NotFoxtrot <https://github.com/NotFoxtrot>
 * Copyright (c) 2018 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.farmingnavigator.farming.garden;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/** Plane-zero transmit coverage from pinned RuneLite FarmingWorld. Patch access is a separate check. */
public enum GardenPatchRegion
{
    RIMMINGTON(new int[]{11570, 11826}, GardenPatchDefinition.RIMMINGTON_BUSH),
    ETCETERIA(new int[]{10300}, GardenPatchDefinition.ETCETERIA_BUSH),
    CHAMPIONS_GUILD(new int[]{12596}, GardenPatchDefinition.CHAMPIONS_GUILD_BUSH),
    ARDOUGNE(new int[]{10290, 10546}, GardenPatchDefinition.ARDOUGNE_BUSH),
    AL_KHARID(new int[]{13106, 13362, 13105}, GardenPatchDefinition.AL_KHARID_CACTUS),
    FARMING_GUILD(new int[]{4922, 5177, 5178, 5179, 4921, 4923, 4665, 4666, 4667},
            GardenPatchDefinition.FARMING_GUILD_BUSH, GardenPatchDefinition.FARMING_GUILD_CACTUS);
    private final int[] regions;
    private final List<GardenPatchDefinition> patches;
    GardenPatchRegion(int[] regions, GardenPatchDefinition... patches)
    {
        this.regions = regions; this.patches = Collections.unmodifiableList(Arrays.asList(patches));
    }
    public List<GardenPatchDefinition> getPatches() { return patches; }
    public static GardenPatchRegion forLocation(WorldPoint location)
    {
        if (location == null || location.getPlane() != 0) { return null; }
        for (GardenPatchRegion region : values())
        {
            for (int id : region.regions) { if (id == location.getRegionID()) { return region; } }
        }
        return null;
    }
}
