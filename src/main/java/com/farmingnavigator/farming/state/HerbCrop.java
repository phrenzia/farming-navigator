package com.farmingnavigator.farming.state;

public enum HerbCrop
{
    UNKNOWN("Unknown herb"),
    GUAM("Guam"),
    MARRENTILL("Marrentill"),
    TARROMIN("Tarromin"),
    HARRALANDER("Harralander"),
    RANARR("Ranarr"),
    TOADFLAX("Toadflax"),
    IRIT("Irit"),
    AVANTOE("Avantoe"),
    HUASCA("Huasca"),
    KWUARM("Kwuarm"),
    SNAPDRAGON("Snapdragon"),
    CADANTINE("Cadantine"),
    LANTADYME("Lantadyme"),
    DWARF_WEED("Dwarf weed"),
    TORSTOL("Torstol"),
    GOUTWEED("Goutweed");

    private final String displayName;

    HerbCrop(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
