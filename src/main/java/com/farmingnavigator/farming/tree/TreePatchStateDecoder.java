/*
 * Copyright (c) 2019 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.farmingnavigator.farming.tree;

/**
 * Adapted from RuneLite PatchImplementation TREE, FRUIT_TREE and HARDWOOD_TREE.
 * https://github.com/runelite/runelite/blob/e98399f931c60bfa8e42f924da850f508ec8e00d/runelite-client/src/main/java/net/runelite/client/plugins/timetracking/farming/PatchImplementation.java
 * Region validation remains the caller's responsibility. No timers or protection are inferred here.
 */
public final class TreePatchStateDecoder
{
    private static final TreeCrop[] REGULAR = {TreeCrop.OAK, TreeCrop.WILLOW, TreeCrop.MAPLE, TreeCrop.YEW, TreeCrop.MAGIC};
    private static final int[] REGULAR_GROW = {8, 15, 24, 35, 48};
    private static final int[] REGULAR_DISEASE = {73, 80, 89, 100, 113};
    private static final int[] REGULAR_DEAD = {137, 144, 153, 164, 177};
    private static final TreeCrop[] FRUIT = {TreeCrop.APPLE, TreeCrop.BANANA, TreeCrop.ORANGE, TreeCrop.CURRY,
            TreeCrop.PINEAPPLE, TreeCrop.PAPAYA, TreeCrop.PALM, TreeCrop.DRAGONFRUIT};
    private static final int[] FRUIT_GROW = {8, 35, 72, 99, 136, 163, 200, 227};
    private static final TreeCrop[] HARDWOOD = {TreeCrop.TEAK, TreeCrop.MAHOGANY, TreeCrop.CAMPHOR, TreeCrop.IRONWOOD, TreeCrop.ROSEWOOD};
    private static final int[] HARDWOOD_GROW = {8, 30, 55, 80, 105};
    private static final int[] HARDWOOD_DISEASE = {18, 41, 66, 91, 117};
    private static final int[] HARDWOOD_DEAD = {24, 48, 73, 98, 125};

    public TreePatchState decode(TreeFamily family, int raw)
    {
        if (family == null || raw < 0 || raw > 255) { return state(raw, TreeCrop.UNKNOWN, TreePatchCondition.UNKNOWN, -1); }
        if (raw <= 3) { return state(raw, TreeCrop.UNKNOWN, raw == 0 ? TreePatchCondition.EMPTY : TreePatchCondition.WEEDS, 3 - raw); }
        switch (family)
        {
            case TREE: return regular(raw);
            case FRUIT_TREE: return fruit(raw);
            case HARDWOOD_TREE: return hardwood(raw);
            case CALQUAT: return calquat(raw);
            case CELASTRUS: return celastrus(raw);
            case REDWOOD: return redwood(raw);
            case CRYSTAL_TREE: return crystal(raw);
            default: return state(raw, TreeCrop.UNKNOWN, TreePatchCondition.UNKNOWN, -1);
        }
    }

    private TreePatchState regular(int raw)
    {
        for (int i = 0; i < REGULAR.length; i++)
        {
            TreeCrop crop = REGULAR[i];
            int growing = crop.getStages() - 1;
            int offset = raw - REGULAR_GROW[i];
            if (offset >= 0 && offset < growing) { return state(raw, crop, TreePatchCondition.GROWING, offset); }
            if (offset == growing) { return state(raw, crop, TreePatchCondition.CHECK_HEALTH, growing); }
            if (offset == growing + 1) { return state(raw, crop, TreePatchCondition.HEALTH_CHECKED, 0); }
            if (offset == growing + 2) { return state(raw, crop, TreePatchCondition.STUMP, 0); }
            TreePatchState illness = regularIllness(raw, crop, REGULAR_DISEASE[i], TreePatchCondition.DISEASED);
            if (illness != null) { return illness; }
            illness = regularIllness(raw, crop, REGULAR_DEAD[i], TreePatchCondition.DEAD);
            if (illness != null) { return illness; }
            // Unmapped raw states between the penultimate and final disease/death models are unknown.
            if (raw == REGULAR_DISEASE[i] + growing - 1 || raw == REGULAR_DEAD[i] + growing - 1)
            {
                return state(raw, TreeCrop.UNKNOWN, TreePatchCondition.UNKNOWN, -1);
            }
        }
        if (raw >= 192 && raw <= 197) { return state(raw, TreeCrop.WILLOW, TreePatchCondition.HEALTH_CHECKED, 0); }
        return empty(raw);
    }

    private TreePatchState regularIllness(int raw, TreeCrop crop, int start, TreePatchCondition condition)
    {
        int offset = raw - start;
        if (offset >= 0 && offset < crop.getStages() - 2) { return state(raw, crop, condition, offset + 1); }
        if (offset == crop.getStages() - 1) { return state(raw, crop, condition, crop.getStages() - 1); }
        return null;
    }

    private TreePatchState fruit(int raw)
    {
        for (int i = 0; i < FRUIT.length; i++)
        {
            int offset = raw - FRUIT_GROW[i];
            TreeCrop crop = FRUIT[i];
            if (offset >= 0 && offset <= 5) { return state(raw, crop, TreePatchCondition.GROWING, offset); }
            if (offset >= 6 && offset <= 12) { return state(raw, crop, TreePatchCondition.HEALTH_CHECKED, offset - 6); }
            if (offset >= 13 && offset <= 18) { return state(raw, crop, TreePatchCondition.DISEASED, offset - 12); }
            if (offset >= 19 && offset <= 24) { return state(raw, crop, TreePatchCondition.DEAD, offset - 18); }
            if (offset == 25) { return state(raw, crop, TreePatchCondition.STUMP, 0); }
            if (offset == 26) { return state(raw, crop, TreePatchCondition.CHECK_HEALTH, 6); }
        }
        return empty(raw);
    }

    private TreePatchState hardwood(int raw)
    {
        for (int i = 0; i < HARDWOOD.length; i++)
        {
            TreeCrop crop = HARDWOOD[i];
            int offset = raw - HARDWOOD_GROW[i];
            int growing = crop.getStages() - 1;
            if (offset >= 0 && offset < growing) { return state(raw, crop, TreePatchCondition.GROWING, offset); }
            if (offset == growing) { return state(raw, crop, TreePatchCondition.CHECK_HEALTH, growing); }
            if (offset == growing + 1) { return state(raw, crop, TreePatchCondition.HEALTH_CHECKED, 0); }
            if (offset == growing + 2) { return state(raw, crop, TreePatchCondition.STUMP, 0); }
            int disease = raw - HARDWOOD_DISEASE[i];
            if (disease >= 0 && disease < crop.getStages() - 2) { return state(raw, crop, TreePatchCondition.DISEASED, disease + 1); }
            int dead = raw - HARDWOOD_DEAD[i];
            if (dead >= 0 && dead < crop.getStages() - 2) { return state(raw, crop, TreePatchCondition.DEAD, dead + 1); }
        }
        return empty(raw);
    }

    private TreePatchState calquat(int raw)
    {
        if (raw <= 11) { return state(raw, TreeCrop.CALQUAT, TreePatchCondition.GROWING, raw - 4); }
        if (raw <= 18) { return state(raw, TreeCrop.CALQUAT, TreePatchCondition.HEALTH_CHECKED, raw - 12); }
        if (raw <= 25) { return state(raw, TreeCrop.CALQUAT, TreePatchCondition.DISEASED, raw - 18); }
        if (raw <= 33) { return state(raw, TreeCrop.CALQUAT, TreePatchCondition.DEAD, raw - 25); }
        if (raw == 34) { return state(raw, TreeCrop.CALQUAT, TreePatchCondition.CHECK_HEALTH, 8); }
        return empty(raw);
    }

    private TreePatchState celastrus(int raw)
    {
        if (raw <= 7 || raw >= 29) { return empty(raw); }
        if (raw <= 12) { return state(raw, TreeCrop.CELASTRUS, TreePatchCondition.GROWING, raw - 8); }
        if (raw == 13) { return state(raw, TreeCrop.CELASTRUS, TreePatchCondition.CHECK_HEALTH, 5); }
        if (raw <= 16) { return state(raw, TreeCrop.CELASTRUS, TreePatchCondition.HARVESTABLE, raw - 14); }
        if (raw == 17) { return state(raw, TreeCrop.CELASTRUS, TreePatchCondition.HARVESTED, 0); }
        if (raw <= 22) { return state(raw, TreeCrop.CELASTRUS, TreePatchCondition.DISEASED, raw - 17); }
        if (raw <= 27) { return state(raw, TreeCrop.CELASTRUS, TreePatchCondition.DEAD, raw - 22); }
        return state(raw, TreeCrop.CELASTRUS, TreePatchCondition.STUMP, 0);
    }

    private TreePatchState redwood(int raw)
    {
        if (raw <= 7) { return empty(raw); }
        if (raw <= 17) { return state(raw, TreeCrop.REDWOOD, TreePatchCondition.GROWING, raw - 8); }
        if (raw == 18 || raw >= 41 && raw <= 55) { return state(raw, TreeCrop.REDWOOD, TreePatchCondition.HEALTH_CHECKED, 0); }
        if (raw >= 19 && raw <= 27) { return state(raw, TreeCrop.REDWOOD, TreePatchCondition.DISEASED, raw - 18); }
        if (raw >= 28 && raw <= 36) { return state(raw, TreeCrop.REDWOOD, TreePatchCondition.DEAD, raw - 27); }
        if (raw == 37) { return state(raw, TreeCrop.REDWOOD, TreePatchCondition.CHECK_HEALTH, 10); }
        return unknown(raw);
    }

    private TreePatchState crystal(int raw)
    {
        if (raw >= 8 && raw <= 13) { return state(raw, TreeCrop.CRYSTAL_TREE, TreePatchCondition.GROWING, raw - 8); }
        if (raw == 14) { return state(raw, TreeCrop.CRYSTAL_TREE, TreePatchCondition.CHECK_HEALTH, 6); }
        if (raw == 15) { return state(raw, TreeCrop.CRYSTAL_TREE, TreePatchCondition.HEALTH_CHECKED, 0); }
        return unknown(raw);
    }

    private TreePatchState unknown(int raw) { return state(raw, TreeCrop.UNKNOWN, TreePatchCondition.UNKNOWN, -1); }
    private TreePatchState empty(int raw) { return state(raw, TreeCrop.UNKNOWN, TreePatchCondition.EMPTY, 3); }
    private TreePatchState state(int raw, TreeCrop crop, TreePatchCondition condition, int stage)
    {
        return new TreePatchState(raw, crop, condition, stage);
    }
}
