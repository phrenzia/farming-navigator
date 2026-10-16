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
package com.farmingnavigator.contracts;

import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.TreeRunCrop;
import com.farmingnavigator.routing.run.GardenRunCrop;

/** Assignment IDs verified against pinned RuneLite Produce contractVarbitValue metadata. */
public enum ContractCrop
{
    REDBERRIES(1, "Redberry", Family.BUSH), CADAVABERRIES(2, "Cadavaberry", Family.BUSH),
    DWELLBERRIES(3, "Dwellberry", Family.BUSH), JANGERBERRIES(4, "Jangerberry", Family.BUSH),
    WHITEBERRIES(5, "Whiteberry", Family.BUSH), POISON_IVY(6, "Poison ivy", Family.BUSH),
    OAK(7, "Oak", Family.TREE), WILLOW(8, "Willow", Family.TREE), MAPLE(9, "Maple", Family.TREE),
    YEW(10, "Yew", Family.TREE), MAGIC(11, "Magic", Family.TREE),
    APPLE(12, "Apple", Family.FRUIT_TREE), BANANA(13, "Banana", Family.FRUIT_TREE), ORANGE(14, "Orange", Family.FRUIT_TREE),
    CURRY(15, "Curry", Family.FRUIT_TREE), PINEAPPLE(16, "Pineapple", Family.FRUIT_TREE), PAPAYA(17, "Papaya", Family.FRUIT_TREE),
    PALM(18, "Palm", Family.FRUIT_TREE), DRAGONFRUIT(19, "Dragonfruit", Family.FRUIT_TREE),
    GUAM(20, "Guam", Family.HERB), MARRENTILL(21, "Marrentill", Family.HERB), TARROMIN(22, "Tarromin", Family.HERB),
    HARRALANDER(23, "Harralander", Family.HERB), RANARR(24, "Ranarr", Family.HERB), TOADFLAX(25, "Toadflax", Family.HERB),
    IRIT(26, "Irit", Family.HERB), AVANTOE(27, "Avantoe", Family.HERB), KWUARM(28, "Kwuarm", Family.HERB),
    SNAPDRAGON(29, "Snapdragon", Family.HERB), CADANTINE(30, "Cadantine", Family.HERB), LANTADYME(31, "Lantadyme", Family.HERB),
    DWARF_WEED(32, "Dwarf weed", Family.HERB), TORSTOL(33, "Torstol", Family.HERB),
    MARIGOLD(34, "Marigold", Family.FLOWER), ROSEMARY(35, "Rosemary", Family.FLOWER), NASTURTIUM(36, "Nasturtium", Family.FLOWER),
    WOAD(37, "Woad", Family.FLOWER), LIMPWURT(38, "Limpwurt", Family.FLOWER), WHITE_LILY(39, "White lily", Family.FLOWER),
    POTATO(40, "Potato", Family.ALLOTMENT), ONION(41, "Onion", Family.ALLOTMENT), CABBAGE(42, "Cabbage", Family.ALLOTMENT),
    TOMATO(43, "Tomato", Family.ALLOTMENT), SWEETCORN(44, "Sweetcorn", Family.ALLOTMENT), STRAWBERRY(45, "Strawberry", Family.ALLOTMENT),
    WATERMELON(46, "Watermelon", Family.ALLOTMENT), SNAPE_GRASS(47, "Snape grass", Family.ALLOTMENT),
    CACTUS(48, "Cactus", Family.CACTUS), POTATO_CACTUS(49, "Potato cactus", Family.CACTUS),
    REDWOOD(50, "Redwood", Family.REDWOOD), CELASTRUS(51, "Celastrus", Family.CELASTRUS);

    public enum Family
    {
        BUSH("Bush"), TREE("Regular tree"), FRUIT_TREE("Fruit tree"), HERB("Herb"), FLOWER("Flower"),
        ALLOTMENT("Allotment"), CACTUS("Cactus"), REDWOOD("Redwood"), CELASTRUS("Celastrus");
        private final String label;
        Family(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
    private final int id;
    private final String label;
    private final Family family;
    ContractCrop(int id, String label, Family family) { this.id = id; this.label = label; this.family = family; }
    public int getId() { return id; }
    public String getLabel() { return label; }
    public Family getFamily() { return family; }
    public HerbRunCrop getHerbRunCrop() { return family == Family.HERB ? HerbRunCrop.valueOf(name()) : null; }
    public com.farmingnavigator.routing.run.FieldRunCrop getFieldRunCrop()
    {
        return family == Family.ALLOTMENT || family == Family.FLOWER ? com.farmingnavigator.routing.run.FieldRunCrop.valueOf(name()) : null;
    }
    public GardenRunCrop getGardenRunCrop() { return family == Family.BUSH || family == Family.CACTUS ? GardenRunCrop.valueOf(name()) : null; }
    public TreeRunCrop getTreeRunCrop()
    {
        return family == Family.TREE || family == Family.FRUIT_TREE || family == Family.REDWOOD || family == Family.CELASTRUS
                ? TreeRunCrop.valueOf(name()) : null;
    }
    public static ContractCrop fromId(int id)
    {
        for (ContractCrop crop : values()) { if (crop.id == id) { return crop; } }
        return null;
    }
}

