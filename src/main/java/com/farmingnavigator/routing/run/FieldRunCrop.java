package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.field.FieldCrop;
import com.farmingnavigator.loadout.SupplyRequirement;
import java.util.Optional;
import net.runelite.api.gameval.ItemID;

/** Verified seed/spore counts and protection. Seaweed: one spore and 200 numulites; mushroom/belladonna: one, no farmer.
 * https://oldschoolrunescape.fandom.com/wiki/Seaweed_spore
 * https://oldschoolrunescape.fandom.com/wiki/Mushroom_patch
 * https://oldschoolrunescape.fandom.com/wiki/Farming
 */
public enum FieldRunCrop
{
    POTATO(FieldCrop.POTATO, 1, ItemID.POTATO_SEED, ItemID.BUCKET_COMPOST, ItemID.Cert.BUCKET_COMPOST, 2, "Compost (farmer payment)"),
    ONION(FieldCrop.ONION, 5, ItemID.ONION_SEED, ItemID.SACK_POTATO_10, ItemID.Cert.SACK_POTATO_10, 1, "Potatoes (full sack of 10)"),
    CABBAGE(FieldCrop.CABBAGE, 7, ItemID.CABBAGE_SEED, ItemID.SACK_ONION_10, ItemID.Cert.SACK_ONION_10, 1, "Onions (full sack of 10)"),
    TOMATO(FieldCrop.TOMATO, 12, ItemID.TOMATO_SEED, ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10, 2, "Cabbages (full sack of 10)"),
    SWEETCORN(FieldCrop.SWEETCORN, 20, ItemID.SWEETCORN_SEED, ItemID.JUTE_FIBRE, ItemID.Cert.JUTE_FIBRE, 10, "Jute fibre"),
    STRAWBERRY(FieldCrop.STRAWBERRY, 31, ItemID.STRAWBERRY_SEED, ItemID.BASKET_APPLE_5, ItemID.Cert.BASKET_APPLE_5, 1, "Apples (full basket of 5)"),
    WATERMELON(FieldCrop.WATERMELON, 47, ItemID.WATERMELON_SEED, ItemID.CURRY_LEAF, ItemID.Cert.CURRY_LEAF, 10, "Curry leaves"),
    SNAPE_GRASS(FieldCrop.SNAPE_GRASS, 61, ItemID.SNAPE_GRASS_SEED, ItemID.JANGERBERRIES, ItemID.Cert.JANGERBERRIES, 5, "Jangerberries"),
    MARIGOLD(FieldCrop.MARIGOLD, 2, ItemID.MARIGOLD_SEED, 0, 0, 0, ""),
    ROSEMARY(FieldCrop.ROSEMARY, 11, ItemID.ROSEMARY_SEED, 0, 0, 0, ""),
    NASTURTIUM(FieldCrop.NASTURTIUM, 24, ItemID.NASTURTIUM_SEED, 0, 0, 0, ""),
    WOAD(FieldCrop.WOAD, 25, ItemID.WOAD_SEED, 0, 0, 0, ""),
    LIMPWURT(FieldCrop.LIMPWURT, 26, ItemID.LIMPWURT_SEED, 0, 0, 0, ""),
    WHITE_LILY(FieldCrop.WHITE_LILY, 58, ItemID.WHITE_LILY_SEED, 0, 0, 0, ""),
    BARLEY(FieldCrop.BARLEY, 3, ItemID.BARLEY_SEED, ItemID.BUCKET_COMPOST, ItemID.Cert.BUCKET_COMPOST, 3, "Compost (farmer payment)"),
    HAMMERSTONE(FieldCrop.HAMMERSTONE, 4, ItemID.HAMMERSTONE_HOP_SEED, ItemID.MARIGOLD, ItemID.Cert.MARIGOLD, 1, "Marigold"),
    ASGARNIAN(FieldCrop.ASGARNIAN, 8, ItemID.ASGARNIAN_HOP_SEED, ItemID.SACK_ONION_10, ItemID.Cert.SACK_ONION_10, 1, "Onions (full sack of 10)"),
    JUTE(FieldCrop.JUTE, 13, ItemID.JUTE_SEED, ItemID.BARLEY_MALT, ItemID.Cert.BARLEY_MALT, 6, "Barley malt"),
    YANILLIAN(FieldCrop.YANILLIAN, 16, ItemID.YANILLIAN_HOP_SEED, ItemID.BASKET_TOMATO_5, ItemID.Cert.BASKET_TOMATO_5, 1, "Tomatoes (full basket of 5)"),
    KRANDORIAN(FieldCrop.KRANDORIAN, 21, ItemID.KRANDORIAN_HOP_SEED, ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10, 3, "Cabbages (full sack of 10)"),
    WILDBLOOD(FieldCrop.WILDBLOOD, 28, ItemID.WILDBLOOD_HOP_SEED, ItemID.NASTURTIUM, ItemID.Cert.NASTURTIUM, 1, "Nasturtium"),
    SEAWEED(FieldCrop.SEAWEED, 23, ItemID.SEAWEED_SEED, ItemID.FOSSIL_NUMULITE, 0, 200, "Numulite (Mernia protection)"),
    MUSHROOM(FieldCrop.MUSHROOM, 53, ItemID.MUSHROOM_SEED, 0, 0, 0, ""),
    BELLADONNA(FieldCrop.BELLADONNA, 63, ItemID.BELLADONNA_SEED, 0, 0, 0, "");
    private final FieldCrop crop;
    private final int level, seedId, paymentId, notedPaymentId, paymentQuantity;
    private final String paymentName;
    FieldRunCrop(FieldCrop crop, int level, int seed, int payment, int noted, int quantity, String name)
    {
        this.crop = crop; this.level = level; this.seedId = seed; this.paymentId = payment;
        this.notedPaymentId = noted; this.paymentQuantity = quantity; this.paymentName = name;
    }
    public FieldCrop getCrop() { return crop; }
    public int getRequiredLevel() { return level; }
    public int getSeedId() { return seedId; }
    public int getSeedsPerPatch()
    {
        switch (crop.getFamily())
        {
            case ALLOTMENT: return 3;
            case HOPS: return this == JUTE ? 3 : 4;
            default: return 1;
        }
    }
    public boolean hasFarmerProtection() { return paymentId != 0; }
    public Optional<SupplyRequirement> protectionPayment(int replants)
    {
        if (!hasFarmerProtection() || replants <= 0) { return Optional.empty(); }
        return Optional.of(new SupplyRequirement(paymentId, paymentName, (long) paymentQuantity * replants, false,
                notedPaymentId == 0 ? "Protection for potential replants; bring the unnoted payment. Pay after planting."
                        : "Farmer protection for potential replants; noted or unnoted payment accepted. Pay after planting.",
                notedPaymentId == 0 ? new int[0] : new int[]{notedPaymentId}));
    }
    @Override public String toString() { return crop.getLabel(); }
}
