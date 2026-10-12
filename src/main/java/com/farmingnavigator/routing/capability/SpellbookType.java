package com.farmingnavigator.routing.capability;

public enum SpellbookType
{
    STANDARD(
            0,
            "Standard"
    ),

    ANCIENT(
            1,
            "Ancient Magicks"
    ),

    LUNAR(
            2,
            "Lunar"
    ),

    ARCEUUS(
            3,
            "Arceuus"
    ),

    UNKNOWN(
            -1,
            "Unknown"
    );

    private final int varbitValue;

    private final String displayName;

    SpellbookType(
            int varbitValue,
            String displayName
    )
    {
        this.varbitValue =
                varbitValue;

        this.displayName =
                displayName;
    }

    public int getVarbitValue()
    {
        return varbitValue;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public static SpellbookType fromVarbitValue(
            int value
    )
    {
        for (
                SpellbookType spellbook
                : values()
        )
        {
            if (
                    spellbook != UNKNOWN
                            && spellbook.varbitValue
                            == value
            )
            {
                return spellbook;
            }
        }

        return UNKNOWN;
    }
}