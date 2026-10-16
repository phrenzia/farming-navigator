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
package com.farmingnavigator.farming.field;

/** Range tables adapted from RuneLite PatchImplementation, including alternate growth bytes. */
public final class FieldPatchStateDecoder
{
    // crop, growing, alternate growing, harvest, disease, disease count, dead, dead count
    private static final Row[] ALLOTMENTS = {
        new Row(FieldCrop.POTATO, 6, 70, 10, 135, 3, 199, 3),
        new Row(FieldCrop.ONION, 13, 77, 17, 142, 3, 206, 3),
        new Row(FieldCrop.CABBAGE, 20, 84, 24, 149, 3, 213, 3),
        new Row(FieldCrop.TOMATO, 27, 91, 31, 156, 3, 220, 3),
        new Row(FieldCrop.SWEETCORN, 34, 98, 40, 163, 5, 227, 5),
        new Row(FieldCrop.STRAWBERRY, 43, 107, 49, 172, 5, 236, 5),
        new Row(FieldCrop.WATERMELON, 52, 116, 60, 181, 7, 245, 7),
        new Row(FieldCrop.SNAPE_GRASS, 63, 128, 138, 196, 3, 193, 3)};
    private static final Row[] FLOWERS = {
        new Row(FieldCrop.MARIGOLD, 8, 72, 12, 137, 3, 201, 4),
        new Row(FieldCrop.ROSEMARY, 13, 77, 17, 142, 3, 206, 4),
        new Row(FieldCrop.NASTURTIUM, 18, 82, 22, 147, 3, 211, 4),
        new Row(FieldCrop.WOAD, 23, 87, 27, 152, 3, 216, 4),
        new Row(FieldCrop.LIMPWURT, 28, 92, 32, 157, 3, 221, 4),
        new Row(FieldCrop.WHITE_LILY, 37, 101, 41, 166, 3, 230, 4)};

    private static final Row[] HOPS = {
        new Row(FieldCrop.HAMMERSTONE, 4, 132, 8, 11, 3, 139, 3),
        new Row(FieldCrop.ASGARNIAN, 14, 142, 19, 22, 4, 150, 4),
        new Row(FieldCrop.YANILLIAN, 26, 154, 32, 35, 5, 163, 5),
        new Row(FieldCrop.KRANDORIAN, 40, 168, 47, 50, 6, 178, 6),
        new Row(FieldCrop.WILDBLOOD, 56, 184, 64, 67, 7, 195, 7),
        new Row(FieldCrop.BARLEY, 74, 202, 78, 81, 3, 209, 3),
        new Row(FieldCrop.JUTE, 84, 212, 89, 92, 4, 220, 4),
        new Row(FieldCrop.FLAX, 96, 224, 99, 102, 2, 230, 2),
        new Row(FieldCrop.HEMP, 104, 232, 108, 111, 3, 239, 3),
        new Row(FieldCrop.COTTON, 114, 242, 119, 122, 4, 250, 4)};

    public FieldPatchState decode(FieldFamily family, int raw)
    {
        if (family == null || raw < 0 || raw > 255) { return state(raw, FieldCrop.UNKNOWN, FieldPatchCondition.UNKNOWN, -1); }
        if (raw <= 3) { return state(raw, FieldCrop.UNKNOWN, raw == 0 ? FieldPatchCondition.EMPTY : FieldPatchCondition.WEEDS, 3 - raw); }
        if (family == FieldFamily.FLOWER && raw >= 33 && raw <= 36)
        {
            return state(raw, FieldCrop.UNKNOWN, FieldPatchCondition.SCARECROW, raw == 36 ? 0 : 35 - raw);
        }
        if (family == FieldFamily.ALLOTMENT)
        {
            if (in(raw, 202, 3)) { return state(raw, FieldCrop.SNAPE_GRASS, FieldPatchCondition.DISEASED, raw - 198); }
            if (in(raw, 209, 3)) { return state(raw, FieldCrop.SNAPE_GRASS, FieldPatchCondition.DEAD, raw - 205); }
        }
        for (Row row : family == FieldFamily.ALLOTMENT ? ALLOTMENTS : family == FieldFamily.FLOWER ? FLOWERS : HOPS)
        {
            if (in(raw, row.growing, row.crop.getStages() - 1)) { return state(raw, row.crop, FieldPatchCondition.GROWING, raw - row.growing); }
            if (in(raw, row.alternate, row.crop.getStages() - 1)) { return state(raw, row.crop, FieldPatchCondition.GROWING, raw - row.alternate); }
            if (in(raw, row.harvest, family == FieldFamily.FLOWER ? 1 : 3)) { return state(raw, row.crop, FieldPatchCondition.HARVESTABLE, family == FieldFamily.HOPS ? row.harvest + 2 - raw : raw - row.harvest); }
            if (in(raw, row.disease, row.diseaseCount)) { return state(raw, row.crop, FieldPatchCondition.DISEASED, raw - row.disease + 1); }
            if (in(raw, row.dead, row.deadCount)) { return state(raw, row.crop, FieldPatchCondition.DEAD, raw - row.dead + 1); }
        }
        return state(raw, FieldCrop.UNKNOWN, FieldPatchCondition.EMPTY, 3);
    }
    private static boolean in(int raw, int start, int count) { return raw >= start && raw < start + count; }
    private static FieldPatchState state(int raw, FieldCrop crop, FieldPatchCondition condition, int stage)
    {
        return new FieldPatchState(raw, crop, condition, stage);
    }
    private static final class Row
    {
        private final FieldCrop crop;
        private final int growing, alternate, harvest, disease, diseaseCount, dead, deadCount;
        private Row(FieldCrop crop, int growing, int alternate, int harvest, int disease, int diseaseCount, int dead, int deadCount)
        {
            this.crop = crop; this.growing = growing; this.alternate = alternate; this.harvest = harvest;
            this.disease = disease; this.diseaseCount = diseaseCount; this.dead = dead; this.deadCount = deadCount;
        }
    }
}
