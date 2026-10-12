package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class BankBackedItemRequirement
        implements RouteRequirement
{
    private final String displayName;

    private final Map<Integer, String>
            itemVariants;

    private final long requiredQuantity;

    public BankBackedItemRequirement(
            String displayName,
            int itemId,
            String itemName,
            long requiredQuantity
    )
    {
        this(
                displayName,
                createSingleVariant(
                        itemId,
                        itemName
                ),
                requiredQuantity
        );
    }

    public BankBackedItemRequirement(
            String displayName,
            Map<Integer, String> itemVariants,
            long requiredQuantity
    )
    {
        this.displayName =
                displayName;

        this.requiredQuantity =
                Math.max(
                        1L,
                        requiredQuantity
                );

        LinkedHashMap<Integer, String>
                validVariants =
                new LinkedHashMap<>();

        if (itemVariants != null)
        {
            for (
                    Map.Entry<Integer, String> entry
                    : itemVariants.entrySet()
            )
            {
                Integer itemId =
                        entry.getKey();

                String itemName =
                        entry.getValue();

                if (
                        itemId == null
                                || itemId <= 0
                )
                {
                    continue;
                }

                if (
                        itemName == null
                                || itemName.trim().isEmpty()
                )
                {
                    itemName =
                            "Item "
                                    + itemId;
                }

                validVariants.put(
                        itemId,
                        itemName
                );
            }
        }

        this.itemVariants =
                Collections.unmodifiableMap(
                        validVariants
                );
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        if (
                context == null
                        || context
                        .getTravelCapabilitySnapshot()
                        == null
                        || !context
                        .getTravelCapabilitySnapshot()
                        .isLoggedIn()
        )
        {
            return RequirementResult.dataRequired(
                    displayName,
                    "Log in so Farming Navigator can verify your carried items."
            );
        }

        if (itemVariants.isEmpty())
        {
            return RequirementResult.dataRequired(
                    displayName,
                    "No valid item variants were configured for this requirement."
            );
        }

        TravelCapabilitySnapshot liveSnapshot =
                context
                        .getTravelCapabilitySnapshot();

        long carriedQuantity =
                getCarriedQuantity(
                        liveSnapshot
                );

        if (
                carriedQuantity
                        >= requiredQuantity
        )
        {
            return RequirementResult.satisfied(
                    displayName,
                    "READY NOW - "
                            + describeCarriedVariants(
                            liveSnapshot
                    )
            );
        }

        StorageSnapshot storageSnapshot =
                context.getStorageSnapshot();

        if (
                storageSnapshot == null
                        || !storageSnapshot
                        .isBankVerified()
        )
        {
            return RequirementResult.dataRequired(
                    displayName,
                    "Bank verification required. "
                            + "Current carried quantity: "
                            + carriedQuantity
                            + ". Open any normal bank once so Farming Navigator can verify the rest."
            );
        }

        long bankQuantity =
                getBankQuantity(
                        storageSnapshot
                );

        long totalVerifiedQuantity =
                carriedQuantity
                        + bankQuantity;

        if (
                totalVerifiedQuantity
                        >= requiredQuantity
        )
        {
            long quantityToWithdraw =
                    requiredQuantity
                            - carriedQuantity;

            return RequirementResult.availableFromBank(
                    displayName,
                    "AVAILABLE FROM BANK - withdraw "
                            + quantityToWithdraw
                            + ". Verified bank: "
                            + describeBankVariants(
                            storageSnapshot
                    )
            );
        }

        if (
                totalVerifiedQuantity == 0
        )
        {
            return RequirementResult.blocked(
                    displayName,
                    "MISSING - no supported "
                            + displayName
                            + " found in inventory, equipment, or verified bank."
            );
        }

        long missingQuantity =
                requiredQuantity
                        - totalVerifiedQuantity;

        return RequirementResult.blocked(
                displayName,
                "MISSING - requires "
                        + requiredQuantity
                        + " total. Carried: "
                        + carriedQuantity
                        + ". Verified bank: "
                        + bankQuantity
                        + ". Still missing: "
                        + missingQuantity
                        + "."
        );
    }

    private long getCarriedQuantity(
            TravelCapabilitySnapshot snapshot
    )
    {
        long quantity = 0;

        for (
                Integer itemId
                : itemVariants.keySet()
        )
        {
            quantity +=
                    snapshot.getCarriedQuantity(
                            itemId
                    );
        }

        return quantity;
    }

    private long getBankQuantity(
            StorageSnapshot snapshot
    )
    {
        long quantity = 0;

        for (
                Integer itemId
                : itemVariants.keySet()
        )
        {
            quantity +=
                    snapshot
                            .getVerifiedBankQuantity(
                                    itemId
                            );
        }

        return quantity;
    }

    private String describeCarriedVariants(
            TravelCapabilitySnapshot snapshot
    )
    {
        StringJoiner joiner =
                new StringJoiner(
                        ", "
                );

        for (
                Map.Entry<Integer, String> entry
                : itemVariants.entrySet()
        )
        {
            int quantity =
                    snapshot.getCarriedQuantity(
                            entry.getKey()
                    );

            if (quantity <= 0)
            {
                continue;
            }

            joiner.add(
                    entry.getValue()
                            + " x"
                            + quantity
            );
        }

        if (joiner.length() == 0)
        {
            return "none carried";
        }

        return joiner.toString();
    }

    private String describeBankVariants(
            StorageSnapshot snapshot
    )
    {
        StringJoiner joiner =
                new StringJoiner(
                        ", "
                );

        for (
                Map.Entry<Integer, String> entry
                : itemVariants.entrySet()
        )
        {
            long quantity =
                    snapshot
                            .getVerifiedBankQuantity(
                                    entry.getKey()
                            );

            if (quantity <= 0)
            {
                continue;
            }

            joiner.add(
                    entry.getValue()
                            + " x"
                            + quantity
            );
        }

        if (joiner.length() == 0)
        {
            return "none";
        }

        return joiner.toString();
    }

    private static Map<Integer, String>
    createSingleVariant(
            int itemId,
            String itemName
    )
    {
        LinkedHashMap<Integer, String> variants =
                new LinkedHashMap<>();

        variants.put(
                itemId,
                itemName
        );

        return variants;
    }
}