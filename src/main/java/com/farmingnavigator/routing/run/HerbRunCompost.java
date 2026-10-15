package com.farmingnavigator.routing.run;

import net.runelite.api.gameval.ItemID;

public enum HerbRunCompost
{
    NONE("No compost supply", 0),
    COMPOST("Compost", ItemID.BUCKET_COMPOST),
    SUPERCOMPOST("Supercompost", ItemID.BUCKET_SUPERCOMPOST),
    ULTRACOMPOST("Ultracompost", ItemID.BUCKET_ULTRACOMPOST),
    BOTTOMLESS_COMPOST("Bottomless bucket: compost (check contents)", ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED),
    BOTTOMLESS_SUPERCOMPOST("Bottomless bucket: supercompost (check contents)", ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED),
    BOTTOMLESS_ULTRACOMPOST("Bottomless bucket: ultracompost (check contents)", ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);

    private final String name;
    private final int itemId;

    HerbRunCompost(String name, int itemId)
    {
        this.name = name;
        this.itemId = itemId;
    }

    public int getItemId() { return itemId; }
    public boolean isBottomless()
    {
        return this == BOTTOMLESS_COMPOST || this == BOTTOMLESS_SUPERCOMPOST || this == BOTTOMLESS_ULTRACOMPOST;
    }
    @Override public String toString() { return name; }
}
