package com.farmingnavigator.account;

public enum DiaryArea
{
    ARDOUGNE("Ardougne"),
    FALADOR("Falador"),
    FREMENNIK("Fremennik"),
    KANDARIN("Kandarin"),
    KOUREND_AND_KEBOS("Kourend & Kebos"),
    LUMBRIDGE_AND_DRAYNOR("Lumbridge & Draynor"),
    MORYTANIA("Morytania");

    private final String displayName;

    DiaryArea(
            String displayName
    )
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}