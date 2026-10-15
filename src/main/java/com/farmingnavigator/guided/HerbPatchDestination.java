/*
 * Copyright (c) 2018, Torkel Velure <https://github.com/TorkelV>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.farmingnavigator.guided;

import com.farmingnavigator.farming.HerbPatchDefinition;
import net.runelite.api.coords.WorldPoint;

/** Farming-area anchors from RuneLite FarmingPatchLocation; not exact clickable object tiles. */
public enum HerbPatchDestination
{
    FALADOR(3052, 3309), CATHERBY(2810, 3462), ARDOUGNE(2663, 3375),
    MORYTANIA(3598, 3524), HOSIDIUS(1729, 3558), TROLL_STRONGHOLD(2828, 3696),
    WEISS(2847, 3933), FARMING_GUILD(1235, 3724), HARMONY_ISLAND(3789, 2840), ORTUS_FARM(1587, 3101);

    private final WorldPoint location;

    HerbPatchDestination(int x, int y)
    {
        location = new WorldPoint(x, y, 0);
    }

    public WorldPoint getLocation() { return location; }

    public static boolean isNear(HerbPatchDefinition patch, WorldPoint player)
    {
        WorldPoint destination = valueOf(patch.name()).location;
        return player != null && player.getPlane() == destination.getPlane() && destination.distanceTo2D(player) <= 12;
    }
}
