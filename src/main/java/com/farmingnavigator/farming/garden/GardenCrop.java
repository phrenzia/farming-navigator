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

/** Growth metadata from pinned RuneLite Produce. No prediction of berries, spines or potato-cactus quantities. */
public enum GardenCrop
{
    UNKNOWN("Unknown", null, 0, 0),
    REDBERRIES("Redberry", GardenFamily.BUSH, 20, 6),
    CADAVABERRIES("Cadavaberry", GardenFamily.BUSH, 20, 7),
    DWELLBERRIES("Dwellberry", GardenFamily.BUSH, 20, 8),
    JANGERBERRIES("Jangerberry", GardenFamily.BUSH, 20, 9),
    WHITEBERRIES("Whiteberry", GardenFamily.BUSH, 20, 9),
    POISON_IVY("Poison ivy", GardenFamily.BUSH, 20, 9),
    CACTUS("Cactus", GardenFamily.CACTUS, 80, 8),
    POTATO_CACTUS("Potato cactus", GardenFamily.CACTUS, 10, 8);
    private final String label;
    private final GardenFamily family;
    private final int growthCycleMinutes;
    private final int stages;
    GardenCrop(String label, GardenFamily family, int cycle, int stages)
    {
        this.label = label; this.family = family; this.growthCycleMinutes = cycle; this.stages = stages;
    }
    public String getLabel() { return label; }
    public GardenFamily getFamily() { return family; }
    public int getGrowthCycleMinutes() { return growthCycleMinutes; }
    public int getStages() { return stages; }
}
