/*
 * Copyright (c) 2018, Torkel Velure <https://github.com/TorkelV>
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

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/** Transmit assignments from FarmingWorld and shared farming-area anchors (not exact plot tiles) from FarmingPatchLocation. */
public enum FieldPatchDefinition
{
    ARDOUGNE_NORTH("Ardougne - North", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 2663, 3375),
    ARDOUGNE_SOUTH("Ardougne - South", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 2663, 3375),
    ARDOUGNE_FLOWER("Ardougne - Flower", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_C, 2663, 3375),
    CATHERBY_NORTH("Catherby - North", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 2810, 3462),
    CATHERBY_SOUTH("Catherby - South", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 2810, 3462),
    CATHERBY_FLOWER("Catherby - Flower", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_C, 2810, 3462),
    ORTUS_FARM_NORTH("Ortus Farm - North", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 1587, 3101),
    ORTUS_FARM_SOUTH("Ortus Farm - South", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 1587, 3101),
    ORTUS_FARM_FLOWER("Ortus Farm - Flower", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_C, 1587, 3101),
    FALADOR_NORTH("Falador - North west", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 3052, 3309),
    FALADOR_SOUTH("Falador - South east", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 3052, 3309),
    FALADOR_FLOWER("Falador - Flower", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_C, 3052, 3309),
    HOSIDIUS_NORTH("Hosidius - North east", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 1729, 3558),
    HOSIDIUS_SOUTH("Hosidius - South west", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 1729, 3558),
    HOSIDIUS_FLOWER("Hosidius - Flower", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_C, 1729, 3558),
    MORYTANIA_NORTH("Morytania - North west", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 3598, 3524),
    MORYTANIA_SOUTH("Morytania - South east", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 3598, 3524),
    MORYTANIA_FLOWER("Morytania - Flower", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_C, 3598, 3524),
    FARMING_GUILD_NORTH("Farming Guild - North", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_C, 1269, 3730),
    FARMING_GUILD_SOUTH("Farming Guild - South", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_D, 1269, 3730),
    FARMING_GUILD_FLOWER("Farming Guild - Flower", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_H, 1259, 3729),
    PRIFDDINAS_NORTH("Prifddinas - North", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 3289, 6100),
    PRIFDDINAS_SOUTH("Prifddinas - South", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_B, 3289, 6100),
    PRIFDDINAS_FLOWER("Prifddinas - Flower", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_C, 3289, 6100),
    HARMONY_ISLAND_ALLOTMENT("Harmony Island", FieldFamily.ALLOTMENT, VarbitID.FARMING_TRANSMIT_A, 3793, 2836),
    KASTORI_FLOWER("Kastori", FieldFamily.FLOWER, VarbitID.FARMING_TRANSMIT_C, 1352, 3025),
    YANILLE_HOPS("Yanille", FieldFamily.HOPS, VarbitID.FARMING_TRANSMIT_A, 2572, 3102),
    SEERS_VILLAGE_HOPS("Seers' Village", FieldFamily.HOPS, VarbitID.FARMING_TRANSMIT_A, 2661, 3523),
    LUMBRIDGE_HOPS("Lumbridge", FieldFamily.HOPS, VarbitID.FARMING_TRANSMIT_A, 3224, 3313),
    ENTRANA_HOPS("Entrana", FieldFamily.HOPS, VarbitID.FARMING_TRANSMIT_A, 2812, 3334),
    ALDARIN_HOPS("Aldarin", FieldFamily.HOPS, VarbitID.FARMING_TRANSMIT_A, 1367, 2938),
    FOSSIL_ISLAND_SEAWEED_NORTH("Fossil Island - North seaweed", FieldFamily.SEAWEED, VarbitID.FARMING_TRANSMIT_A, 3730, 10271),
    FOSSIL_ISLAND_SEAWEED_SOUTH("Fossil Island - South seaweed", FieldFamily.SEAWEED, VarbitID.FARMING_TRANSMIT_B, 3730, 10271),
    MORYTANIA_MUSHROOM("Morytania mushroom", FieldFamily.MUSHROOM, VarbitID.FARMING_TRANSMIT_A, 3449, 3471),
    DRAYNOR_MANOR_BELLADONNA("Draynor Manor belladonna", FieldFamily.BELLADONNA, VarbitID.FARMING_TRANSMIT_A, 3084, 3356),
    AUBURNVALE_BELLADONNA("Auburnvale belladonna", FieldFamily.BELLADONNA, VarbitID.FARMING_TRANSMIT_B, 1449, 3354),
    VINERY_EAST_1("Hosidius vinery - East 1", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_A1, 1807, 3555),
    VINERY_EAST_2("Hosidius vinery - East 2", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_A2, 1807, 3555),
    VINERY_EAST_3("Hosidius vinery - East 3", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_B1, 1807, 3555),
    VINERY_EAST_4("Hosidius vinery - East 4", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_B2, 1807, 3555),
    VINERY_EAST_5("Hosidius vinery - East 5", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_C1, 1807, 3555),
    VINERY_EAST_6("Hosidius vinery - East 6", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_C2, 1807, 3555),
    VINERY_WEST_1("Hosidius vinery - West 1", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_D1, 1807, 3555),
    VINERY_WEST_2("Hosidius vinery - West 2", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_D2, 1807, 3555),
    VINERY_WEST_3("Hosidius vinery - West 3", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_E1, 1807, 3555),
    VINERY_WEST_4("Hosidius vinery - West 4", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_E2, 1807, 3555),
    VINERY_WEST_5("Hosidius vinery - West 5", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_F1, 1807, 3555),
    VINERY_WEST_6("Hosidius vinery - West 6", FieldFamily.GRAPES, VarbitID.FARMING_TRANSMIT_F2, 1807, 3555),
    GREAT_CONCH_CORAL_EAST("Great Conch - East coral", FieldFamily.CORAL, VarbitID.FARMING_TRANSMIT_A, 3296, 8861),
    GREAT_CONCH_CORAL_WEST("Great Conch - West coral", FieldFamily.CORAL, VarbitID.FARMING_TRANSMIT_B, 3296, 8861),
    FARMING_GUILD_ANIMA("Farming Guild - Anima", FieldFamily.ANIMA, VarbitID.FARMING_TRANSMIT_M, 1235, 3724);
    private final String label;
    private final FieldFamily family;
    private final int varbit;
    private final WorldPoint location;
    FieldPatchDefinition(String label, FieldFamily family, int varbit, int x, int y)
    {
        this.label = label; this.family = family; this.varbit = varbit; this.location = new WorldPoint(x, y, 0);
    }
    public String getLabel() { return label; }
    public FieldFamily getFamily() { return family; }
    public int getVarbit() { return varbit; }
    public WorldPoint getLocation() { return location; }
    public boolean isNear(WorldPoint player) { return player != null && player.getPlane() == 0 && location.distanceTo2D(player) <= 12; }
}
