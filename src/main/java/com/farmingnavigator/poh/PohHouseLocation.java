package com.farmingnavigator.poh;

public enum PohHouseLocation
{
    NONE("No POH / Do not use POH"),
    RIMMINGTON("Rimmington"),
    TAVERLEY("Taverley"),
    POLLNIVNEACH("Pollnivneach"),
    HOSIDIUS("Hosidius"),
    RELLEKKA("Rellekka"),
    ALDARIN("Aldarin"),
    BRIMHAVEN("Brimhaven"),
    YANILLE("Yanille"),
    PRIFDDINAS("Prifddinas");

    private final String displayName;

    PohHouseLocation(
            String displayName
    )
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public boolean hasHouse()
    {
        return this != NONE;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}