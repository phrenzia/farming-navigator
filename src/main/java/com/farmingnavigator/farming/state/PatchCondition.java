package com.farmingnavigator.farming.state;

public enum PatchCondition
{
    UNKNOWN("Data required"),
    EMPTY("Empty"),
    WEEDS("Weeds present"),
    GROWING("Growing"),
    HARVESTABLE("Ready to harvest"),
    DISEASED("Diseased"),
    DEAD("Dead");

    private final String displayName;

    PatchCondition(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
