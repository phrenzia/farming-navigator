package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.state.HerbCrop;
import net.runelite.api.gameval.ItemID;

/** Planting levels verified against RuneLite FarmingAction; one seed per herb patch. */
public enum HerbRunCrop
{
    GUAM(ItemID.GUAM_SEED, 9),
    MARRENTILL(ItemID.MARRENTILL_SEED, 14),
    TARROMIN(ItemID.TARROMIN_SEED, 19),
    HARRALANDER(ItemID.HARRALANDER_SEED, 26),
    RANARR(ItemID.RANARR_SEED, 32),
    TOADFLAX(ItemID.TOADFLAX_SEED, 38),
    IRIT(ItemID.IRIT_SEED, 44),
    AVANTOE(ItemID.AVANTOE_SEED, 50),
    KWUARM(ItemID.KWUARM_SEED, 56),
    SNAPDRAGON(ItemID.SNAPDRAGON_SEED, 62),
    HUASCA(ItemID.HUASCA_SEED, 65),
    CADANTINE(ItemID.CADANTINE_SEED, 67),
    LANTADYME(ItemID.LANTADYME_SEED, 73),
    DWARF_WEED(ItemID.DWARF_WEED_SEED, 79),
    TORSTOL(ItemID.TORSTOL_SEED, 85);

    private final int seedId;
    private final int level;

    HerbRunCrop(int seedId, int level)
    {
        this.seedId = seedId;
        this.level = level;
    }

    public int getSeedId() { return seedId; }
    public int getRequiredLevel() { return level; }
    public String getDisplayName() { return HerbCrop.valueOf(name()).getDisplayName(); }

    @Override
    public String toString()
    {
        return getDisplayName() + " (" + level + " Farming)";
    }
}
