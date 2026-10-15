/*
 * Copyright (c) 2018, Torkel Velure <https://github.com/TorkelV>
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

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/** Transmit assignments from FarmingWorld and farming-area anchors from FarmingPatchLocation. */
public enum GardenPatchDefinition
{
    RIMMINGTON_BUSH("Rimmington", GardenFamily.BUSH, VarbitID.FARMING_TRANSMIT_A, 2938, 3223),
    ETCETERIA_BUSH("Etceteria", GardenFamily.BUSH, VarbitID.FARMING_TRANSMIT_A, 2589, 3862),
    CHAMPIONS_GUILD_BUSH("Champions' Guild", GardenFamily.BUSH, VarbitID.FARMING_TRANSMIT_A, 3182, 3356),
    ARDOUGNE_BUSH("Ardougne", GardenFamily.BUSH, VarbitID.FARMING_TRANSMIT_A, 2615, 3224),
    FARMING_GUILD_BUSH("Farming Guild", GardenFamily.BUSH, VarbitID.FARMING_TRANSMIT_B, 1259, 3729),
    AL_KHARID_CACTUS("Al Kharid", GardenFamily.CACTUS, VarbitID.FARMING_TRANSMIT_A, 3313, 3201),
    FARMING_GUILD_CACTUS("Farming Guild", GardenFamily.CACTUS, VarbitID.FARMING_TRANSMIT_F, 1264, 3745);
    private final String label;
    private final GardenFamily family;
    private final int varbit;
    private final WorldPoint location;
    GardenPatchDefinition(String label, GardenFamily family, int varbit, int x, int y)
    {
        this.label = label; this.family = family; this.varbit = varbit; this.location = new WorldPoint(x, y, 0);
    }
    public String getLabel() { return label; }
    public GardenFamily getFamily() { return family; }
    public int getVarbit() { return varbit; }
    public WorldPoint getLocation() { return location; }
    public boolean isNear(WorldPoint player) { return player != null && player.getPlane() == 0 && location.distanceTo2D(player) <= 12; }
}
