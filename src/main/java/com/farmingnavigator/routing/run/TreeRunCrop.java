package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.tree.TreeCrop;
import com.farmingnavigator.loadout.SupplyRequirement;
import java.util.Optional;
import net.runelite.api.gameval.ItemID;

/** Finished saplings, planting levels and farmer payments; unknown payments remain explicit. */
public enum TreeRunCrop
{
    OAK(TreeCrop.OAK, 15, ItemID.PLANTPOT_OAK_SAPLING, ItemID.BASKET_TOMATO_5, ItemID.Cert.BASKET_TOMATO_5, 1, "Tomatoes (full basket of 5)"),
    WILLOW(TreeCrop.WILLOW, 30, ItemID.PLANTPOT_WILLOW_SAPLING, ItemID.BASKET_APPLE_5, ItemID.Cert.BASKET_APPLE_5, 1, "Apples (full basket of 5)"),
    MAPLE(TreeCrop.MAPLE, 45, ItemID.PLANTPOT_MAPLE_SAPLING, ItemID.BASKET_ORANGE_5, ItemID.Cert.BASKET_ORANGE_5, 1, "Oranges (full basket of 5)"),
    YEW(TreeCrop.YEW, 60, ItemID.PLANTPOT_YEW_SAPLING, ItemID.CACTUS_SPINE, ItemID.Cert.CACTUS_SPINE, 10, "Cactus spines"),
    MAGIC(TreeCrop.MAGIC, 75, ItemID.PLANTPOT_MAGIC_TREE_SAPLING, ItemID.COCONUT, ItemID.Cert.COCONUT, 25, "Coconuts"),
    APPLE(TreeCrop.APPLE, 27, ItemID.PLANTPOT_APPLE_SAPLING, ItemID.SWEETCORN, ItemID.Cert.SWEETCORN, 9, "Raw sweetcorn"),
    BANANA(TreeCrop.BANANA, 33, ItemID.PLANTPOT_BANANA_SAPLING, ItemID.BASKET_APPLE_5, ItemID.Cert.BASKET_APPLE_5, 4, "Apples (full basket of 5)"),
    ORANGE(TreeCrop.ORANGE, 39, ItemID.PLANTPOT_ORANGE_SAPLING, ItemID.BASKET_STRAWBERRY_5, ItemID.Cert.BASKET_STRAWBERRY_5, 3, "Strawberries (full basket of 5)"),
    CURRY(TreeCrop.CURRY, 42, ItemID.PLANTPOT_CURRY_SAPLING, ItemID.BASKET_BANANA_5, ItemID.Cert.BASKET_BANANA_5, 5, "Bananas (full basket of 5)"),
    PINEAPPLE(TreeCrop.PINEAPPLE, 51, ItemID.PLANTPOT_PINEAPPLE_SAPLING, ItemID.WATERMELON, ItemID.Cert.WATERMELON, 10, "Watermelons"),
    PAPAYA(TreeCrop.PAPAYA, 57, ItemID.PLANTPOT_PAPAYA_SAPLING, ItemID.PINEAPPLE, ItemID.Cert.PINEAPPLE, 10, "Pineapples"),
    PALM(TreeCrop.PALM, 68, ItemID.PLANTPOT_PALM_SAPLING, ItemID.PAPAYA, ItemID.Cert.PAPAYA, 15, "Papaya fruit"),
    DRAGONFRUIT(TreeCrop.DRAGONFRUIT, 81, ItemID.PLANTPOT_DRAGONFRUIT_SAPLING, ItemID.COCONUT, ItemID.Cert.COCONUT, 15, "Coconuts"),
    TEAK(TreeCrop.TEAK, 35, ItemID.PLANTPOT_TEAK_SAPLING, ItemID.LIMPWURT_ROOT, ItemID.Cert.LIMPWURT_ROOT, 15, "Limpwurt roots"),
    MAHOGANY(TreeCrop.MAHOGANY, 55, ItemID.PLANTPOT_MAHOGANY_SAPLING, ItemID.YANILLIAN_HOPS, ItemID.Cert.YANILLIAN_HOPS, 25, "Yanillian hops"),
    CAMPHOR(TreeCrop.CAMPHOR, 66, ItemID.PLANTPOT_CAMPHOR_SAPLING),
    IRONWOOD(TreeCrop.IRONWOOD, 80, ItemID.PLANTPOT_IRONWOOD_SAPLING),
    ROSEWOOD(TreeCrop.ROSEWOOD, 92, ItemID.PLANTPOT_ROSEWOOD_SAPLING);

    private final TreeCrop crop;
    private final int requiredLevel;
    private final int saplingId;
    private final int paymentId;
    private final int notedPaymentId;
    private final int paymentQuantity;
    private final String paymentName;

    TreeRunCrop(TreeCrop crop, int level, int saplingId)
    {
        this(crop, level, saplingId, 0, 0, 0, "");
    }
    TreeRunCrop(TreeCrop crop, int level, int saplingId, int paymentId, int notedPaymentId, int quantity, String paymentName)
    {
        this.crop = crop;
        this.requiredLevel = level;
        this.saplingId = saplingId;
        this.paymentId = paymentId;
        this.notedPaymentId = notedPaymentId;
        this.paymentQuantity = quantity;
        this.paymentName = paymentName;
    }
    public TreeCrop getCrop() { return crop; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getSaplingId() { return saplingId; }
    public Optional<SupplyRequirement> protectionPayment(int replants)
    {
        if (paymentId == 0 || replants <= 0) { return Optional.empty(); }
        return Optional.of(new SupplyRequirement(paymentId, paymentName, (long) paymentQuantity * replants, false,
                "Farmer protection for potential replants; noted or unnoted accepted. Pay after planting.", notedPaymentId));
    }
    @Override public String toString() { return crop.getLabel(); }
}
