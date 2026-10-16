package com.farmingnavigator.routing.run;

import net.runelite.api.gameval.ItemID;

/** Filled buckets or a manually declared bottomless type. Contents and uses are not inferred. */
public enum TreeRunCompost
{
    NONE("No compost supply", 0), COMPOST("Compost", ItemID.BUCKET_COMPOST),
    SUPERCOMPOST("Supercompost", ItemID.BUCKET_SUPERCOMPOST), ULTRACOMPOST("Ultracompost", ItemID.BUCKET_ULTRACOMPOST),
    BOTTOMLESS_COMPOST("Bottomless bucket: compost (check contents)", ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED),
    BOTTOMLESS_SUPERCOMPOST("Bottomless bucket: supercompost (check contents)", ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED),
    BOTTOMLESS_ULTRACOMPOST("Bottomless bucket: ultracompost (check contents)", ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED),
    FERTILE_SOIL_SUPER("Fertile Soil (supercompost)", 0),
    FERTILE_SOIL_ULTRA("Fertile Soil (ultracompost; confirm tome)", 0);
    private final String label;
    private final int itemId;
    TreeRunCompost(String label, int itemId) { this.label = label; this.itemId = itemId; }
    public int getItemId() { return itemId; }
    public boolean isBottomless()
    {
        return this == BOTTOMLESS_COMPOST || this == BOTTOMLESS_SUPERCOMPOST || this == BOTTOMLESS_ULTRACOMPOST;
    }
    public boolean isSpell() { return this == FERTILE_SOIL_SUPER || this == FERTILE_SOIL_ULTRA; }
    @Override public String toString() { return label; }
}
