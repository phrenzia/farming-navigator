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
package com.farmingnavigator.farming.tree;

/** Growth metadata from RuneLite Produce; stages include the final check-health state. */
public enum TreeCrop
{
    UNKNOWN("Unknown", null, 0, 0),
    OAK("Oak", TreeFamily.TREE, 40, 5),
    WILLOW("Willow", TreeFamily.TREE, 40, 7),
    MAPLE("Maple", TreeFamily.TREE, 40, 9),
    YEW("Yew", TreeFamily.TREE, 40, 11),
    MAGIC("Magic", TreeFamily.TREE, 40, 13),
    APPLE("Apple", TreeFamily.FRUIT_TREE, 160, 7),
    BANANA("Banana", TreeFamily.FRUIT_TREE, 160, 7),
    ORANGE("Orange", TreeFamily.FRUIT_TREE, 160, 7),
    CURRY("Curry", TreeFamily.FRUIT_TREE, 160, 7),
    PINEAPPLE("Pineapple", TreeFamily.FRUIT_TREE, 160, 7),
    PAPAYA("Papaya", TreeFamily.FRUIT_TREE, 160, 7),
    PALM("Palm", TreeFamily.FRUIT_TREE, 160, 7),
    DRAGONFRUIT("Dragonfruit", TreeFamily.FRUIT_TREE, 160, 7),
    TEAK("Teak", TreeFamily.HARDWOOD_TREE, 640, 8),
    MAHOGANY("Mahogany", TreeFamily.HARDWOOD_TREE, 640, 9),
    CAMPHOR("Camphor", TreeFamily.HARDWOOD_TREE, 640, 9),
    IRONWOOD("Ironwood", TreeFamily.HARDWOOD_TREE, 640, 9),
    ROSEWOOD("Rosewood", TreeFamily.HARDWOOD_TREE, 640, 10),
    CALQUAT("Calquat", TreeFamily.CALQUAT, 160, 9),
    CELASTRUS("Celastrus", TreeFamily.CELASTRUS, 160, 6),
    REDWOOD("Redwood", TreeFamily.REDWOOD, 640, 11),
    CRYSTAL_TREE("Crystal tree", TreeFamily.CRYSTAL_TREE, 80, 7),
    SPIRIT_TREE("Spirit tree", TreeFamily.SPIRIT_TREE, 320, 13);

    private final String label;
    private final TreeFamily family;
    private final int growthCycleMinutes;
    private final int stages;

    TreeCrop(String label, TreeFamily family, int growthCycleMinutes, int stages)
    {
        this.label = label;
        this.family = family;
        this.growthCycleMinutes = growthCycleMinutes;
        this.stages = stages;
    }

    public String getLabel() { return label; }
    public TreeFamily getFamily() { return family; }
    public int getGrowthCycleMinutes() { return growthCycleMinutes; }
    public int getStages() { return stages; }
}
