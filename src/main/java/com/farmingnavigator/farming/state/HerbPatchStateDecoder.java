/*
 * Copyright (c) 2019 Abex
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

/**
 * Adapted from RuneLite PatchImplementation.HERB, retaining its exact known ranges.
 * https://github.com/runelite/runelite/blob/e98399f931c60bfa8e42f924da850f508ec8e00d/runelite-client/src/main/java/net/runelite/client/plugins/timetracking/farming/PatchImplementation.java
 *
 * This decoder does not establish whether a transmitted value belongs to a patch.
 * Callers must verify region and freshness before recording an observation.
 */
public final class HerbPatchStateDecoder
{
    private static final HerbCrop[] NORMAL_CROPS = {
            HerbCrop.GUAM, HerbCrop.MARRENTILL, HerbCrop.TARROMIN, HerbCrop.HARRALANDER,
            HerbCrop.RANARR, HerbCrop.TOADFLAX, HerbCrop.IRIT, HerbCrop.AVANTOE,
            HerbCrop.HUASCA, HerbCrop.KWUARM, HerbCrop.SNAPDRAGON, HerbCrop.CADANTINE,
            HerbCrop.LANTADYME, HerbCrop.DWARF_WEED, HerbCrop.TORSTOL
    };
    private static final int[] GROWING_BASES = {4, 11, 18, 25, 32, 39, 46, 53, 60, 68, 75, 82, 89, 96, 103};
    private static final int[] DISEASED_BASES = {128, 131, 134, 137, 140, 143, 146, 149, 173, 152, 155, 158, 161, 164, 167};

    public HerbPatchState decode(int value)
    {
        if (value >= 0 && value <= 3)
        {
            return state(value, value == 0 ? PatchCondition.EMPTY : PatchCondition.WEEDS,
                    HerbCrop.UNKNOWN, 3 - value);
        }
        if (value == 67 || (value >= 176 && value <= 191)
                || (value >= 204 && value <= 219) || (value >= 221 && value <= 255))
        {
            return state(value, PatchCondition.EMPTY, HerbCrop.UNKNOWN, 3);
        }
        for (int i = 0; i < NORMAL_CROPS.length; i++)
        {
            int offset = value - GROWING_BASES[i];
            if (offset >= 0 && offset <= 3)
            {
                return state(value, PatchCondition.GROWING, NORMAL_CROPS[i], offset);
            }
            if (offset >= 4 && offset <= 6)
            {
                return state(value, PatchCondition.HARVESTABLE, NORMAL_CROPS[i], 6 - offset);
            }
            int diseasedOffset = value - DISEASED_BASES[i];
            if (diseasedOffset >= 0 && diseasedOffset <= 2)
            {
                return state(value, PatchCondition.DISEASED, NORMAL_CROPS[i], diseasedOffset + 1);
            }
        }
        if (value >= 170 && value <= 172)
        {
            // These states expose a dead herb, but do not identify which herb.
            return state(value, PatchCondition.DEAD, HerbCrop.UNKNOWN, value - 169);
        }
        if (value >= 192 && value <= 195)
        {
            return state(value, PatchCondition.GROWING, HerbCrop.GOUTWEED, value - 192);
        }
        if (value >= 196 && value <= 197)
        {
            return state(value, PatchCondition.HARVESTABLE, HerbCrop.GOUTWEED, 197 - value);
        }
        if (value >= 198 && value <= 200)
        {
            return state(value, PatchCondition.DISEASED, HerbCrop.GOUTWEED, value - 197);
        }
        if (value >= 201 && value <= 203)
        {
            return state(value, PatchCondition.DEAD, HerbCrop.GOUTWEED, value - 200);
        }
        return state(value, PatchCondition.UNKNOWN, HerbCrop.UNKNOWN, -1);
    }

    private HerbPatchState state(int raw, PatchCondition condition, HerbCrop crop, int stage)
    {
        return new HerbPatchState(raw, condition, crop, stage);
    }
}
