package com.farmingnavigator.transport;

public enum TransportCategory
{
    DIARY_ITEM("Diary item"),
    ITEM("Item"),
    SPELL("Spell"),
    JEWELLERY("Jewellery"),
    FAIRY_RING("Fairy ring"),
    PLAYER_OWNED_HOUSE("Player-owned house"),
    SKILLCAPE("Skillcape"),
    TRANSPORT_NETWORK("Transport network"),
    MINIGAME("Minigame"),
    OTHER("Other");

    private final String displayName;

    TransportCategory(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}