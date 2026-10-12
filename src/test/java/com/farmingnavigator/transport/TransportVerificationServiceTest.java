package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransportVerificationServiceTest
{
    @Test
    public void stonyBasaltIsLockedBeforeQuestEvenWhenCarried()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.NOT_STARTED,
                        false
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.STRONGHOLD_TELEPORT_BASALT,
                        1L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        storageSnapshot,
                        HerbPatchDefinition.TROLL_STRONGHOLD,
                        TransportVerificationKind.STONY_BASALT,
                        "Stony basalt"
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Making Friends with My Arm"
                        )
        );
    }

    @Test
    public void stonyBasaltIsLockedWhileQuestIsInProgress()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.IN_PROGRESS,
                        false
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.STRONGHOLD_TELEPORT_BASALT,
                        1L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        storageSnapshot,
                        HerbPatchDefinition.TROLL_STRONGHOLD,
                        TransportVerificationKind.STONY_BASALT,
                        "Stony basalt"
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );
    }

    @Test
    public void stonyBasaltIsReadyNowAfterQuest()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.FINISHED,
                        false
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.STRONGHOLD_TELEPORT_BASALT,
                        3L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        storageSnapshot,
                        HerbPatchDefinition.TROLL_STRONGHOLD,
                        TransportVerificationKind.STONY_BASALT,
                        "Stony basalt"
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "3 in inventory"
                        )
        );
    }

    @Test
    public void stonyBasaltCanBeAvailableFromBankAfterQuest()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.FINISHED,
                        false
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.STRONGHOLD_TELEPORT_BASALT,
                        0L,
                        4L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        storageSnapshot,
                        HerbPatchDefinition.TROLL_STRONGHOLD,
                        TransportVerificationKind.STONY_BASALT,
                        "Stony basalt"
                );

        assertEquals(
                VerifiedTransportState.AVAILABLE_FROM_BANK,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "withdraw 1"
                        )
        );
    }

    @Test
    public void icyBasaltIsLockedUntilQuestIsFinished()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.IN_PROGRESS,
                        true
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.WEISS_TELEPORT_BASALT,
                        1L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        storageSnapshot,
                        HerbPatchDefinition.WEISS,
                        TransportVerificationKind.ICY_BASALT,
                        "Icy basalt"
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Making Friends with My Arm"
                        )
        );
    }

    @Test
    public void icyBasaltDoesNotRequireFireOfNourishmentForTeleport()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.FINISHED,
                        false
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.WEISS_TELEPORT_BASALT,
                        2L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        storageSnapshot,
                        HerbPatchDefinition.WEISS,
                        TransportVerificationKind.ICY_BASALT,
                        "Icy basalt"
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "2 in inventory"
                        )
        );
    }

    @Test
    public void finishedQuestWithoutBasaltIsMissing()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.FINISHED,
                        true
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.WEISS_TELEPORT_BASALT,
                        0L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        storageSnapshot,
                        HerbPatchDefinition.WEISS,
                        TransportVerificationKind.ICY_BASALT,
                        "Icy basalt"
                );

        assertEquals(
                VerifiedTransportState.MISSING,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "No Icy basalt"
                        )
        );
    }

    @Test
    public void carriedSkillsNecklaceUsesVerifiedChargeCatalogue()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.FINISHED,
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                createInventoryTravelSnapshot(
                        ItemID.JEWL_NECKLACE_OF_SKILLS_3,
                        1
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.JEWL_NECKLACE_OF_SKILLS_3,
                        0L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        travelSnapshot,
                        storageSnapshot,
                        HerbPatchDefinition.FARMING_GUILD,
                        TransportVerificationKind.SKILLS_NECKLACE,
                        "Skills necklace"
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Carried usable charges: 3"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Skills necklace(3) x1"
                        )
        );
    }

    @Test
    public void equippedSkillsNecklaceCountsAsReadyNow()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.FINISHED,
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                createEquipmentTravelSnapshot(
                        ItemID.JEWL_NECKLACE_OF_SKILLS_2,
                        1
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.JEWL_NECKLACE_OF_SKILLS_2,
                        0L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        travelSnapshot,
                        storageSnapshot,
                        HerbPatchDefinition.FARMING_GUILD,
                        TransportVerificationKind.SKILLS_NECKLACE,
                        "Skills necklace"
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Carried usable charges: 2"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Skills necklace(2) x1"
                        )
        );
    }

    @Test
    public void bankedSkillsNecklaceUsesRequirementWithdrawalPlan()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.FINISHED,
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                createEmptyTravelSnapshot();

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.JEWL_NECKLACE_OF_SKILLS_6,
                        0L,
                        1L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        travelSnapshot,
                        storageSnapshot,
                        HerbPatchDefinition.FARMING_GUILD,
                        TransportVerificationKind.SKILLS_NECKLACE,
                        "Skills necklace"
                );

        assertEquals(
                VerifiedTransportState.AVAILABLE_FROM_BANK,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Verified bank usable charges: 6"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Withdraw: Skills necklace(6) x1"
                        )
        );
    }

    @Test
    public void unchargedSkillsNecklaceRemainsMissing()
    {
        AccountState accountState =
                createAccountState(
                        QuestState.FINISHED,
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                createInventoryTravelSnapshot(
                        ItemID.JEWL_NECKLACE_OF_SKILLS,
                        1
                );

        StorageSnapshot storageSnapshot =
                createStorageSnapshot(
                        ItemID.JEWL_NECKLACE_OF_SKILLS,
                        1L,
                        0L
                );

        VerifiedTransportResult result =
                evaluateSingleOption(
                        accountState,
                        travelSnapshot,
                        storageSnapshot,
                        HerbPatchDefinition.FARMING_GUILD,
                        TransportVerificationKind.SKILLS_NECKLACE,
                        "Skills necklace"
                );

        assertEquals(
                VerifiedTransportState.MISSING,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    private VerifiedTransportResult evaluateSingleOption(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            HerbPatchDefinition patch,
            TransportVerificationKind verificationKind,
            String displayName
    )
    {
        return evaluateSingleOption(
                accountState,
                createEmptyTravelSnapshot(),
                storageSnapshot,
                patch,
                verificationKind,
                displayName
        );
    }

    private VerifiedTransportResult evaluateSingleOption(
            AccountState accountState,
            TravelCapabilitySnapshot travelSnapshot,
            StorageSnapshot storageSnapshot,
            HerbPatchDefinition patch,
            TransportVerificationKind verificationKind,
            String displayName
    )
    {
        TransportOption option =
                new TransportOption(
                        1,
                        displayName,
                        "Test transport instructions.",
                        TransportCategory.ITEM,
                        verificationKind
                );

        Map<
                HerbPatchDefinition,
                List<TransportOption>
                > options =
                new EnumMap<>(
                        HerbPatchDefinition.class
                );

        options.put(
                patch,
                Collections.singletonList(
                        option
                )
        );

        TransportVerificationService service =
                new TransportVerificationService();

        Map<
                HerbPatchDefinition,
                List<VerifiedTransportResult>
                > results =
                service.evaluateAll(
                        accountState,
                        travelSnapshot,
                        storageSnapshot,
                        options
                );

        assertTrue(
                results.containsKey(
                        patch
                )
        );

        assertEquals(
                1,
                results.get(
                        patch
                ).size()
        );

        return results
                .get(
                        patch
                )
                .get(
                        0
                );
    }

    private AccountState createAccountState(
            QuestState makingFriendsState,
            boolean fireOfNourishmentBuilt
    )
    {
        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.MAKING_FRIENDS_WITH_MY_ARM,
                makingFriendsState
        );

        return new AccountState(
                true,
                99,
                99,
                99,
                99,
                99,
                99,
                99,
                99,
                questStates,
                Collections.emptyMap(),
                Collections.emptyMap(),
                fireOfNourishmentBuilt
        );
    }

    private TravelCapabilitySnapshot
    createEmptyTravelSnapshot()
    {
        return new TravelCapabilitySnapshot(
                true,
                SpellbookType.STANDARD,
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    private TravelCapabilitySnapshot
    createInventoryTravelSnapshot(
            int itemId,
            int quantity
    )
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                itemId,
                quantity
        );

        return new TravelCapabilitySnapshot(
                true,
                SpellbookType.STANDARD,
                null,
                inventory,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    private TravelCapabilitySnapshot
    createEquipmentTravelSnapshot(
            int itemId,
            int quantity
    )
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                itemId,
                quantity
        );

        return new TravelCapabilitySnapshot(
                true,
                SpellbookType.STANDARD,
                null,
                Collections.emptyMap(),
                equipment,
                Collections.emptyMap()
        );
    }

    private StorageSnapshot createStorageSnapshot(
            int itemId,
            long inventoryQuantity,
            long bankQuantity
    )
    {
        Map<Integer, Long> inventory =
                new LinkedHashMap<>();

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        if (inventoryQuantity > 0L)
        {
            inventory.put(
                    itemId,
                    inventoryQuantity
            );
        }

        if (bankQuantity > 0L)
        {
            bank.put(
                    itemId,
                    bankQuantity
            );
        }

        return new StorageSnapshot(
                inventory,
                Collections.emptyMap(),
                bank,
                true,
                Instant.now()
        );
    }
}