package com.farmingnavigator.routing.requirement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum InfiniteRuneSource
{
    STAFF_OF_AIR(
            "Staff of air",
            1381,
            1,
            RuneKind.AIR
    ),

    STAFF_OF_WATER(
            "Staff of water",
            1383,
            1,
            RuneKind.WATER
    ),

    STAFF_OF_EARTH(
            "Staff of earth",
            1385,
            1,
            RuneKind.EARTH
    ),

    STAFF_OF_FIRE(
            "Staff of fire",
            1387,
            1,
            RuneKind.FIRE
    ),

    FIRE_BATTLESTAFF(
            "Fire battlestaff",
            1393,
            30,
            RuneKind.FIRE
    ),

    WATER_BATTLESTAFF(
            "Water battlestaff",
            1395,
            30,
            RuneKind.WATER
    ),

    AIR_BATTLESTAFF(
            "Air battlestaff",
            1397,
            30,
            RuneKind.AIR
    ),

    EARTH_BATTLESTAFF(
            "Earth battlestaff",
            1399,
            30,
            RuneKind.EARTH
    ),

    MYSTIC_FIRE_STAFF(
            "Mystic fire staff",
            1401,
            40,
            RuneKind.FIRE
    ),

    MYSTIC_WATER_STAFF(
            "Mystic water staff",
            1403,
            40,
            RuneKind.WATER
    ),

    MYSTIC_AIR_STAFF(
            "Mystic air staff",
            1405,
            40,
            RuneKind.AIR
    ),

    MYSTIC_EARTH_STAFF(
            "Mystic earth staff",
            1407,
            40,
            RuneKind.EARTH
    ),

    LAVA_BATTLESTAFF(
            "Lava battlestaff",
            3053,
            30,
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    MYSTIC_LAVA_STAFF(
            "Mystic lava staff",
            3054,
            40,
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    MUD_BATTLESTAFF(
            "Mud battlestaff",
            6562,
            30,
            RuneKind.WATER,
            RuneKind.EARTH
    ),

    MYSTIC_MUD_STAFF(
            "Mystic mud staff",
            6563,
            40,
            RuneKind.WATER,
            RuneKind.EARTH
    ),

    STEAM_BATTLESTAFF(
            "Steam battlestaff",
            11787,
            30,
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    MYSTIC_STEAM_STAFF(
            "Mystic steam staff",
            11789,
            40,
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    SMOKE_BATTLESTAFF(
            "Smoke battlestaff",
            11998,
            30,
            RuneKind.AIR,
            RuneKind.FIRE
    ),

    MYSTIC_SMOKE_STAFF(
            "Mystic smoke staff",
            12000,
            40,
            RuneKind.AIR,
            RuneKind.FIRE
    ),

    STEAM_BATTLESTAFF_OR(
            "Steam battlestaff (or)",
            12795,
            30,
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    MYSTIC_STEAM_STAFF_OR(
            "Mystic steam staff (or)",
            12796,
            40,
            RuneKind.WATER,
            RuneKind.FIRE
    ),

    MIST_BATTLESTAFF(
            "Mist battlestaff",
            20730,
            30,
            RuneKind.AIR,
            RuneKind.WATER
    ),

    MYSTIC_MIST_STAFF(
            "Mystic mist staff",
            20733,
            40,
            RuneKind.AIR,
            RuneKind.WATER
    ),

    DUST_BATTLESTAFF(
            "Dust battlestaff",
            20736,
            30,
            RuneKind.AIR,
            RuneKind.EARTH
    ),

    MYSTIC_DUST_STAFF(
            "Mystic dust staff",
            20739,
            40,
            RuneKind.AIR,
            RuneKind.EARTH
    ),

    KODAI_WAND(
            "Kodai wand",
            21006,
            80,
            RuneKind.WATER
    ),

    LAVA_BATTLESTAFF_OR(
            "Lava battlestaff (or)",
            21198,
            30,
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    MYSTIC_LAVA_STAFF_OR(
            "Mystic lava staff (or)",
            21200,
            40,
            RuneKind.EARTH,
            RuneKind.FIRE
    ),

    TWINFLAME_STAFF(
            "Twinflame staff",
            30634,
            60,
            RuneKind.WATER,
            RuneKind.FIRE
    );

    private final String displayName;

    private final int itemId;

    private final int minimumRealMagicLevel;

    private final Set<RuneKind> suppliedKinds;

    InfiniteRuneSource(
            String displayName,
            int itemId,
            int minimumRealMagicLevel,
            RuneKind... suppliedKinds
    )
    {
        this.displayName =
                displayName;

        this.itemId =
                itemId;

        this.minimumRealMagicLevel =
                minimumRealMagicLevel;

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