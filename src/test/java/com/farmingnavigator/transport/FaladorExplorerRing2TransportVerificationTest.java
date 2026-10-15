package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.DailyTransportUseKind;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FaladorExplorerRing2TransportVerificationTest
{
    @Test
    public void carriedAndEquippedRingAreReadyWithOneUseRemaining()
    {
        VerifiedTransportResult carried = dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                Collections.emptyMap(), Collections.emptyMap(), 1, true));
        assertEquals(VerifiedTransportState.READY_NOW, carried.getState());
        assertTrue(carried.isUsableForPlannedRun());
        assertTrue(carried.getDetail().contains("daily Cabbage-port teleport remains available"));
        assertFalse(carried.getDetail().contains("unlimited"));

        assertEquals(VerifiedTransportState.READY_NOW, dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, Collections.emptyMap(),
                items(ItemID.LUMBRIDGE_RING_MEDIUM), Collections.emptyMap(), 1, true)).getState());
    }

    @Test
    public void bankedRingIsAvailableFromBank()
    {
        VerifiedTransportResult result = dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, Collections.emptyMap(),
                Collections.emptyMap(), items(ItemID.LUMBRIDGE_RING_MEDIUM), 1, true));
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, result.getState());
        assertTrue(result.isUsableForPlannedRun());
        assertTrue(result.getDetail().contains("Explorer's ring 2"));
        assertTrue(result.getDetail().contains("AVAILABLE FROM BANK"));
    }

    @Test
    public void incompleteDiaryAndUnclaimedRewardAreLocked()
    {
        assertEquals(VerifiedTransportState.LOCKED, dailyResult(evaluate(
                DiaryTier.EASY, DiaryTier.EASY, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                Collections.emptyMap(), Collections.emptyMap(), 1, true)).getState());
        VerifiedTransportResult unclaimed = dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.EASY, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                Collections.emptyMap(), Collections.emptyMap(), 1, true));
        assertEquals(VerifiedTransportState.LOCKED, unclaimed.getState());
        assertFalse(unclaimed.isUsableForPlannedRun());
        assertTrue(unclaimed.getDetail().contains("reward has not been claimed"));
    }

    @Test
    public void missingRingIsMissing()
    {
        assertEquals(VerifiedTransportState.MISSING, dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), 1, true)).getState());
    }

    @Test
    public void exhaustedUseBlocksCarriedAndBankedRing()
    {
        VerifiedTransportResult carried = dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                Collections.emptyMap(), Collections.emptyMap(), 0, true));
        assertEquals(VerifiedTransportState.MISSING, carried.getState());
        assertFalse(carried.isUsableForPlannedRun());
        assertEquals(VerifiedTransportState.MISSING, dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, Collections.emptyMap(),
                Collections.emptyMap(), items(ItemID.LUMBRIDGE_RING_MEDIUM), 0, true)).getState());
    }

    @Test
    public void unknownDailyUseRequiresDataForCarriedAndBankedRing()
    {
        VerifiedTransportResult carried = dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                Collections.emptyMap(), Collections.emptyMap(), null, true));
        assertEquals(VerifiedTransportState.DATA_REQUIRED, carried.getState());
        assertFalse(carried.isUsableForPlannedRun());
        assertEquals(VerifiedTransportState.DATA_REQUIRED, dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, Collections.emptyMap(),
                Collections.emptyMap(), items(ItemID.LUMBRIDGE_RING_MEDIUM), null, true)).getState());
    }

    @Test
    public void knownDiaryBlockerWinsOverUnknownDailyUse()
    {
        assertEquals(VerifiedTransportState.LOCKED, dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.EASY, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                Collections.emptyMap(), Collections.emptyMap(), null, true)).getState());
    }

    @Test
    public void higherRingDoesNotReplaceRing2OrDependOnDailyUse()
    {
        List<VerifiedTransportResult> results = evaluate(
                DiaryTier.ELITE, DiaryTier.ELITE, items(ItemID.LUMBRIDGE_RING_ELITE),
                Collections.emptyMap(), Collections.emptyMap(), 0, true);
        assertEquals(VerifiedTransportState.READY_NOW, results.get(0).getState());
        assertEquals(1, results.get(0).getOption().getBaselineRank());
        assertEquals(VerifiedTransportState.MISSING, dailyResult(results).getState());
    }

    @Test
    public void usableDailyRouteSortsBeforeMissingUnlimitedRoute()
    {
        List<VerifiedTransportResult> results = evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                Collections.emptyMap(), Collections.emptyMap(), 1, true);
        assertEquals(2, results.get(0).getOption().getBaselineRank());
        assertEquals(VerifiedTransportState.READY_NOW, results.get(0).getState());
        assertEquals(VerifiedTransportState.MISSING, results.get(1).getState());
    }

    @Test
    public void unknownBankPreservesVerificationGate()
    {
        assertTrue(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                Collections.emptyMap(), Collections.emptyMap(), 1, false).isEmpty());
    }

    @Test
    public void cataloguePreservesRankAndGuidance()
    {
        TransportOption option = new HerbPatchTransportService().getOptions(HerbPatchDefinition.FALADOR)
                .stream().filter(o -> o.getBaselineRank() == 2).findFirst().get();
        assertEquals("Explorer's ring 2", option.getDisplayName());
        assertEquals(TransportCategory.DIARY_ITEM, option.getCategory());
        assertEquals(TransportVerificationKind.EXPLORER_RING_2_DAILY, option.getVerificationKind());
        assertEquals("Use one of the three daily Cabbage-port teleports.", option.getInstructions());
    }

    @Test
    public void allPositiveDailyCountsAreUsable()
    {
        for (int remaining = 1; remaining <= 3; remaining++)
        {
            assertEquals(VerifiedTransportState.READY_NOW, dailyResult(evaluate(
                    DiaryTier.MEDIUM, DiaryTier.MEDIUM, items(ItemID.LUMBRIDGE_RING_MEDIUM),
                    Collections.emptyMap(), Collections.emptyMap(), remaining, true)).getState());
        }
    }

    private VerifiedTransportResult dailyResult(List<VerifiedTransportResult> results)
    {
        return results.stream().filter(result -> result.getOption().getVerificationKind()
                == TransportVerificationKind.EXPLORER_RING_2_DAILY).findFirst().get();
    }

    private Map<Integer, Long> items(int itemId)
    {
        return Collections.singletonMap(itemId, 1L);
    }

    private List<VerifiedTransportResult> evaluate(
            DiaryTier completed, DiaryTier claimed,
            Map<Integer, Long> inventory, Map<Integer, Long> equipment,
            Map<Integer, Long> bank, Integer remainingUses, boolean bankVerified)
    {
        AccountState account = new AccountState(
                true, 99, 99, 99, 99, 99, 99, 99, 99,
                Collections.emptyMap(), Collections.singletonMap(DiaryArea.LUMBRIDGE_AND_DRAYNOR, completed),
                Collections.singletonMap(DiaryArea.LUMBRIDGE_AND_DRAYNOR, claimed), false, false);
        Map<DailyTransportUseKind, Integer> dailyUses = new EnumMap<>(DailyTransportUseKind.class);
        if (remainingUses != null)
        {
            dailyUses.put(DailyTransportUseKind.EXPLORER_RING_CABBAGE_PORT, remainingUses);
        }
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(
                true, SpellbookType.STANDARD, null,
                inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())),
                equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())),
                Collections.emptyMap(), Collections.emptyMap(), false, dailyUses);
        StorageSnapshot storage = new StorageSnapshot(inventory, equipment, bank,
                bankVerified, bankVerified ? Instant.now() : null);
        List<TransportOption> options = new HerbPatchTransportService()
                .getOptions(HerbPatchDefinition.FALADOR).stream()
                .filter(option -> option.getVerificationKind() == TransportVerificationKind.EXPLORER_RING_2_DAILY
                        || option.getVerificationKind() == TransportVerificationKind.EXPLORER_RING_UNLIMITED)
                .collect(Collectors.toList());
        return new TransportVerificationService().evaluateAll(account, travel, storage,
                Collections.singletonMap(HerbPatchDefinition.FALADOR, options))
                .getOrDefault(HerbPatchDefinition.FALADOR, Collections.emptyList());
    }
}
