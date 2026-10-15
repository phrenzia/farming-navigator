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

/** Byte ranges adapted from pinned RuneLite PatchImplementation, including legacy disease states. */
public final class GardenPatchStateDecoder
{
    private static final Row[] BUSH = {
        new Row(GardenCrop.REDBERRIES, 5, 10, 5, 70, 5, 134, 5, 250),
        new Row(GardenCrop.CADAVABERRIES, 15, 21, 5, 80, 6, 144, 6, 251),
        new Row(GardenCrop.DWELLBERRIES, 26, 33, 5, 91, 7, 155, 7, 252),
        new Row(GardenCrop.JANGERBERRIES, 38, 46, 5, 103, 8, 167, 8, 253),
        new Row(GardenCrop.WHITEBERRIES, 51, 59, 5, 116, 8, 180, 8, 254),
        new Row(GardenCrop.POISON_IVY, 197, 205, 5, 210, 7, 217, 8, 255)};
    private static final Row[] CACTUS = {
        new Row(GardenCrop.CACTUS, 8, 15, 4, 19, 6, 25, 6, 31),
        new Row(GardenCrop.POTATO_CACTUS, 32, 39, 7, 46, 6, 52, 6, 58)};

    public GardenPatchState decode(GardenFamily family, int raw)
    {
        if (family == null || raw < 0 || raw > 255) { return state(raw, GardenCrop.UNKNOWN, GardenPatchCondition.UNKNOWN, -1); }
        if (raw <= 3) { return state(raw, GardenCrop.UNKNOWN, raw == 0 ? GardenPatchCondition.EMPTY : GardenPatchCondition.WEEDS, 3 - raw); }
        if (family == GardenFamily.BUSH && raw == 225) { return state(raw, GardenCrop.POISON_IVY, GardenPatchCondition.DISEASED, 8); }
        Row[] rows = family == GardenFamily.BUSH ? BUSH : CACTUS;
        for (Row row : rows)
        {
            if (raw == row.healthCheck) { return state(raw, row.crop, GardenPatchCondition.CHECK_HEALTH, row.crop.getStages() - 1); }
            if (in(raw, row.growing, row.crop.getStages() - 1)) { return state(raw, row.crop, GardenPatchCondition.GROWING, raw - row.growing); }
            if (in(raw, row.harvest, row.harvestCount)) { return state(raw, row.crop, GardenPatchCondition.HEALTH_CHECKED, raw - row.harvest); }
            if (in(raw, row.disease, row.diseaseCount)) { return state(raw, row.crop, GardenPatchCondition.DISEASED, raw - row.disease + 1); }
            if (in(raw, row.dead, row.deadCount)) { return state(raw, row.crop, GardenPatchCondition.DEAD, raw - row.dead + 1); }
        }
        return state(raw, GardenCrop.UNKNOWN, GardenPatchCondition.EMPTY, 3);
    }
    private static boolean in(int raw, int start, int count) { return raw >= start && raw < start + count; }
    private static GardenPatchState state(int raw, GardenCrop crop, GardenPatchCondition condition, int stage)
    {
        return new GardenPatchState(raw, crop, condition, stage);
    }
    private static final class Row
    {
        private final GardenCrop crop;
        private final int growing, harvest, harvestCount, disease, diseaseCount, dead, deadCount, healthCheck;
        private Row(GardenCrop crop, int growing, int harvest, int harvestCount, int disease, int diseaseCount, int dead, int deadCount, int healthCheck)
        {
            this.crop = crop; this.growing = growing; this.harvest = harvest; this.harvestCount = harvestCount;
            this.disease = disease; this.diseaseCount = diseaseCount; this.dead = dead; this.deadCount = deadCount; this.healthCheck = healthCheck;
        }
    }
}
