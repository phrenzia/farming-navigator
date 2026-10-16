/*
 * Copyright (c) 2018 Abex
 * Copyright (c) 2018, NotFoxtrot <https://github.com/NotFoxtrot>
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

/** Pinned RuneLite Produce metadata; harvest stages are not produce quantities. */
public enum FieldCrop
{
    UNKNOWN("Unknown", null, 0, 0),
    POTATO("Potato", FieldFamily.ALLOTMENT, 10, 5),
    ONION("Onion", FieldFamily.ALLOTMENT, 10, 5),
    CABBAGE("Cabbage", FieldFamily.ALLOTMENT, 10, 5),
    TOMATO("Tomato", FieldFamily.ALLOTMENT, 10, 5),
    SWEETCORN("Sweetcorn", FieldFamily.ALLOTMENT, 10, 7),
    STRAWBERRY("Strawberry", FieldFamily.ALLOTMENT, 10, 7),
    WATERMELON("Watermelon", FieldFamily.ALLOTMENT, 10, 9),
    SNAPE_GRASS("Snape grass", FieldFamily.ALLOTMENT, 10, 8),
    MARIGOLD("Marigold", FieldFamily.FLOWER, 5, 5),
    ROSEMARY("Rosemary", FieldFamily.FLOWER, 5, 5),
    NASTURTIUM("Nasturtium", FieldFamily.FLOWER, 5, 5),
    WOAD("Woad", FieldFamily.FLOWER, 5, 5),
    LIMPWURT("Limpwurt", FieldFamily.FLOWER, 5, 5),
    WHITE_LILY("White lily", FieldFamily.FLOWER, 5, 5),
    BARLEY("Barley", FieldFamily.HOPS, 10, 5),
    HAMMERSTONE("Hammerstone", FieldFamily.HOPS, 10, 5),
    ASGARNIAN("Asgarnian", FieldFamily.HOPS, 10, 6),
    JUTE("Jute", FieldFamily.HOPS, 10, 6),
    YANILLIAN("Yanillian", FieldFamily.HOPS, 10, 7),
    KRANDORIAN("Krandorian", FieldFamily.HOPS, 10, 8),
    WILDBLOOD("Wildblood", FieldFamily.HOPS, 10, 9),
    FLAX("Flax", FieldFamily.HOPS, 20, 4),
    HEMP("Hemp", FieldFamily.HOPS, 20, 5),
    COTTON("Cotton", FieldFamily.HOPS, 20, 6);
    private final String label;
    private final FieldFamily family;
    private final int growthCycleMinutes;
    private final int stages;
    FieldCrop(String label, FieldFamily family, int cycle, int stages)
    {
        this.label = label; this.family = family; this.growthCycleMinutes = cycle; this.stages = stages;
    }
    public String getLabel() { return label; }
    public FieldFamily getFamily() { return family; }
    public int getGrowthCycleMinutes() { return growthCycleMinutes; }
    public int getStages() { return stages; }
}
