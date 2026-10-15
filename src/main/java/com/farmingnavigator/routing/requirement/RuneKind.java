package com.farmingnavigator.routing.requirement;

import net.runelite.api.gameval.ItemID;

public enum RuneKind
{
    AIR(
            "Air rune",
            ItemID.AIRRUNE
    ),

    WATER(
            "Water rune",
            ItemID.WATERRUNE
    ),

    EARTH(
            "Earth rune",
            ItemID.EARTHRUNE
    ),

    FIRE(
            "Fire rune",
            ItemID.FIRERUNE
    ),

    COSMIC(
            "Cosmic rune",
            ItemID.COSMICRUNE
    ),

    ASTRAL(
            "Astral rune",
            ItemID.ASTRALRUNE
    ),

    NATURE(
            "Nature rune",
            ItemID.NATURERUNE
    ),

    LAW(
            "Law rune",
            ItemID.LAWRUNE
    ),

    BLOOD(
            "Blood rune",
            ItemID.BLOODRUNE
    ),

    SOUL(
            "Soul rune",
            ItemID.SOULRUNE
    );

    private final String displayName;

    private final int canonicalItemId;

    RuneKind(
            String displayName,
            int canonicalItemId
    )
    {
        this.displayName =
                displayName;

        this.canonicalItemId =
                canonicalItemId;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public int getCanonicalItemId()
    {
        return canonicalItemId;
    }
}