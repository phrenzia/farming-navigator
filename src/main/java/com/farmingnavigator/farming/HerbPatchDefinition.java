package com.farmingnavigator.farming;

import net.runelite.api.Quest;

public enum HerbPatchDefinition
{
    FALADOR(
            "Falador",
            0,
            null,
            false,
            false
    ),

    CATHERBY(
            "Catherby",
            0,
            null,
            false,
            false
    ),

    ARDOUGNE(
            "Ardougne",
            0,
            null,
            false,
            false
    ),

    MORYTANIA(
            "Port Phasmatys",
            0,
            Quest.PRIEST_IN_PERIL,
            false,
            false
    ),

    HOSIDIUS(
            "Hosidius",
            0,
            null,
            false,
            false
    ),

    TROLL_STRONGHOLD(
            "Troll Stronghold",
            0,
            Quest.MY_ARMS_BIG_ADVENTURE,
            false,
            true
    ),

    WEISS(
            "Weiss",
            0,
            Quest.MAKING_FRIENDS_WITH_MY_ARM,
            false,
            true
    ),

    FARMING_GUILD(
            "Farming Guild",
            65,
            null,
            false,
            false
    ),

    HARMONY_ISLAND(
            "Harmony Island",
            0,
            null,
            true,
            true
    ),

    ORTUS_FARM(
            "Ortus Farm (Varlamore)",
            0,
            Quest.CHILDREN_OF_THE_SUN,
            false,
            false
    );

    private final String displayName;

    private final int minimumFarmingLevel;

    private final Quest requiredQuest;

    private final boolean
            requiresMorytaniaEliteReward;

    private final boolean
            alwaysDiseaseFree;

    HerbPatchDefinition(
            String displayName,
            int minimumFarmingLevel,
            Quest requiredQuest,
            boolean requiresMorytaniaEliteReward,
            boolean alwaysDiseaseFree
    )
    {
        this.displayName =
                displayName;

        this.minimumFarmingLevel =
                minimumFarmingLevel;

        this.requiredQuest =
                requiredQuest;

        this.requiresMorytaniaEliteReward =
                requiresMorytaniaEliteReward;

        this.alwaysDiseaseFree =
                alwaysDiseaseFree;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public int getMinimumFarmingLevel()
    {
        return minimumFarmingLevel;
    }

    public Quest getRequiredQuest()
    {
        return requiredQuest;
    }

    public boolean requiresMorytaniaEliteReward()
    {
        return requiresMorytaniaEliteReward;
    }

    public boolean isAlwaysDiseaseFree()
    {
        return alwaysDiseaseFree;
    }
}