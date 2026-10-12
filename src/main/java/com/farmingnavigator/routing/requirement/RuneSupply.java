package com.farmingnavigator.routing.requirement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

public enum RuneSupply
{
    AIR_RUNE(
            ItemID.AIRRUNE,
            "Air rune",
            RuneKind.AIR
    ),

    WATER_RUNE(
            ItemID.WATERRUNE,
            "Water rune",
            RuneKind.WATER
    ),

    EARTH_RUNE(
            ItemID.EARTHRUNE,
            "Earth rune",
            RuneKind.EARTH
    ),

    FIRE_RUNE(
            ItemID.FIRERUNE,
            "Fire rune",
            RuneKind.FIRE
    ),

    COSMIC_RUNE(
            ItemID.COSMICRUNE,
            "Cosmic rune",
            RuneKind.COSMIC
    ),

    ASTRAL_RUNE(
            ItemID.ASTRALRUNE,
            "Astral rune",
            RuneKind.ASTRAL
    ),

    NATURE_RUNE(
            ItemID.NATURERUNE,
            "Nature rune",
            RuneKind.NATURE
    ),

    LAW_RUNE(
            ItemID.LAWRUNE,
            "Law rune",
            RuneKind.LAW
    ),

    SOUL_RUNE(
            ItemID.SOULRUNE,
            "Soul rune",
            RuneKind.SOUL
    ),

    MIST_RUNE(
            ItemID.MISTRUNE,
            "Mist rune",
            RuneKind.AIR,
            RuneKind.WATER
    ),

    DUST_RUNE(
            ItemID.DUSTRUNE,
            "Dust rune",
            RuneKind.AIR,
            RuneKind.EARTH
    ),

    MUD_RUNE(
            ItemID.MUDRUNE,
            "Mud rune",
            RuneKind.WATER,
            RuneKind.EARTH
    ),

    SMOKE_RUNE(
            ItemID.SMOKERUNE,
            "Smoke rune",
            RuneKind.AIR,
            RuneKind.FIRE
    ),

    STEAM_RUNE(
            ItemID.STEAMRUNE,
            "Steam rune",
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    LAVA_RUNE(
            ItemID.LAVARUNE,
            "Lava rune",
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    /*
     * Current 2026 game data allows Sunfire runes to replace
     * ordinary fire runes for non-combat spells such as teleports.
     *
     * Farming Navigator's route spells are non-combat teleports.
     */
    SUNFIRE_RUNE(
            ItemID.SUNFIRERUNE,
            "Sunfire rune",
            RuneKind.FIRE
    ),

    /*
     * Aether runes act as both Cosmic and Soul runes.
     */
    AETHER_RUNE(
            ItemID.AETHERRUNE,
            "Aether rune",
            RuneKind.COSMIC,
            RuneKind.SOUL
    );

    private final int itemId;

    private final String displayName;

    private final Set<RuneKind> suppliedKinds;

    RuneSupply(
            int itemId,
            String displayName,
            RuneKind... suppliedKinds
    )
    {
        this.itemId =
                itemId;

        this.displayName =
                displayName;

        EnumSet<RuneKind> kinds =
                EnumSet.noneOf(
                        RuneKind.class
                );

        if (suppliedKinds != null)
        {
            Collections.addAll(
                    kinds,
                    suppliedKinds
            );
        }

        this.suppliedKinds =
                Collections.unmodifiableSet(
                        kinds
                );
    }

    public int getItemId()
    {
        return itemId;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public Set<RuneKind> getSuppliedKinds()
    {
        return suppliedKinds;
    }

    public boolean supplies(
            RuneKind kind
    )
    {
        return suppliedKinds.contains(
                kind
        );
    }
}