package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.requirement.FairyRingAccessRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RequirementState;
import com.farmingnavigator.routing.requirement.RouteRequirement;
import com.farmingnavigator.routing.requirement.RuneCostRequirement;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.routing.spell.TeleportSpellRequirement;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;

public class TransportVerificationService
{
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
            TravelCapabilitySnapshot travelCapabilitySnapshot,
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
                        || travelCapabilitySnapshot == null
                        || !travelCapabilitySnapshot.isLoggedIn()
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
                                travelCapabilitySnapshot,
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
            TravelCapabilitySnapshot travelCapabilitySnapshot,
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
                        accountState,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case ECTOPHIAL:
                return evaluateEctophial(
                        storageSnapshot,
                        option
                );

            case STONY_BASALT:
                return evaluateBasalt(
                        accountState,
                        storageSnapshot,
                        option,
                        ItemID.STRONGHOLD_TELEPORT_BASALT,
                        "Stony basalt"
                );

            case ICY_BASALT:
                return evaluateBasalt(
                        accountState,
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

            case FAIRY_RING_ACCESS:
                return evaluateFairyRingAccess(
                        accountState,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case TELEPORT_SPELL:
                return evaluateTeleportSpell(
                        accountState,
                        travelCapabilitySnapshot,
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
            AccountState accountState,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        null,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                ChargedTeleportItemDefinition
                        .SKILLS_NECKLACE
                        .createRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateFairyRingAccess(
            AccountState accountState,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        null,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        /*
         * FairyRingAccessRequirement has two definitive
         * BLOCKED outcomes:
         *
         * 1. The Fairy Godfather unlock has not happened.
         *    That is a real account lock.
         *
         * 2. The network is unlocked, but the account lacks
         *    a usable Dramen/Lunar staff and does not have the
         *    claimed Lumbridge & Draynor Elite exemption.
         *    That is a missing preparation resource.
         */
        VerifiedTransportState blockedState =
                accountState
                        .isFairyRingAccessUnlocked()
                        ? VerifiedTransportState.MISSING
                        : VerifiedTransportState.LOCKED;

        return new VerifiedTransportResult(
                option,
                blockedState,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateTeleportSpell(
            AccountState accountState,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        TeleportSpellDefinition spellDefinition =
                option.getTeleportSpellDefinition();

        if (spellDefinition == null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    "DATA REQUIRED - teleport spell definition is missing."
            );
        }

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        null,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        TeleportSpellRequirement requirement =
                spellDefinition
                        .createRequirement();

        RequirementResult requirementResult =
                requirement.evaluate(
                        context
                );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - "
                            + spellDefinition
                            .getDisplayName()
                            + " requirements are satisfied."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                classifyBlockedSpellRequirement(
                        requirement,
                        context
                ),
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportState
    classifyBlockedSpellRequirement(
            TeleportSpellRequirement requirement,
            RequirementContext context
    )
    {
        boolean hasLockedRequirement =
                false;

        boolean hasMissingRunes =
                false;

        boolean hasDataRequired =
                false;

        boolean hasConfigurationRequired =
                false;

        for (
                RouteRequirement component
                : requirement
                .getComponentRequirements()
        )
        {
            RequirementResult componentResult =
                    component.evaluate(
                            context
                    );

            if (
                    componentResult
                            .isSatisfied()
            )
            {
                continue;
            }

            if (
                    componentResult.getState()
                            == RequirementState.BLOCKED
            )
            {
                if (
                        component
                                instanceof RuneCostRequirement
                )
                {
                    hasMissingRunes =
                            true;
                }
                else
                {
                    hasLockedRequirement =
                            true;
                }

                continue;
            }

            if (
                    componentResult.getState()
                            == RequirementState.DATA_REQUIRED
            )
            {
                hasDataRequired =
                        true;

                continue;
            }

            if (
                    componentResult.getState()
                            == RequirementState.CONFIGURATION_REQUIRED
            )
            {
                hasConfigurationRequired =
                        true;
            }
        }

        if (hasLockedRequirement)
        {
            return VerifiedTransportState.LOCKED;
        }

        if (hasMissingRunes)
        {
            return VerifiedTransportState.MISSING;
        }

        if (hasDataRequired)
        {
            return VerifiedTransportState.DATA_REQUIRED;
        }

        if (hasConfigurationRequired)
        {
            return VerifiedTransportState.CONFIGURATION_REQUIRED;
        }

        return VerifiedTransportState.LOCKED;
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
    evaluateBasalt(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            TransportOption option,
            int itemId,
            String itemName
    )
    {
        if (
                !accountState.hasCompletedQuest(
                        Quest.MAKING_FRIENDS_WITH_MY_ARM
                )
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    "LOCKED - requires completion of "
                            + Quest.MAKING_FRIENDS_WITH_MY_ARM
                            .getName()
                            + " before "
                            + itemName
                            + " can be used."
            );
        }

        return evaluateStackItem(
                storageSnapshot,
                option,
                itemId,
                itemName
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

            case DATA_REQUIRED:
                return 2;

            case CONFIGURATION_REQUIRED:
                return 3;

            case MISSING:
                return 4;

            case LOCKED:
            default:
                return 5;
        }
    }
}