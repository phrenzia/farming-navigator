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
package com.farmingnavigator.farming.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/** Plane-zero transmit coverage from pinned RuneLite FarmingWorld. Patch access is a separate check. */
public enum FieldPatchRegion
{
    ARDOUGNE(new int[]{10548}, FieldPatchDefinition.ARDOUGNE_NORTH, FieldPatchDefinition.ARDOUGNE_SOUTH, FieldPatchDefinition.ARDOUGNE_FLOWER),
    CATHERBY(new int[]{11062, 11061, 11318, 11317}, FieldPatchDefinition.CATHERBY_NORTH, FieldPatchDefinition.CATHERBY_SOUTH, FieldPatchDefinition.CATHERBY_FLOWER),
    ORTUS_FARM(new int[]{6192, 6447, 6448, 6449, 6191, 6193}, FieldPatchDefinition.ORTUS_FARM_NORTH, FieldPatchDefinition.ORTUS_FARM_SOUTH, FieldPatchDefinition.ORTUS_FARM_FLOWER),
    FALADOR(new int[]{12083}, FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_SOUTH, FieldPatchDefinition.FALADOR_FLOWER),
    HOSIDIUS(new int[]{6967, 6711}, FieldPatchDefinition.HOSIDIUS_NORTH, FieldPatchDefinition.HOSIDIUS_SOUTH, FieldPatchDefinition.HOSIDIUS_FLOWER),
    MORYTANIA(new int[]{14391, 14390}, FieldPatchDefinition.MORYTANIA_NORTH, FieldPatchDefinition.MORYTANIA_SOUTH, FieldPatchDefinition.MORYTANIA_FLOWER),
    FARMING_GUILD(new int[]{4922, 5177, 5178, 5179, 4921, 4923, 4665, 4666, 4667}, FieldPatchDefinition.FARMING_GUILD_NORTH, FieldPatchDefinition.FARMING_GUILD_SOUTH, FieldPatchDefinition.FARMING_GUILD_FLOWER),
    PRIFDDINAS(new int[]{13151, 12895, 12894, 13150, 12994, 12993, 12737, 12738, 12126, 12127, 13250}, FieldPatchDefinition.PRIFDDINAS_NORTH, FieldPatchDefinition.PRIFDDINAS_SOUTH, FieldPatchDefinition.PRIFDDINAS_FLOWER),
    HARMONY_ISLAND(new int[]{15148}, FieldPatchDefinition.HARMONY_ISLAND_ALLOTMENT),
    KASTORI(new int[]{5423, 5167, 5424}, FieldPatchDefinition.KASTORI_FLOWER),
    YANILLE(new int[]{10288}, FieldPatchDefinition.YANILLE_HOPS),
    SEERS_VILLAGE(new int[]{10551, 10550}, FieldPatchDefinition.SEERS_VILLAGE_HOPS),
    LUMBRIDGE(new int[]{12851}, FieldPatchDefinition.LUMBRIDGE_HOPS),
    ENTRANA(new int[]{11060, 11316}, FieldPatchDefinition.ENTRANA_HOPS),
    ALDARIN(new int[]{5421, 5165, 5166, 5422, 5677, 5678}, FieldPatchDefinition.ALDARIN_HOPS),
    SEAWEED(new int[]{15008}, FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH, FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_SOUTH),
    MORYTANIA_MUSHROOM(new int[]{13622, 13878}, FieldPatchDefinition.MORYTANIA_MUSHROOM),
    DRAYNOR_MANOR(new int[]{12340}, FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA),
    AUBURNVALE(new int[]{5427, 5428, 5684}, FieldPatchDefinition.AUBURNVALE_BELLADONNA);
    private final int[] regions;
    private final List<FieldPatchDefinition> patches;
    FieldPatchRegion(int[] regions, FieldPatchDefinition... patches)
    {
        this.regions = regions; this.patches = Collections.unmodifiableList(Arrays.asList(patches));
    }
    public List<FieldPatchDefinition> getPatches() { return patches; }
    private boolean isInBounds(WorldPoint location)
    {
        if (this == FALADOR) { return location.getY() >= 3272; }
        if (this == CATHERBY && location.getX() >= 2816 && location.getY() < 3456)
        {
            return location.getX() < 2840 && location.getY() >= 3440;
        }
        return true;
    }
    public static FieldPatchRegion forLocation(WorldPoint location)
    {
        if (location == null || location.getPlane() != 0) { return null; }
        for (FieldPatchRegion region : values())
        {
            for (int id : region.regions) { if (id == location.getRegionID() && region.isInBounds(location)) { return region; } }
        }
        return null;
    }
}
