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
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArdougneKandarinHeadgearTransportVerificationTest
{
    @Test
    public void catalogueAddsUnlimitedHeadgearAndKeepsHeadgear3Future()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption unlimited =
                findOption(
                        service,
                        "Kandarin headgear 4 to Sherlock"
                );

        assertNotNull(
                unlimited
        );

        assertEquals(
                TransportVerificationKind.KANDARIN_HEADGEAR_UNLIMITED,
                unlimited.getVerificationKind()
        );

        assertEquals(
                TransportCategory.DIARY_ITEM,
                unlimited.getCategory()
        );

        assertEquals(
                12,
                unlimited.getBaselineRank()
        );

        TransportOption limited =
                findOption(
                        service,
                        "Kandarin headgear 3 to Sherlock"
                );

        assertNotNull(
                limited
        );

        assertEquals(
                TransportVerificationKind.NOT_IMPLEMENTED,
                limited.getVerificationKind()
        );

        assertEquals(
                13,
                limited.getBaselineRank()
        );
    }

    @Test
    public void claimedEliteAndCarriedHeadgearIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
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
                                "Sherlock teleport is unlimited"
                        )
        );
    }

    @Test
    public void claimedEliteAndEquippedHeadgearIsReadyNow()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        Collections.emptyMap(),
                        equipment,
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
    public void claimedEliteAndBankedHeadgearIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1L
        );

        VerifiedTransportResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
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
                                "Kandarin headgear 4"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "AVAILABLE FROM BANK"
                        )
        );
    }

    @Test
    public void completedEliteButRewardNotClaimedIsLocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.HARD,
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
                                "reward has not been claimed"
                        )
        );
    }

    @Test
    public void eliteDiaryNotCompletedIsLocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        DiaryTier.HARD,
                        DiaryTier.HARD,
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
                                "Kandarin Elite Diary"
                        )
        );
    }

    @Test
    public void claimedEliteWithoutHeadgearIsMissing()
    {
        VerifiedTransportResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
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
    public void headgear3DoesNotQualifyForUnlimitedRoute()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_HARD,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        inventory,
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
    }

    private TransportOption findOption(
            HerbPatchTransportService service,
            String displayName
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                HerbPatchDefinition.ARDOUGNE
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

    private VerifiedTransportResult evaluate(
            DiaryTier completedKandarinTier,
            DiaryTier claimedKandarinTier,
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
                        "Kandarin headgear 4 to Sherlock"
                );

        assertNotNull(
                option
        );

        EnumMap<DiaryArea, DiaryTier>
                completedDiaryTiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        completedDiaryTiers.put(
                DiaryArea.KANDARIN,
                completedKandarinTier
        );

        EnumMap<DiaryArea, DiaryTier>
                claimedDiaryTiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        claimedDiaryTiers.put(
                DiaryArea.KANDARIN,
                claimedKandarinTier
        );

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
                        Collections.emptyMap(),
                        completedDiaryTiers,
                        claimedDiaryTiers,
                        false,
                        false
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
                HerbPatchDefinition.ARDOUGNE,
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
                        HerbPatchDefinition.ARDOUGNE
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