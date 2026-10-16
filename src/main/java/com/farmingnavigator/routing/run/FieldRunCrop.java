package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.field.FieldCrop;
import com.farmingnavigator.loadout.SupplyRequirement;
import java.util.Optional;
import net.runelite.api.gameval.ItemID;

/** Three seeds per allotment and one per flower. Farmer payments use the exact full container or produce requested. */
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
    WHITE_LILY(FieldCrop.WHITE_LILY, 58, ItemID.WHITE_LILY_SEED, 0, 0, 0, "");
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
    public int getSeedsPerPatch() { return crop.getFamily() == com.farmingnavigator.farming.field.FieldFamily.ALLOTMENT ? 3 : 1; }
    public boolean hasFarmerProtection() { return paymentId != 0; }
    public Optional<SupplyRequirement> protectionPayment(int replants)
    {
        if (!hasFarmerProtection() || replants <= 0) { return Optional.empty(); }
        return Optional.of(new SupplyRequirement(paymentId, paymentName, (long) paymentQuantity * replants, false,
                "Farmer protection for potential replants; noted or unnoted payment accepted. Pay after planting.", notedPaymentId));
    }
    @Override public String toString() { return crop.getLabel(); }
}
