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

import net.runelite.api.gameval.VarbitID;

/** Patch identities and transmit assignments from RuneLite FarmingWorld. */
public enum TreePatchDefinition
{
    VARROCK_TREE("Varrock", TreeFamily.TREE, VarbitID.FARMING_TRANSMIT_A),
    TAVERLEY_TREE("Taverley", TreeFamily.TREE, VarbitID.FARMING_TRANSMIT_A),
    FALADOR_TREE("Falador", TreeFamily.TREE, VarbitID.FARMING_TRANSMIT_A),
    LUMBRIDGE_TREE("Lumbridge", TreeFamily.TREE, VarbitID.FARMING_TRANSMIT_A),
    GNOME_STRONGHOLD_TREE("Gnome Stronghold", TreeFamily.TREE, VarbitID.FARMING_TRANSMIT_A),
    FARMING_GUILD_TREE("Farming Guild", TreeFamily.TREE, VarbitID.FARMING_TRANSMIT_G),
    AUBURNVALE_TREE("Auburnvale", TreeFamily.TREE, VarbitID.FARMING_TRANSMIT_A),
    BRIMHAVEN_FRUIT("Brimhaven", TreeFamily.FRUIT_TREE, VarbitID.FARMING_TRANSMIT_A),
    CATHERBY_FRUIT("Catherby", TreeFamily.FRUIT_TREE, VarbitID.FARMING_TRANSMIT_A),
    GNOME_STRONGHOLD_FRUIT("Gnome Stronghold", TreeFamily.FRUIT_TREE, VarbitID.FARMING_TRANSMIT_B),
    TREE_GNOME_VILLAGE_FRUIT("Tree Gnome Village", TreeFamily.FRUIT_TREE, VarbitID.FARMING_TRANSMIT_A),
    LLETYA_FRUIT("Lletya", TreeFamily.FRUIT_TREE, VarbitID.FARMING_TRANSMIT_A),
    FARMING_GUILD_FRUIT("Farming Guild", TreeFamily.FRUIT_TREE, VarbitID.FARMING_TRANSMIT_K),
    KASTORI_FRUIT("Kastori", TreeFamily.FRUIT_TREE, VarbitID.FARMING_TRANSMIT_B),
    FOSSIL_ISLAND_EAST("Fossil Island east", TreeFamily.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_A),
    FOSSIL_ISLAND_MIDDLE("Fossil Island middle", TreeFamily.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_B),
    FOSSIL_ISLAND_WEST("Fossil Island west", TreeFamily.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_C),
    AVIUM_SAVANNAH_HARDWOOD("Avium Savannah", TreeFamily.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_A),
    ANGLERS_RETREAT_HARDWOOD("Anglers' Retreat", TreeFamily.HARDWOOD_TREE, VarbitID.FARMING_TRANSMIT_A),
    TAI_BWO_WANNAI_CALQUAT("Tai Bwo Wannai", TreeFamily.CALQUAT, VarbitID.FARMING_TRANSMIT_A),
    KASTORI_CALQUAT("Kastori", TreeFamily.CALQUAT, VarbitID.FARMING_TRANSMIT_A),
    GREAT_CONCH_CALQUAT("Great Conch", TreeFamily.CALQUAT, VarbitID.FARMING_TRANSMIT_C),
    FARMING_GUILD_CELASTRUS("Farming Guild", TreeFamily.CELASTRUS, VarbitID.FARMING_TRANSMIT_L),
    FARMING_GUILD_REDWOOD("Farming Guild", TreeFamily.REDWOOD, VarbitID.FARMING_TRANSMIT_I),
    PRIFDDINAS_CRYSTAL("Prifddinas", TreeFamily.CRYSTAL_TREE, VarbitID.FARMING_TRANSMIT_E);

    private final String label;
    private final TreeFamily family;
    private final int varbit;
    TreePatchDefinition(String label, TreeFamily family, int varbit)
    {
        this.label = label;
        this.family = family;
        this.varbit = varbit;
    }
    public String getLabel() { return label; }
    public TreeFamily getFamily() { return family; }
    public int getVarbit() { return varbit; }
}
