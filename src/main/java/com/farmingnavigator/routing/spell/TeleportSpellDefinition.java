package com.farmingnavigator.routing.spell;

import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.requirement.RuneCost;
import com.farmingnavigator.routing.requirement.RuneKind;
import net.runelite.api.Quest;

public enum TeleportSpellDefinition
{
    VARROCK_TELEPORT("Varrock Teleport", "Varrock town square (or configured Grand Exchange redirect)", 25,
            SpellbookType.STANDARD, RuneCost.builder().add(RuneKind.AIR, 3).add(RuneKind.FIRE, 1)
                    .add(RuneKind.LAW, 1).build(), null, false),
    LUMBRIDGE_TELEPORT("Lumbridge Teleport", "Lumbridge Castle courtyard", 31,
            SpellbookType.STANDARD, RuneCost.builder().add(RuneKind.AIR, 3).add(RuneKind.EARTH, 1)
                    .add(RuneKind.LAW, 1).build(), null, false),
    DRAYNOR_MANOR_TELEPORT(
            "Draynor Manor Teleport",
            "Draynor Manor",
            17,
            SpellbookType.ARCEUUS,
            RuneCost.builder()
                    .add(
                            RuneKind.EARTH,
                            1
                    )
                    .add(
                            RuneKind.WATER,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            1
                    )
                    .build(),
            null,
            false
    ),

    BATTLEFRONT_TELEPORT(
            "Battlefront Teleport",
            "Ancient Grave outside the Kebos Battlefront",
            23,
            SpellbookType.ARCEUUS,
            RuneCost.builder()
                    .add(
                            RuneKind.EARTH,
                            1
                    )
                    .add(
                            RuneKind.FIRE,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            1
                    )
                    .build(),
            null,
            false
    ),

    FALADOR_TELEPORT(
            "Falador Teleport",
            "Centre of Falador",
            37,
            SpellbookType.STANDARD,
            RuneCost.builder()
                    .add(
                            RuneKind.AIR,
                            3
                    )
                    .add(
                            RuneKind.WATER,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            1
                    )
                    .build(),
            null,
            false
    ),

    SALVE_GRAVEYARD_TELEPORT(
            "Salve Graveyard Teleport",
            "Ghoul area near the entrance to Mort Myre Swamp",
            40,
            SpellbookType.ARCEUUS,
            RuneCost.builder()
                    .add(
                            RuneKind.SOUL,
                            2
                    )
                    .add(
                            RuneKind.LAW,
                            1
                    )
                    .build(),
            Quest.PRIEST_IN_PERIL,
            false
    ),

    TELEPORT_TO_HOUSE(
            "Teleport to House",
            "Player-owned house",
            40,
            SpellbookType.STANDARD,
            RuneCost.builder()
                    .add(
                            RuneKind.AIR,
                            1
                    )
                    .add(
                            RuneKind.EARTH,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            1
                    )
                    .build(),
            null,
            true
    ),

    CAMELOT_TELEPORT(
            "Camelot Teleport",
            "Gates of Camelot",
            45,
            SpellbookType.STANDARD,
            RuneCost.builder()
                    .add(
                            RuneKind.AIR,
                            5
                    )
                    .add(
                            RuneKind.LAW,
                            1
                    )
                    .build(),
            null,
            false
    ),

    KOUREND_CASTLE_TELEPORT(
            "Kourend Castle Teleport",
            "Kourend Castle courtyard",
            48,
            SpellbookType.STANDARD,
            RuneCost.builder()
                    .add(
                            RuneKind.WATER,
                            1
                    )
                    .add(
                            RuneKind.FIRE,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            2
                    )
                    .build(),
            Quest.CLIENT_OF_KOUREND,
            false
    ),

    FENKENSTRAINS_CASTLE_TELEPORT(
            "Fenkenstrain's Castle Teleport",
            "Fenkenstrain's Castle",
            48,
            SpellbookType.ARCEUUS,
            RuneCost.builder()
                    .add(
                            RuneKind.EARTH,
                            1
                    )
                    .add(
                            RuneKind.SOUL,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            1
                    )
                    .build(),
            Quest.PRIEST_IN_PERIL,
            false
    ),

    /* Requirements corroborated by Plugin Hub Shortest Path's teleportation_spells.tsv. */
    ARDOUGNE_TELEPORT(
            "Ardougne Teleport",
            "East Ardougne market",
            51,
            SpellbookType.STANDARD,
            RuneCost.builder()
                    .add(RuneKind.WATER, 2)
                    .add(RuneKind.LAW, 2)
                    .build(),
            Quest.PLAGUE_CITY,
            false
    ),

    CIVITAS_ILLA_FORTIS_TELEPORT(
            "Civitas illa Fortis Teleport",
            "Sunrise Palace in Civitas illa Fortis",
            54,
            SpellbookType.STANDARD,
            RuneCost.builder()
                    .add(
                            RuneKind.EARTH,
                            1
                    )
                    .add(
                            RuneKind.FIRE,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            2
                    )
                    .build(),
            Quest.TWILIGHTS_PROMISE,
            false
    ),

    TROLLHEIM_TELEPORT(
            "Trollheim Teleport",
            "Top of Trollheim",
            61,
            SpellbookType.STANDARD,
            RuneCost.builder()
                    .add(
                            RuneKind.FIRE,
                            2
                    )
                    .add(
                            RuneKind.LAW,
                            2
                    )
                    .build(),
            Quest.EADGARS_RUSE,
            false
    ),

    HARMONY_ISLAND_TELEPORT(
            "Harmony Island Teleport",
            "Harmony Island",
            65,
            SpellbookType.ARCEUUS,
            RuneCost.builder()
                    .add(
                            RuneKind.NATURE,
                            1
                    )
                    .add(
                            RuneKind.SOUL,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            1
                    )
                    .build(),
            Quest.THE_GREAT_BRAIN_ROBBERY,
            false
    ),

    KHARYRLL_TELEPORT(
            "Kharyrll Teleport",
            "Canifis",
            66,
            SpellbookType.ANCIENT,
            RuneCost.builder()
                    .add(
                            RuneKind.BLOOD,
                            1
                    )
                    .add(
                            RuneKind.LAW,
                            2
                    )
                    .build(),
            Quest.DESERT_TREASURE_I,
            false
    ),

    FISHING_GUILD_TELEPORT(
            "Fishing Guild Teleport",
            "Entrance to the Fishing Guild",
            85,
            SpellbookType.LUNAR,
            RuneCost.builder()
                    .add(
                            RuneKind.WATER,
                            10
                    )
                    .add(
                            RuneKind.ASTRAL,
                            3
                    )
                    .add(
                            RuneKind.LAW,
                            3
                    )
                    .build(),
            Quest.LUNAR_DIPLOMACY,
            false
    ),

    CATHERBY_TELEPORT(
            "Catherby Teleport",
            "Near the Catherby bank",
            87,
            SpellbookType.LUNAR,
            RuneCost.builder()
                    .add(
                            RuneKind.WATER,
                            10
                    )
                    .add(
                            RuneKind.ASTRAL,
                            3
                    )
                    .add(
                            RuneKind.LAW,
                            3
                    )
                    .build(),
            Quest.LUNAR_DIPLOMACY,
            false
    );

    private final String displayName;

    private final String destinationDescription;

    private final int requiredMagicLevel;

    private final SpellbookType spellbook;

    private final RuneCost runeCost;

    private final Quest requiredQuest;

    private final boolean requiresConfiguredHouse;

    TeleportSpellDefinition(
            String displayName,
            String destinationDescription,
            int requiredMagicLevel,
            SpellbookType spellbook,
            RuneCost runeCost,
            Quest requiredQuest,
            boolean requiresConfiguredHouse
    )
    {
        this.displayName =
                displayName;

        this.destinationDescription =
                destinationDescription;

        this.requiredMagicLevel =
                requiredMagicLevel;

        this.spellbook =
                spellbook;

        this.runeCost =
                runeCost;

        this.requiredQuest =
                requiredQuest;

        this.requiresConfiguredHouse =
                requiresConfiguredHouse;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getDestinationDescription()
    {
        return destinationDescription;
    }

    public int getRequiredMagicLevel()
    {
        return requiredMagicLevel;
    }

    public SpellbookType getSpellbook()
    {
        return spellbook;
    }

    public RuneCost getRuneCost()
    {
        return runeCost;
    }

    public Quest getRequiredQuest()
    {
        return requiredQuest;
    }

    public boolean requiresConfiguredHouse()
    {
        return requiresConfiguredHouse;
    }

    public TeleportSpellRequirement
    createRequirement()
    {
        return new TeleportSpellRequirement(
                this
        );
    }
}
