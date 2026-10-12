package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import net.runelite.api.gameval.ItemID;

public class FairyRingAccessRequirement
        implements RouteRequirement
{
    @Override
    public String getDisplayName()
    {
        return "Fairy ring access";
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        if (
                context == null
                        || context.getAccountState() == null
                        || !context
                        .getAccountState()
                        .isLoggedIn()
                        || context
                        .getTravelCapabilitySnapshot()
                        == null
                        || !context
                        .getTravelCapabilitySnapshot()
                        .isLoggedIn()
        )
        {
            return RequirementResult.dataRequired(
                    getDisplayName(),
                    "DATA REQUIRED - log in so Farming Navigator can verify fairy-ring access."
            );
        }

        AccountState accountState =
                context.getAccountState();

        if (
                !accountState
                        .isFairyRingAccessUnlocked()
        )
        {
            return RequirementResult.blocked(
                    getDisplayName(),
                    "LOCKED - fairy-ring access requires Fairytale II - Cure a Queen progress through the Fairy Godfather conversation."
            );
        }

        /*
         * The Lumbridge & Draynor Elite diary reward removes
         * the need to wield a Dramen or Lunar staff.
         *
         * Use the claimed-reward state rather than merely the
         * task-completion state so Farming Navigator does not
         * grant an unclaimed diary perk.
         */
        if (
                accountState
                        .hasClaimedDiaryTier(
                                DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                                DiaryTier.ELITE
                        )
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    "READY NOW - fairy-ring access is unlocked and the claimed Lumbridge & Draynor Elite reward removes the staff requirement."
            );
        }

        TravelCapabilitySnapshot liveSnapshot =
                context
                        .getTravelCapabilitySnapshot();

        if (
                liveSnapshot.isEquipped(
                        ItemID.DRAMEN_STAFF
                )
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    "READY NOW - Dramen staff is equipped."
            );
        }

        if (
                liveSnapshot.isEquipped(
                        ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF
                )
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    "READY NOW - Lunar staff is equipped."
            );
        }

        if (
                liveSnapshot.hasInInventory(
                        ItemID.DRAMEN_STAFF
                )
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    "READY NOW - equip the Dramen staff from inventory before using the fairy ring."
            );
        }

        if (
                liveSnapshot.hasInInventory(
                        ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF
                )
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    "READY NOW - equip the Lunar staff from inventory before using the fairy ring."
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
                    getDisplayName(),
                    "DATA REQUIRED - fairy-ring access is unlocked, but no usable staff is carried. Open any normal bank once so Farming Navigator can check for a Dramen or Lunar staff."
            );
        }

        long bankedDramenStaves =
                storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.DRAMEN_STAFF
                        );

        if (bankedDramenStaves > 0L)
        {
            return RequirementResult.availableFromBank(
                    getDisplayName(),
                    "AVAILABLE FROM BANK - withdraw and equip a Dramen staff before using the fairy ring."
            );
        }

        long bankedLunarStaves =
                storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF
                        );

        if (bankedLunarStaves > 0L)
        {
            return RequirementResult.availableFromBank(
                    getDisplayName(),
                    "AVAILABLE FROM BANK - withdraw and equip a Lunar staff before using the fairy ring."
            );
        }

        return RequirementResult.blocked(
                getDisplayName(),
                "MISSING - fairy-ring access is unlocked, but no Dramen or Lunar staff was found and the Lumbridge & Draynor Elite reward is not claimed."
        );
    }
}