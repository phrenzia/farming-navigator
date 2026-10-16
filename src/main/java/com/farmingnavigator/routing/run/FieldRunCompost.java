package com.farmingnavigator.routing.run;

import net.runelite.api.gameval.ItemID;

/** Filled buckets only. Bottomless contents and spell care are not inferred. */
public enum FieldRunCompost
{
    NONE("No compost supply", 0), COMPOST("Compost", ItemID.BUCKET_COMPOST),
    SUPERCOMPOST("Supercompost", ItemID.BUCKET_SUPERCOMPOST), ULTRACOMPOST("Ultracompost", ItemID.BUCKET_ULTRACOMPOST);
    private final String label;
    private final int itemId;
    FieldRunCompost(String label, int itemId) { this.label = label; this.itemId = itemId; }
    public int getItemId() { return itemId; }
    @Override public String toString() { return label; }
}
