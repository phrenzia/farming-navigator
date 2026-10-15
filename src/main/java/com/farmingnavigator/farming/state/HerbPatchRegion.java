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
package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/**
 * Herb transmit assignments and bounds adapted from RuneLite FarmingWorld.
 * https://github.com/runelite/runelite/blob/e98399f931c60bfa8e42f924da850f508ec8e00d/runelite-client/src/main/java/net/runelite/client/plugins/timetracking/farming/FarmingWorld.java
 * Regions are map-square coordinates, rather than game object IDs.
 */
public enum HerbPatchRegion
{
    FALADOR(HerbPatchDefinition.FALADOR, VarbitID.FARMING_TRANSMIT_D, 12083),
    CATHERBY(HerbPatchDefinition.CATHERBY, VarbitID.FARMING_TRANSMIT_D, 11062, 11061, 11318, 11317),
    ARDOUGNE(HerbPatchDefinition.ARDOUGNE, VarbitID.FARMING_TRANSMIT_D, 10548),
    MORYTANIA(HerbPatchDefinition.MORYTANIA, VarbitID.FARMING_TRANSMIT_D, 14391, 14390),
    HOSIDIUS(HerbPatchDefinition.HOSIDIUS, VarbitID.FARMING_TRANSMIT_D, 6967, 6711),
    TROLL_STRONGHOLD(HerbPatchDefinition.TROLL_STRONGHOLD, VarbitID.FARMING_TRANSMIT_A, 11321),
    WEISS(HerbPatchDefinition.WEISS, VarbitID.FARMING_TRANSMIT_A, 11325),
    FARMING_GUILD(HerbPatchDefinition.FARMING_GUILD, VarbitID.FARMING_TRANSMIT_E,
            4922, 5177, 5178, 5179, 4921, 4923, 4665, 4666, 4667),
    HARMONY_ISLAND(HerbPatchDefinition.HARMONY_ISLAND, VarbitID.FARMING_TRANSMIT_B, 15148),
    ORTUS_FARM(HerbPatchDefinition.ORTUS_FARM, VarbitID.FARMING_TRANSMIT_D,
            6192, 6447, 6448, 6449, 6191, 6193);

    private final HerbPatchDefinition patch;
    private final int varbit;
    private final int[] regionIds;

    HerbPatchRegion(HerbPatchDefinition patch, int varbit, int... regionIds)
    {
        this.patch = patch;
        this.varbit = varbit;
        this.regionIds = regionIds;
    }

    public HerbPatchDefinition getPatch()
    {
        return patch;
    }

    public int getVarbit()
    {
        return varbit;
    }

    public static HerbPatchRegion forLocation(WorldPoint location)
    {
        // Upper floors and instances are deliberately not inferred from ground-floor data.
        if (location == null || location.getPlane() != 0)
        {
            return null;
        }
        for (HerbPatchRegion region : values())
        {
            for (int id : region.regionIds)
            {
                if (location.getRegionID() == id && region.isInBounds(location))
                {
                    return region;
                }
            }
        }
        return null;
    }

    private boolean isInBounds(WorldPoint location)
    {
        if (this == FALADOR)
        {
            return location.getY() >= 3272;
        }
        if (this == CATHERBY && location.getX() >= 2816 && location.getY() < 3456)
        {
            return location.getX() < 2840 && location.getY() >= 3440;
        }
        return true;
    }
}
