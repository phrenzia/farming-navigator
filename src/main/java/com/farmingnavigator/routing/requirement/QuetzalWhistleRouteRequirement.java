package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import net.runelite.api.gameval.ItemID;

public class QuetzalWhistleRouteRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Quetzal whistle to Hunter Guild";

    private static final int[] FINITE_WHISTLE_IDS = {
            ItemID.HG_QUETZALWHISTLE_BASIC,
            ItemID.HG_QUETZALWHISTLE_ENHANCED,
            ItemID.HG_QUETZALWHISTLE_PERFECTED
    };

    private static final int UNLIMITED_WHISTLE_ID =
            ItemID.HG_QUETZALWHISTLE_PERFECTED_INFINITE;

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
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
                    DISPLAY_NAME,
                    "DATA REQUIRED - log in so Farming Navigator can verify your Quetzal whistle."
            );
        }

        TravelCapabilitySnapshot travelSnapshot =
                context.getTravelCapabilitySnapshot();

        if (
                travelSnapshot.hasInInventory(
                        UNLIMITED_WHISTLE_ID
                )
        )
        {
            return RequirementResult.satisfied(
                    DISPLAY_NAME,
                    "READY NOW - Perfected quetzal whistle(i) is carried and has unlimited charges."
            );
        }

        StorageSnapshot storageSnapshot =
                context.getStorageSnapshot();

        if (
                storageSnapshot != null
                        && storageSnapshot
                        .isBankVerified()
                        && storageSnapshot
                        .getVerifiedBankQuantity(
                                UNLIMITED_WHISTLE_ID
                        ) > 0L
        )
        {
            return RequirementResult.availableFromBank(
                    DISPLAY_NAME,
                    "AVAILABLE FROM BANK - withdraw the Perfected quetzal whistle(i); it has unlimited charges."
            );
        }

        if (
                hasFiniteWhistleInInventory(
                        travelSnapshot
                )
        )
        {
            return RequirementResult.dataRequired(
                    DISPLAY_NAME,
                    "DATA REQUIRED - a finite-charge Quetzal whistle is carried, but Farming Navigator cannot yet verify its remaining charges."
            );
        }

        if (
                storageSnapshot == null
                        || !storageSnapshot
                        .isBankVerified()
        )
        {
            return RequirementResult.dataRequired(
                    DISPLAY_NAME,
                    "DATA REQUIRED - open any normal bank once so Farming Navigator can check for a Quetzal whistle."
            );
        }

        if (
                hasFiniteWhistleInBank(
                        storageSnapshot
                )
        )
        {
            return RequirementResult.dataRequired(
                    DISPLAY_NAME,
                    "DATA REQUIRED - a finite-charge Quetzal whistle is in the verified bank, but Farming Navigator cannot yet verify its remaining charges."
            );
        }

        return RequirementResult.blocked(
                DISPLAY_NAME,
                "MISSING - no supported Quetzal whistle was found in inventory or the verified bank."
        );
    }

    private boolean hasFiniteWhistleInInventory(
            TravelCapabilitySnapshot travelSnapshot
    )
    {
        for (int itemId : FINITE_WHISTLE_IDS)
        {
            if (
                    travelSnapshot.hasInInventory(
                            itemId
                    )
            )
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasFiniteWhistleInBank(
            StorageSnapshot storageSnapshot
    )
    {
        for (int itemId : FINITE_WHISTLE_IDS)
        {
            if (
                    storageSnapshot
                            .getVerifiedBankQuantity(
                                    itemId
                            ) > 0L
            )
            {
                return true;
            }
        }

        return false;
    }
}