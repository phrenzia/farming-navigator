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
package com.farmingnavigator.farming.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/** Region lists and special bounds from the pinned RuneLite FarmingWorld. Plane zero only. */
public enum TreePatchRegion
{
    VARROCK(new int[]{12854, 12853}, TreePatchDefinition.VARROCK_TREE),
    TAVERLEY(new int[]{11573, 11829}, TreePatchDefinition.TAVERLEY_TREE),
    FALADOR(new int[]{11828, 12084}, TreePatchDefinition.FALADOR_TREE),
    LUMBRIDGE(new int[]{12594, 12850}, TreePatchDefinition.LUMBRIDGE_TREE),
    GNOME_STRONGHOLD(new int[]{9781, 9782, 9526, 9525}, TreePatchDefinition.GNOME_STRONGHOLD_TREE, TreePatchDefinition.GNOME_STRONGHOLD_FRUIT),
    FARMING_GUILD(new int[]{4922, 5177, 5178, 5179, 4921, 4923, 4665, 4666, 4667}, TreePatchDefinition.FARMING_GUILD_TREE, TreePatchDefinition.FARMING_GUILD_FRUIT,
            TreePatchDefinition.FARMING_GUILD_CELASTRUS, TreePatchDefinition.FARMING_GUILD_REDWOOD),
    AUBURNVALE(new int[]{5427, 5428, 5684}, TreePatchDefinition.AUBURNVALE_TREE),
    BRIMHAVEN(new int[]{11058, 11057}, TreePatchDefinition.BRIMHAVEN_FRUIT),
    CATHERBY(new int[]{11317}, TreePatchDefinition.CATHERBY_FRUIT),
    TREE_GNOME_VILLAGE(new int[]{9777, 10033}, TreePatchDefinition.TREE_GNOME_VILLAGE_FRUIT),
    LLETYA(new int[]{9265, 11103}, TreePatchDefinition.LLETYA_FRUIT),
    KASTORI(new int[]{5423, 5167, 5424}, TreePatchDefinition.KASTORI_FRUIT, TreePatchDefinition.KASTORI_CALQUAT),
    FOSSIL_ISLAND(new int[]{14651, 14907, 14908, 15164, 14652, 14906, 14650, 15162, 15163},
            TreePatchDefinition.FOSSIL_ISLAND_EAST, TreePatchDefinition.FOSSIL_ISLAND_MIDDLE, TreePatchDefinition.FOSSIL_ISLAND_WEST),
    AVIUM_SAVANNAH(new int[]{6702, 6446}, TreePatchDefinition.AVIUM_SAVANNAH_HARDWOOD),
    ANGLERS_RETREAT(new int[]{9770}, TreePatchDefinition.ANGLERS_RETREAT_HARDWOOD),
    TAI_BWO_WANNAI(new int[]{11056}, TreePatchDefinition.TAI_BWO_WANNAI_CALQUAT),
    GREAT_CONCH(new int[]{12581, 12325, 12326, 12327, 12580, 12582, 12583, 12836, 12837, 12838, 12839, 13092, 13093, 13194},
            TreePatchDefinition.GREAT_CONCH_CALQUAT),
    PRIFDDINAS(new int[]{13151, 12895, 12894, 13150, 12994, 12993, 12737, 12738, 12126, 12127, 13250},
            TreePatchDefinition.PRIFDDINAS_CRYSTAL);

    private final int[] regions;
    private final List<TreePatchDefinition> patches;
    TreePatchRegion(int[] regions, TreePatchDefinition... patches)
    {
        this.regions = regions;
        this.patches = Collections.unmodifiableList(Arrays.asList(patches));
    }
    public List<TreePatchDefinition> getPatches() { return patches; }

    public static TreePatchRegion forLocation(WorldPoint location)
    {
        if (location == null || location.getPlane() != 0) { return null; }
        for (TreePatchRegion region : values())
        {
            for (int id : region.regions)
            {
                if (id == location.getRegionID() && region.inBounds(location)) { return region; }
            }
        }
        return null;
    }

    private boolean inBounds(WorldPoint location)
    {
        if (this == CATHERBY) { return location.getX() >= 2840 || location.getY() < 3440; }
        if (this == FOSSIL_ISLAND)
        {
            int x = location.getX();
            int y = location.getY();
            if (x == 3753 && y >= 3868 && y <= 3870) { return false; }
            return !((x == 3729 || x == 3728 || x == 3747 || x == 3746) && y >= 3830 && y <= 3832);
        }
        return true;
    }
}
