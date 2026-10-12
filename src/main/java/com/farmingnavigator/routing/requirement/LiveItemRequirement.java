package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class LiveItemRequirement
        implements RouteRequirement
{
    private final String displayName;

    private final Set<Integer> itemIds;

    private final int requiredQuantity;

    private final LiveItemLocation location;

    public LiveItemRequirement(
            String displayName,
            int itemId,
            int requiredQuantity,
            LiveItemLocation location
    )
    {
        this(
                displayName,
                Collections.singleton(
                        itemId
                ),
                requiredQuantity,
                location
        );
    }

    public LiveItemRequirement(
            String displayName,
            Set<Integer> itemIds,
            int requiredQuantity,
            LiveItemLocation location
    )
    {
        this.displayName =
                displayName;

        this.requiredQuantity =
                Math.max(
                        1,
                        requiredQuantity
                );

        this.location =
                location == null
                        ? LiveItemLocation
                        .INVENTORY_OR_EQUIPMENT
                        : location;

        LinkedHashSet<Integer> validItemIds =
                new LinkedHashSet<>();

        if (itemIds != null)
        {
            for (
                    Integer itemId
                    : itemIds
            )
            {
                if (
                        itemId != null
                                && itemId > 0
                )
                {
                    validItemIds.add(
                            itemId
                    );
                }
            }
        }

        this.itemIds =
                Collections.unmodifiableSet(
                        validItemIds
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
                    "Log in so Farming Navigator can verify your "
                            + location.getDisplayName()
                            + "."
            );
        }

        if (itemIds.isEmpty())
        {
            return RequirementResult.dataRequired(
                    displayName,
                    "No valid item IDs were configured for this requirement."
            );
        }

        TravelCapabilitySnapshot snapshot =
                context
                        .getTravelCapabilitySnapshot();

        int quantity =
                getAvailableQuantity(
                        snapshot
                );

        if (
                quantity >= requiredQuantity
        )
        {
            return RequirementResult.satisfied(
                    displayName,
                    "Found "
                            + quantity
                            + " in "
                            + location.getDisplayName()
                            + "."
            );
        }

        return RequirementResult.blocked(
                displayName,
                "Requires "
                        + requiredQuantity
                        + " in "
                        + location.getDisplayName()
                        + ". Verified quantity: "
                        + quantity
                        + "."
        );
    }

    private int getAvailableQuantity(
            TravelCapabilitySnapshot snapshot
    )
    {
        int quantity = 0;

        for (
                Integer itemId
                : itemIds
        )
        {
            switch (location)
            {
                case INVENTORY:
                    quantity +=
                            snapshot
                                    .getInventoryQuantity(
                                            itemId
                                    );
                    break;

                case EQUIPMENT:
                    quantity +=
                            snapshot
                                    .getEquipmentQuantity(
                                            itemId
                                    );
                    break;

                case INVENTORY_OR_EQUIPMENT:
                default:
                    quantity +=
                            snapshot
                                    .getCarriedQuantity(
                                            itemId
                                    );
                    break;
            }
        }

        return quantity;
    }
}