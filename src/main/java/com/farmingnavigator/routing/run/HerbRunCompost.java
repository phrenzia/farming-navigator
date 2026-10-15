package com.farmingnavigator.routing.run;

import net.runelite.api.gameval.ItemID;

public enum HerbRunCompost
{
    NONE("No compost supply", 0),
    COMPOST("Compost", ItemID.BUCKET_COMPOST),
    SUPERCOMPOST("Supercompost", ItemID.BUCKET_SUPERCOMPOST),
    ULTRACOMPOST("Ultracompost", ItemID.BUCKET_ULTRACOMPOST);

    private final String name;
    private final int itemId;

    HerbRunCompost(String name, int itemId)
    {
        this.name = name;
        this.itemId = itemId;
    }

    public int getItemId() { return itemId; }
    @Override public String toString() { return name; }
}
