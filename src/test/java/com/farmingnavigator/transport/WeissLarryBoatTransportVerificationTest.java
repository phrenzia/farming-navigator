package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WeissLarryBoatTransportVerificationTest
{
    @Test
    public void cataloguePromotesLarryBoatToVerifiedTransport()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        service
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.WEISS_LARRY_BOAT,
                option.getVerificationKind()
        );

        assertEquals(
                TransportCategory.FAIRY_RING,
                option.getCategory()
        );

        assertEquals(
                3,
                option.getBaselineRank()
        );
    }

    @Test
    public void finishedQuestWithCarriedDramenStaffIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        false,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap()
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
                                "READY NOW"
                        )
        );
    }

    @Test
    public void finishedQuestWithClaimedLumbridgeEliteIsReadyNow()
    {
        VerifiedTransportResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        true,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );
    }

    @Test
    public void bankedDramenStaffIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.DRAMEN_STAFF,
                1L
        );

        VerifiedTransportResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        false,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank
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
                                "Dramen staff"
                        )
        );
    }

    @Test
    public void unfinishedQuestIsLocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        QuestState.IN_PROGRESS,
                        true,
                        false,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap()
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
    public void lockedFairyRingAccessIsLocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        QuestState.FINISHED,
                        false,
                        false,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap()
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
    public void missingStaffIsMissing()
    {
        VerifiedTransportResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        false,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap()
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

    private TransportOption findOption(
            HerbPatchTransportService service
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                HerbPatchDefinition.WEISS
        )
        )
        {
            if (
                    "Fairy ring DKS and Larry's boat"
                            .equals(
                                    option.getDisplayName()
                            )
            )
            {
                return option;
            }
        }

        return null;
    }

    private VerifiedTransportResult evaluate(
            QuestState questState,
            boolean fairyRingAccessUnlocked,
            boolean lumbridgeEliteClaimed,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank
    )
    {
        HerbPatchTransportService transportService =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        transportService
                );

        assertNotNull(
                option
        );

        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.MAKING_FRIENDS_WITH_MY_ARM,
                questState
        );

        EnumMap<DiaryArea, DiaryTier>
                claimedDiaryTiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (lumbridgeEliteClaimed)
        {
            claimedDiaryTiers.put(
                    DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                    DiaryTier.ELITE
            );
        }

        AccountState accountState =
                new AccountState(
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
                        claimedDiaryTiers,
                        true,
                        fairyRingAccessUnlocked
                );

        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.STANDARD,
                        null,
                        inventory,
                        equipment,
                        Collections.emptyMap()
                );

        StorageSnapshot storageSnapshot =
                new StorageSnapshot(
                        convertToLongMap(
                                inventory
                        ),
                        convertToLongMap(
                                equipment
                        ),
                        bank,
                        true,
                        Instant.now()
                );

        Map<
                HerbPatchDefinition,
                List<TransportOption>
                > options =
                new EnumMap<>(
                        HerbPatchDefinition.class
                );

        options.put(
                HerbPatchDefinition.WEISS,
                Collections.singletonList(
                        option
                )
        );

        Map<
                HerbPatchDefinition,
                List<VerifiedTransportResult>
                > results =
                new TransportVerificationService()
                        .evaluateAll(
                                accountState,
                                travelSnapshot,
                                storageSnapshot,
                                options
                        );

        return results
                .get(
                        HerbPatchDefinition.WEISS
                )
                .get(
                        0
                );
    }

    private Map<Integer, Long>
    convertToLongMap(
            Map<Integer, Integer> source
    )
    {
        Map<Integer, Long> result =
                new LinkedHashMap<>();

        for (
                Map.Entry<Integer, Integer> entry
                : source.entrySet()
        )
        {
            result.put(
                    entry.getKey(),
                    entry.getValue()
                            .longValue()
            );
        }

        return result;
    }
}