package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
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

    private VerifiedTransportResult evaluateSingleOption(
            AccountState accountState,
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

    private StorageSnapshot createStorageSnapshot(
            int itemId,
            long inventoryQuantity,
            long bankQuantity
    )
    {
        Map<Integer, Long> inventory =
                new java.util.LinkedHashMap<>();

        Map<Integer, Long> bank =
                new java.util.LinkedHashMap<>();

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