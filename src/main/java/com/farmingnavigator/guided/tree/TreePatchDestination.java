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
package com.farmingnavigator.guided.tree;

import com.farmingnavigator.farming.tree.TreePatchDefinition;
import net.runelite.api.coords.WorldPoint;

/** Farming-area anchors from RuneLite FarmingPatchLocation; not clickable object tiles. */
public enum TreePatchDestination
{
    VARROCK_TREE(3226, 3457), TAVERLEY_TREE(2933, 3436), FALADOR_TREE(3005, 3375),
    LUMBRIDGE_TREE(3189, 3233), GNOME_STRONGHOLD_TREE(2434, 3418), FARMING_GUILD_TREE(1234, 3736),
    AUBURNVALE_TREE(1366, 3318), BRIMHAVEN_FRUIT(2765, 3211), CATHERBY_FRUIT(2858, 3432),
    GNOME_STRONGHOLD_FRUIT(2472, 3445), TREE_GNOME_VILLAGE_FRUIT(2487, 3181), LLETYA_FRUIT(2343, 3160),
    FARMING_GUILD_FRUIT(1242, 3755), KASTORI_FRUIT(1347, 3058),
    FOSSIL_ISLAND_EAST(3707, 3838), FOSSIL_ISLAND_MIDDLE(3707, 3838), FOSSIL_ISLAND_WEST(3707, 3838),
    AVIUM_SAVANNAH_HARDWOOD(1683, 2971), ANGLERS_RETREAT_HARDWOOD(2472, 2705),
    TAI_BWO_WANNAI_CALQUAT(2793, 3099), KASTORI_CALQUAT(1365, 3035), GREAT_CONCH_CALQUAT(3125, 2403),
    FARMING_GUILD_CELASTRUS(1242, 3755), FARMING_GUILD_REDWOOD(1233, 3754), PRIFDDINAS_CRYSTAL(3292, 6120);
    private final WorldPoint location;
    TreePatchDestination(int x, int y) { location = new WorldPoint(x, y, 0); }
    public WorldPoint getLocation() { return location; }
    public static boolean isNear(TreePatchDefinition patch, WorldPoint player)
    {
        WorldPoint anchor = valueOf(patch.name()).location;
        return player != null && player.getPlane() == 0 && anchor.distanceTo2D(player) <= 12;
    }
}
