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

public class FairyRingTransportVerificationTest
{
    @Test
    public void catalogueUsesSafeFairyRingVerificationKinds()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        assertVerificationKind(
                service,
                HerbPatchDefinition.MORYTANIA,
                "Fairy ring ALQ",
                TransportVerificationKind.FAIRY_RING_ACCESS
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.HOSIDIUS,
                "Fairy ring AKR",
                TransportVerificationKind.FAIRY_RING_ACCESS
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.FARMING_GUILD,
                "Fairy ring CIR",
                TransportVerificationKind.FAIRY_RING_ACCESS
        );

        /*
         * DKS is not just a fairy-ring destination.
         *
         * The complete Weiss route also needs Making Friends
         * with My Arm completed and Larry's boat access.
         *
         * It therefore uses its specialized verifier rather
         * than the generic FAIRY_RING_ACCESS verifier.
         */
        assertVerificationKind(
                service,
                HerbPatchDefinition.WEISS,
                "Fairy ring DKS and Larry's boat",
                TransportVerificationKind.WEISS_LARRY_BOAT
        );

        /*
         * AJP remains intentionally excluded until its
         * Varlamore-specific destination access state is
         * independently verified.
         */
        assertVerificationKind(
                service,
                HerbPatchDefinition.ORTUS_FARM,
                "Fairy ring AJP",
                TransportVerificationKind.NOT_IMPLEMENTED
        );
    }

    @Test
    public void fairyRingRouteIsLockedBeforeGodfatherUnlock()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluateFairyRing(
                        createAccountState(
                                false,
                                false
                        ),
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
                                "Fairy Godfather"
                        )
        );
    }

    @Test
    public void unlockedFairyRingWithCarriedStaffIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluateFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
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
                                "Dramen staff"
                        )
        );
    }

    @Test
    public void unlockedFairyRingWithBankedStaffIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF,
                1L
        );

        VerifiedTransportResult result =
                evaluateFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
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
    public void unlockedFairyRingWithoutStaffIsMissing()
    {
        VerifiedTransportResult result =
                evaluateFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
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

    @Test
    public void claimedLumbridgeEliteMakesFairyRingReadyWithoutStaff()
    {
        VerifiedTransportResult result =
                evaluateFairyRing(
                        createAccountState(
                                true,
                                true
                        ),
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

        assertTrue(
                result.getDetail()
                        .contains(
                                "removes the staff requirement"
                        )
        );
    }

    private void assertVerificationKind(
            HerbPatchTransportService service,
            HerbPatchDefinition patch,
            String displayName,
            TransportVerificationKind expectedKind
    )
    {
        TransportOption option =
                findOption(
                        service,
                        patch,
                        displayName
                );

        assertNotNull(
                option
        );

        assertEquals(
                expectedKind,
                option.getVerificationKind()
        );
    }

    private TransportOption findOption(
            HerbPatchTransportService service,
            HerbPatchDefinition patch,
            String displayName
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                patch
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

    private VerifiedTransportResult evaluateFairyRing(
            AccountState accountState,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank
    )
    {
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

        TransportOption option =
                new TransportOption(
                        1,
                        "Fairy ring ALQ",
                        "Test fairy-ring route.",
                        TransportCategory.FAIRY_RING,
                        TransportVerificationKind.FAIRY_RING_ACCESS
                );

        Map<
                HerbPatchDefinition,
                List<TransportOption>
                > options =
                new EnumMap<>(
                        HerbPatchDefinition.class
                );

        options.put(
                HerbPatchDefinition.MORYTANIA,
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
                        HerbPatchDefinition.MORYTANIA
                )
                .get(
                        0
                );
    }

    private AccountState createAccountState(
            boolean fairyRingAccessUnlocked,
            boolean lumbridgeEliteClaimed
    )
    {
        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        /*
         * Deliberately keep the quest IN_PROGRESS in both
         * cases.
         *
         * The exact progress flag, not the coarse quest
         * state, decides whether fairy rings are unlocked.
         */
        questStates.put(
                Quest.FAIRYTALE_II__CURE_A_QUEEN,
                QuestState.IN_PROGRESS
        );

        EnumMap<DiaryArea, DiaryTier>
                completedDiaries =
                new EnumMap<>(
                        DiaryArea.class
                );

        EnumMap<DiaryArea, DiaryTier>
                claimedDiaries =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (lumbridgeEliteClaimed)
        {
            completedDiaries.put(
                    DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                    DiaryTier.ELITE
            );

            claimedDiaries.put(
                    DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                    DiaryTier.ELITE
            );
        }

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
                completedDiaries,
                claimedDiaries,
                false,
                fairyRingAccessUnlocked
        );
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