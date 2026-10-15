package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.garden.GardenCrop;
import com.farmingnavigator.loadout.SupplyRequirement;
import java.util.Optional;
import net.runelite.api.gameval.ItemID;

/** One seed per new bush/cactus. Farmer payments use the exact full container or produce requested. */
public enum GardenRunCrop
{
    REDBERRIES(GardenCrop.REDBERRIES, 10, ItemID.REDBERRY_BUSH_SEED, ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10, 4, "Cabbages (full sack of 10)"),
    CADAVABERRIES(GardenCrop.CADAVABERRIES, 22, ItemID.CADAVABERRY_BUSH_SEED, ItemID.BASKET_TOMATO_5, ItemID.Cert.BASKET_TOMATO_5, 3, "Tomatoes (full basket of 5)"),
    DWELLBERRIES(GardenCrop.DWELLBERRIES, 36, ItemID.DWELLBERRY_BUSH_SEED, ItemID.BASKET_STRAWBERRY_5, ItemID.Cert.BASKET_STRAWBERRY_5, 3, "Strawberries (full basket of 5)"),
    JANGERBERRIES(GardenCrop.JANGERBERRIES, 48, ItemID.JANGERBERRY_BUSH_SEED, ItemID.WATERMELON, ItemID.Cert.WATERMELON, 6, "Watermelons"),
    WHITEBERRIES(GardenCrop.WHITEBERRIES, 59, ItemID.WHITEBERRY_BUSH_SEED, ItemID.BITTERCAP_MUSHROOM, ItemID.Cert.BITTERCAP_MUSHROOM, 8, "Bittercap mushrooms"),
    POISON_IVY(GardenCrop.POISON_IVY, 70, ItemID.POISONIVY_BUSH_SEED, 0, 0, 0, ""),
    CACTUS(GardenCrop.CACTUS, 55, ItemID.CACTUS_SEED, ItemID.CADAVABERRIES, ItemID.Cert.CADAVABERRIES, 6, "Cadava berries"),
    POTATO_CACTUS(GardenCrop.POTATO_CACTUS, 64, ItemID.POTATO_CACTUS_SEED, ItemID.SNAPE_GRASS, ItemID.Cert.SNAPE_GRASS, 8, "Snape grass");
    private final GardenCrop crop;
    private final int level, seedId, paymentId, notedPaymentId, paymentQuantity;
    private final String paymentName;
    GardenRunCrop(GardenCrop crop, int level, int seed, int payment, int noted, int quantity, String name)
    {
        this.crop = crop; this.level = level; this.seedId = seed; this.paymentId = payment;
        this.notedPaymentId = noted; this.paymentQuantity = quantity; this.paymentName = name;
    }
    public GardenCrop getCrop() { return crop; }
    public int getRequiredLevel() { return level; }
    public int getSeedId() { return seedId; }
    public boolean hasFarmerProtection() { return paymentId != 0; }
    public Optional<SupplyRequirement> protectionPayment(int replants)
    {
        if (!hasFarmerProtection() || replants <= 0) { return Optional.empty(); }
        return Optional.of(new SupplyRequirement(paymentId, paymentName, (long) paymentQuantity * replants, false,
                "Farmer protection for potential replants; noted or unnoted payment accepted. Pay after planting.", notedPaymentId));
    }
    @Override public String toString() { return crop.getLabel(); }
}
