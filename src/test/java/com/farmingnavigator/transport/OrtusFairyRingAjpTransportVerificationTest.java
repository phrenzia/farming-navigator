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

public class OrtusFairyRingAjpTransportVerificationTest
{
    @Test
    public void cataloguePromotesAjpInPlaceAndPreservesOtherOrtusRoutes()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        List<TransportOption> options =
                service.getOptions(
                        HerbPatchDefinition.ORTUS_FARM
                );

        assertEquals(
                5,
                options.size()
        );

        assertEquals(
                1,
                options.get(0)
                        .getBaselineRank()
        );

        assertEquals(
                TransportVerificationKind.QUETZAL_WHISTLE,
                options.get(0)
                        .getVerificationKind()
        );

        assertEquals(
                2,
                options.get(1)
                        .getBaselineRank()
        );

        assertEquals(
                TransportVerificationKind.NOT_IMPLEMENTED,
                options.get(1)
                        .getVerificationKind()
        );

        assertEquals(
                3,
                options.get(2)
                        .getBaselineRank()
        );

        assertEquals(
                TransportVerificationKind.TELEPORT_SPELL,
                options.get(2)
                        .getVerificationKind()
        );

        assertEquals(
                4,
                options.get(3)
                        .getBaselineRank()
        );

        assertEquals(
                TransportVerificationKind.POH_PORTAL,
                options.get(3)
                        .getVerificationKind()
        );

        TransportOption ajp =
                options.get(4);

        assertEquals(
                5,
                ajp.getBaselineRank()
        );

        assertEquals(
                "Fairy ring AJP",
                ajp.getDisplayName()
        );

        assertEquals(
                TransportCategory.FAIRY_RING,
                ajp.getCategory()
        );

        assertEquals(
                TransportVerificationKind.FAIRY_RING_AJP,
                ajp.getVerificationKind()
        );
    }

    @Test
    public void visitedVarlamoreAndCarriedDramenStaffIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluateAjp(
                        true,
                        true,
                        false,
                        QuestState.FINISHED,
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
    }

    @Test
    public void visitedVarlamoreAndBankedLunarStaffIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF,
                1L
        );

        VerifiedTransportResult result =
                evaluateAjp(
                        true,
                        true,
                        false,
                        QuestState.FINISHED,
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
                                "Lunar staff"
                        )
        );
    }

    @Test
    public void claimedLumbridgeEliteNeedsNoStaff()
    {
        VerifiedTransportResult result =
                evaluateAjp(
                        true,
                        true,
                        true,
                        QuestState.FINISHED,
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
    public void completedChildrenOfTheSunDoesNotReplaceFirstVisit()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluateAjp(
                        true,
                        false,
                        false,
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        equipment,
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
                                "Civitas illa Fortis"
                        )
        );
    }

    @Test
    public void fairyRingAccessNotUnlockedIsLocked()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluateAjp(
                        false,
                        true,
                        false,
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        equipment,
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
                                "Fairytale II"
                        )
        );
    }

    @Test
    public void visitedVarlamoreWithoutStaffIsMissingWithVerifiedBank()
    {
        VerifiedTransportResult result =
                evaluateAjp(
                        true,
                        true,
                        false,
                        QuestState.FINISHED,
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

    private VerifiedTransportResult evaluateAjp(
            boolean fairyRingAccessUnlocked,
            boolean varlamoreVisited,
            boolean lumbridgeEliteClaimed,
            QuestState childrenOfTheSunState,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank
    )
    {
        HerbPatchTransportService transportService =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        transportService,
                        "Fairy ring AJP"
                );

        assertNotNull(
                option
        );

        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.CHILDREN_OF_THE_SUN,
                childrenOfTheSunState
        );

        /*
         * Deliberately use the exact persistent fairy-ring
         * flag below instead of treating coarse quest state
         * as the unlock.
         */
        questStates.put(
                Quest.FAIRYTALE_II__CURE_A_QUEEN,
                QuestState.IN_PROGRESS
        );

        EnumMap<DiaryArea, DiaryTier>
                completedDiaryTiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        EnumMap<DiaryArea, DiaryTier>
                claimedDiaryTiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (lumbridgeEliteClaimed)
        {
            completedDiaryTiers.put(
                    DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                    DiaryTier.ELITE
            );

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
                        completedDiaryTiers,
                        claimedDiaryTiers,
                        false,
                        fairyRingAccessUnlocked,
                        varlamoreVisited
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
                HerbPatchDefinition.ORTUS_FARM,
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

        assertTrue(
                results.containsKey(
                        HerbPatchDefinition.ORTUS_FARM
                )
        );

        assertEquals(
                1,
                results.get(
                        HerbPatchDefinition.ORTUS_FARM
                ).size()
        );

        return results
                .get(
                        HerbPatchDefinition.ORTUS_FARM
                )
                .get(
                        0
                );
    }

    private TransportOption findOption(
            HerbPatchTransportService service,
            String displayName
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                HerbPatchDefinition.ORTUS_FARM
        )
        )
        {
            if (
                    displayName.equals(
                            option.getDisplayName()
                    )
            )
            {
                return option;
            }
        }

        return null;
    }

    private Map<Integer, Long> convertToLongMap(
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