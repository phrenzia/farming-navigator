package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.gameval.ItemID;

public class TransportVerificationService
{
    private static final int[] SKILLS_NECKLACE_IDS = {
            ItemID.JEWL_NECKLACE_OF_SKILLS_6,
            ItemID.JEWL_NECKLACE_OF_SKILLS_5,
            ItemID.JEWL_NECKLACE_OF_SKILLS_4,
            ItemID.JEWL_NECKLACE_OF_SKILLS_3,
            ItemID.JEWL_NECKLACE_OF_SKILLS_2,
            ItemID.JEWL_NECKLACE_OF_SKILLS_1
    };

    private static final int[] SKILLS_NECKLACE_CHARGES = {
            6,
            5,
            4,
            3,
            2,
            1
    };

    private static final int[] UNLIMITED_EXPLORER_RING_IDS = {
            ItemID.LUMBRIDGE_RING_ELITE,
            ItemID.LUMBRIDGE_RING_HARD
    };

    private static final int[] FARMING_CAPE_IDS = {
            ItemID.SKILLCAPE_FARMING,
            ItemID.SKILLCAPE_FARMING_TRIMMED,
            ItemID.SKILLCAPE_MAX_WORN
    };

    @Inject
    public TransportVerificationService()
    {
    }

    public Map<
            HerbPatchDefinition,
            List<VerifiedTransportResult>
            > evaluateAll(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            Map<
                    HerbPatchDefinition,
                    List<TransportOption>
                    > optionsByPatch
    )
    {
        if (
                accountState == null
                        || !accountState.isLoggedIn()
                        || storageSnapshot == null
                        || !storageSnapshot.isBankVerified()
        )
        {
            return Collections.emptyMap();
        }

        EnumMap<
                HerbPatchDefinition,
                List<VerifiedTransportResult>
                > results =
                new EnumMap<>(
                        HerbPatchDefinition.class
                );

        for (
                Map.Entry<
                        HerbPatchDefinition,
                        List<TransportOption>
                        > entry
                : optionsByPatch.entrySet()
        )
        {
            List<VerifiedTransportResult>
                    patchResults =
                    new ArrayList<>();

            for (
                    TransportOption option
                    : entry.getValue()
            )
            {
                VerifiedTransportResult result =
                        evaluateOption(
                                accountState,
                                storageSnapshot,
                                option
                        );

                if (result != null)
                {
                    patchResults.add(result);
                }
            }

            patchResults.sort(
                    Comparator
                            .comparingInt(
                                    this::statePriority
                            )
                            .thenComparingInt(
                                    result ->
                                            result
                                                    .getOption()
                                                    .getBaselineRank()
                            )
            );

            results.put(
                    entry.getKey(),
                    Collections.unmodifiableList(
                            patchResults
                    )
            );
        }

        return Collections.unmodifiableMap(
                results
        );
    }

    private VerifiedTransportResult evaluateOption(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        switch (
                option.getVerificationKind()
        )
        {
            case EXPLORER_RING_UNLIMITED:
                return evaluateExplorerRingUnlimited(
                        storageSnapshot,
                        option
                );

            case SKILLS_NECKLACE:
                return evaluateSkillsNecklace(
                        storageSnapshot,
                        option
                );

            case ECTOPHIAL:
                return evaluateEctophial(
                        storageSnapshot,
                        option
                );

            case STONY_BASALT:
                return evaluateStackItem(
                        storageSnapshot,
                        option,
                        ItemID.STRONGHOLD_TELEPORT_BASALT,
                        "Stony basalt"
                );

            case ICY_BASALT:
                return evaluateStackItem(
                        storageSnapshot,
                        option,
                        ItemID.WEISS_TELEPORT_BASALT,
                        "Icy basalt"
                );

            case FARMING_CAPE:
                return evaluateFarmingCape(
                        accountState,
                        storageSnapshot,
                        option
                );

            case NOT_IMPLEMENTED:
            default:
                /*
                 * Deliberately excluded.
                 *
                 * A transport is not allowed into verified
                 * ranking until we can prove every requirement.
                 */
                return null;
        }
    }

    private VerifiedTransportResult
    evaluateExplorerRingUnlimited(
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        String carriedVariant =
                findFirstCarriedVariant(
                        storageSnapshot,
                        UNLIMITED_EXPLORER_RING_IDS,
                        new String[] {
                                "Explorer's ring 4",
                                "Explorer's ring 3"
                        }
                );

        if (carriedVariant != null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    carriedVariant
                            + " carried - Cabbage-port is unlimited"
            );
        }

        String bankVariant =
                findFirstBankVariant(
                        storageSnapshot,
                        UNLIMITED_EXPLORER_RING_IDS,
                        new String[] {
                                "Explorer's ring 4",
                                "Explorer's ring 3"
                        }
                );

        if (bankVariant != null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    bankVariant
                            + " in bank - unlimited Cabbage-port; withdraw before run"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "No Explorer's ring 3 or 4 found in inventory, equipment, or verified bank"
        );
    }

    private VerifiedTransportResult
    evaluateSkillsNecklace(
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        long carriedCharges =
                calculateSkillsNecklaceCharges(
                        storageSnapshot,
                        false
                );

        long bankCharges =
                calculateSkillsNecklaceCharges(
                        storageSnapshot,
                        true
                );

        String bestCarriedVariant =
                findBestSkillsNecklaceVariant(
                        storageSnapshot,
                        false
                );

        String bestBankVariant =
                findBestSkillsNecklaceVariant(
                        storageSnapshot,
                        true
                );

        if (carriedCharges > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    bestCarriedVariant
                            + " ready - "
                            + carriedCharges
                            + " total carried teleport charge"
                            + pluralSuffix(carriedCharges)
            );
        }

        if (bankCharges > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    bestBankVariant
                            + " available in bank - "
                            + bankCharges
                            + " total banked teleport charge"
                            + pluralSuffix(bankCharges)
                            + "; withdraw one"
            );
        }

        long unchargedCount =
                storageSnapshot
                        .getInventoryQuantity(
                                ItemID.JEWL_NECKLACE_OF_SKILLS
                        )
                        + storageSnapshot
                        .getEquipmentQuantity(
                                ItemID.JEWL_NECKLACE_OF_SKILLS
                        )
                        + storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.JEWL_NECKLACE_OF_SKILLS
                        );

        if (unchargedCount > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.MISSING,
                    unchargedCount
                            + " uncharged Skills necklace"
                            + pluralSuffix(unchargedCount)
                            + " found, but no necklace with teleport charges"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "No charged Skills necklace found in inventory, equipment, or verified bank"
        );
    }

    private VerifiedTransportResult
    evaluateEctophial(
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        long carriedFull =
                storageSnapshot.getCarriedQuantity(
                        ItemID.ECTOPHIAL
                );

        long bankFull =
                storageSnapshot.getVerifiedBankQuantity(
                        ItemID.ECTOPHIAL
                );

        long emptyTotal =
                storageSnapshot.getCarriedQuantity(
                        ItemID.ECTOPHIAL_EMPTY
                )
                        + storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.ECTOPHIAL_EMPTY
                        );

        if (carriedFull > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "Filled Ectophial carried - ready now"
            );
        }

        if (bankFull > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    "Filled Ectophial in bank - withdraw before run"
            );
        }

        if (emptyTotal > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.MISSING,
                    "Only empty Ectophial found - refill it before this transport is usable"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "No filled Ectophial found in inventory, equipment, or verified bank"
        );
    }

    private VerifiedTransportResult
    evaluateStackItem(
            StorageSnapshot storageSnapshot,
            TransportOption option,
            int itemId,
            String itemName
    )
    {
        long inventoryQuantity =
                storageSnapshot.getInventoryQuantity(
                        itemId
                );

        long bankQuantity =
                storageSnapshot
                        .getVerifiedBankQuantity(
                                itemId
                        );

        if (inventoryQuantity > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    itemName
                            + ": "
                            + inventoryQuantity
                            + " in inventory, "
                            + bankQuantity
                            + " in bank"
            );
        }

        if (bankQuantity > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    itemName
                            + ": "
                            + bankQuantity
                            + " in bank - withdraw 1 before run"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "No "
                        + itemName
                        + " found in inventory or verified bank"
        );
    }

    private VerifiedTransportResult
    evaluateFarmingCape(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        if (
                accountState.getRealFarmingLevel()
                        < 99
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    "Requires 99 Farming"
            );
        }

        String carriedVariant =
                findFirstCarriedVariant(
                        storageSnapshot,
                        FARMING_CAPE_IDS,
                        new String[] {
                                "Farming cape",
                                "Farming cape(t)",
                                "Max cape"
                        }
                );

        if (carriedVariant != null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    carriedVariant
                            + " carried - Farming Guild teleport ready"
            );
        }

        String bankVariant =
                findFirstBankVariant(
                        storageSnapshot,
                        FARMING_CAPE_IDS,
                        new String[] {
                                "Farming cape",
                                "Farming cape(t)",
                                "Max cape"
                        }
                );

        if (bankVariant != null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    bankVariant
                            + " in bank - withdraw before run"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "99 Farming confirmed, but no supported Farming cape or Max cape found"
        );
    }

    private long calculateSkillsNecklaceCharges(
            StorageSnapshot storageSnapshot,
            boolean bankOnly
    )
    {
        long totalCharges = 0;

        for (
                int index = 0;
                index < SKILLS_NECKLACE_IDS.length;
                index++
        )
        {
            int itemId =
                    SKILLS_NECKLACE_IDS[index];

            int charges =
                    SKILLS_NECKLACE_CHARGES[index];

            long quantity;

            if (bankOnly)
            {
                quantity =
                        storageSnapshot
                                .getVerifiedBankQuantity(
                                        itemId
                                );
            }
            else
            {
                quantity =
                        storageSnapshot
                                .getCarriedQuantity(
                                        itemId
                                );
            }

            totalCharges +=
                    quantity * charges;
        }

        return totalCharges;
    }

    private String findBestSkillsNecklaceVariant(
            StorageSnapshot storageSnapshot,
            boolean bankOnly
    )
    {
        for (
                int index = 0;
                index < SKILLS_NECKLACE_IDS.length;
                index++
        )
        {
            int itemId =
                    SKILLS_NECKLACE_IDS[index];

            long quantity;

            if (bankOnly)
            {
                quantity =
                        storageSnapshot
                                .getVerifiedBankQuantity(
                                        itemId
                                );
            }
            else
            {
                quantity =
                        storageSnapshot
                                .getCarriedQuantity(
                                        itemId
                                );
            }

            if (quantity > 0)
            {
                return "Skills necklace("
                        + SKILLS_NECKLACE_CHARGES[index]
                        + ")";
            }
        }

        return "Skills necklace";
    }

    private String findFirstCarriedVariant(
            StorageSnapshot storageSnapshot,
            int[] itemIds,
            String[] names
    )
    {
        for (
                int index = 0;
                index < itemIds.length;
                index++
        )
        {
            if (
                    storageSnapshot
                            .getCarriedQuantity(
                                    itemIds[index]
                            ) > 0
            )
            {
                return names[index];
            }
        }

        return null;
    }

    private String findFirstBankVariant(
            StorageSnapshot storageSnapshot,
            int[] itemIds,
            String[] names
    )
    {
        for (
                int index = 0;
                index < itemIds.length;
                index++
        )
        {
            if (
                    storageSnapshot
                            .getVerifiedBankQuantity(
                                    itemIds[index]
                            ) > 0
            )
            {
                return names[index];
            }
        }

        return null;
    }

    private String pluralSuffix(
            long quantity
    )
    {
        return quantity == 1
                ? ""
                : "s";
    }

    private int statePriority(
            VerifiedTransportResult result
    )
    {
        switch (result.getState())
        {
            case READY_NOW:
                return 0;

            case AVAILABLE_FROM_BANK:
                return 1;

            case MISSING:
                return 2;

            case LOCKED:
            default:
                return 3;
        }
    }
}