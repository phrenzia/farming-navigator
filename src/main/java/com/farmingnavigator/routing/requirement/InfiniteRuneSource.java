package com.farmingnavigator.routing.requirement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Wield levels are unboosted. Battlestaves need 30 Attack; mystic staves need 40.
 * https://oldschool.runescape.wiki/w/Battlestaves
 * https://oldschool.runescape.wiki/w/Mystic_staff
 */
public enum InfiniteRuneSource
{
    STAFF_OF_AIR(
            "Staff of air",
            ItemID.STAFF_OF_AIR,
            1,
            1,
            RuneKind.AIR
    ),

    STAFF_OF_WATER(
            "Staff of water",
            ItemID.STAFF_OF_WATER,
            1,
            1,
            RuneKind.WATER
    ),

    STAFF_OF_EARTH(
            "Staff of earth",
            ItemID.STAFF_OF_EARTH,
            1,
            1,
            RuneKind.EARTH
    ),

    STAFF_OF_FIRE(
            "Staff of fire",
            ItemID.STAFF_OF_FIRE,
            1,
            1,
            RuneKind.FIRE
    ),

    FIRE_BATTLESTAFF(
            "Fire battlestaff",
            ItemID.FIRE_BATTLESTAFF,
            30,
            30,
            RuneKind.FIRE
    ),

    WATER_BATTLESTAFF(
            "Water battlestaff",
            ItemID.WATER_BATTLESTAFF,
            30,
            30,
            RuneKind.WATER
    ),

    AIR_BATTLESTAFF(
            "Air battlestaff",
            ItemID.AIR_BATTLESTAFF,
            30,
            30,
            RuneKind.AIR
    ),

    EARTH_BATTLESTAFF(
            "Earth battlestaff",
            ItemID.EARTH_BATTLESTAFF,
            30,
            30,
            RuneKind.EARTH
    ),

    MYSTIC_FIRE_STAFF(
            "Mystic fire staff",
            ItemID.MYSTIC_FIRE_STAFF,
            40,
            40,
            RuneKind.FIRE
    ),

    MYSTIC_WATER_STAFF(
            "Mystic water staff",
            ItemID.MYSTIC_WATER_STAFF,
            40,
            40,
            RuneKind.WATER
    ),

    MYSTIC_AIR_STAFF(
            "Mystic air staff",
            ItemID.MYSTIC_AIR_STAFF,
            40,
            40,
            RuneKind.AIR
    ),

    MYSTIC_EARTH_STAFF(
            "Mystic earth staff",
            ItemID.MYSTIC_EARTH_STAFF,
            40,
            40,
            RuneKind.EARTH
    ),

    LAVA_BATTLESTAFF(
            "Lava battlestaff",
            ItemID.LAVA_BATTLESTAFF,
            30,
            30,
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    MYSTIC_LAVA_STAFF(
            "Mystic lava staff",
            ItemID.MYSTIC_LAVA_STAFF,
            40,
            40,
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    MUD_BATTLESTAFF(
            "Mud battlestaff",
            ItemID.MUD_BATTLESTAFF,
            30,
            30,
            RuneKind.WATER,
            RuneKind.EARTH
    ),

    MYSTIC_MUD_STAFF(
            "Mystic mud staff",
            ItemID.MYSTIC_MUD_STAFF,
            40,
            40,
            RuneKind.WATER,
            RuneKind.EARTH
    ),

    STEAM_BATTLESTAFF(
            "Steam battlestaff",
            ItemID.STEAM_BATTLESTAFF,
            30,
            30,
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    MYSTIC_STEAM_STAFF(
            "Mystic steam staff",
            ItemID.MYSTIC_STEAM_BATTLESTAFF,
            40,
            40,
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    SMOKE_BATTLESTAFF(
            "Smoke battlestaff",
            ItemID.SMOKE_BATTLESTAFF,
            30,
            30,
            RuneKind.AIR,
            RuneKind.FIRE
    ),

    MYSTIC_SMOKE_STAFF(
            "Mystic smoke staff",
            ItemID.MYSTIC_SMOKE_BATTLESTAFF,
            40,
            40,
            RuneKind.AIR,
            RuneKind.FIRE
    ),

    STEAM_BATTLESTAFF_OR(
            "Steam battlestaff (or)",
            ItemID.STEAM_BATTLESTAFF_PRETTY,
            30,
            30,
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    MYSTIC_STEAM_STAFF_OR(
            "Mystic steam staff (or)",
            ItemID.MYSTIC_STEAM_BATTLESTAFF_PRETTY,
            40,
            40,
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    MIST_BATTLESTAFF(
            "Mist battlestaff",
            ItemID.MIST_BATTLESTAFF,
            30,
            30,
            RuneKind.AIR,
            RuneKind.WATER
    ),

    MYSTIC_MIST_STAFF(
            "Mystic mist staff",
            ItemID.MYSTIC_MIST_BATTLESTAFF,
            40,
            40,
            RuneKind.AIR,
            RuneKind.WATER
    ),

    DUST_BATTLESTAFF(
            "Dust battlestaff",
            ItemID.DUST_BATTLESTAFF,
            30,
            30,
            RuneKind.AIR,
            RuneKind.EARTH
    ),

    MYSTIC_DUST_STAFF(
            "Mystic dust staff",
            ItemID.MYSTIC_DUST_BATTLESTAFF,
            40,
            40,
            RuneKind.AIR,
            RuneKind.EARTH
    ),

    KODAI_WAND(
            "Kodai wand",
            ItemID.KODAI_WAND,
            80,
            1,
            RuneKind.WATER
    ),

    LAVA_BATTLESTAFF_OR(
            "Lava battlestaff (or)",
            ItemID.LAVA_BATTLESTAFF_PRETTY,
            30,
            30,
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    MYSTIC_LAVA_STAFF_OR(
            "Mystic lava staff (or)",
            ItemID.MYSTIC_LAVA_STAFF_PRETTY,
            40,
            40,
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    TWINFLAME_STAFF(
            "Twinflame staff",
            ItemID.TWINFLAME_STAFF,
            60,
            1,
            RuneKind.WATER,
            RuneKind.FIRE
    );

    private final String displayName;

    private final int itemId;

    private final int minimumRealMagicLevel;
    private final int minimumRealAttackLevel;

    private final Set<RuneKind> suppliedKinds;

    InfiniteRuneSource(
            String displayName,
            int itemId,
            int minimumRealMagicLevel,
            int minimumRealAttackLevel,
            RuneKind... suppliedKinds
    )
    {
        this.displayName =
                displayName;

        this.itemId =
                itemId;

        this.minimumRealMagicLevel =
                minimumRealMagicLevel;
        this.minimumRealAttackLevel = minimumRealAttackLevel;

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

    public String getDisplayName()
    {
        return displayName;
    }

    public int getItemId()
    {
        return itemId;
    }

    public int getMinimumRealMagicLevel()
    {
        return minimumRealMagicLevel;
    }

    public int getMinimumRealAttackLevel() { return minimumRealAttackLevel; }

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
