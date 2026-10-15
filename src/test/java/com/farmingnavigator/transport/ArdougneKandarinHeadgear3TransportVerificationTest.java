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

public class ArdougneKandarinHeadgear3TransportVerificationTest
{
    @Test
    public void carriedAndEquippedHeadgearAreReadyWithOneUseRemaining()
    {
        VerifiedTransportResult carried = dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, items(ItemID.SEERS_HEADBAND_HARD),
                Collections.emptyMap(), Collections.emptyMap(), 1, true));
        assertEquals(VerifiedTransportState.READY_NOW, carried.getState());
        assertTrue(carried.isUsableForPlannedRun());
        assertTrue(carried.getDetail().contains("daily Sherlock teleport remains available"));
        assertFalse(carried.getDetail().contains("unlimited"));

        assertEquals(VerifiedTransportState.READY_NOW, dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, Collections.emptyMap(),
                items(ItemID.SEERS_HEADBAND_HARD), Collections.emptyMap(), 1, true)).getState());
    }

    @Test
    public void bankedHeadgearIsAvailableFromBank()
    {
        VerifiedTransportResult result = dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, Collections.emptyMap(),
                Collections.emptyMap(), items(ItemID.SEERS_HEADBAND_HARD), 1, true));
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, result.getState());
        assertTrue(result.isUsableForPlannedRun());
        assertTrue(result.getDetail().contains("Kandarin headgear 3"));
        assertTrue(result.getDetail().contains("AVAILABLE FROM BANK"));
    }

    @Test
    public void incompleteDiaryAndUnclaimedRewardAreLocked()
    {
        assertEquals(VerifiedTransportState.LOCKED, dailyResult(evaluate(
                DiaryTier.MEDIUM, DiaryTier.MEDIUM, items(ItemID.SEERS_HEADBAND_HARD),
                Collections.emptyMap(), Collections.emptyMap(), 1, true)).getState());
        VerifiedTransportResult unclaimed = dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.MEDIUM, items(ItemID.SEERS_HEADBAND_HARD),
                Collections.emptyMap(), Collections.emptyMap(), 1, true));
        assertEquals(VerifiedTransportState.LOCKED, unclaimed.getState());
        assertFalse(unclaimed.isUsableForPlannedRun());
        assertTrue(unclaimed.getDetail().contains("reward has not been claimed"));
    }

    @Test
    public void missingHeadgearIsMissing()
    {
        assertEquals(VerifiedTransportState.MISSING, dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), 1, true)).getState());
    }

    @Test
    public void exhaustedUseBlocksCarriedAndBankedHeadgear()
    {
        VerifiedTransportResult carried = dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, items(ItemID.SEERS_HEADBAND_HARD),
                Collections.emptyMap(), Collections.emptyMap(), 0, true));
        assertEquals(VerifiedTransportState.MISSING, carried.getState());
        assertFalse(carried.isUsableForPlannedRun());
        assertEquals(VerifiedTransportState.MISSING, dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, Collections.emptyMap(),
                Collections.emptyMap(), items(ItemID.SEERS_HEADBAND_HARD), 0, true)).getState());
    }

    @Test
    public void unknownDailyUseRequiresDataForCarriedAndBankedHeadgear()
    {
        VerifiedTransportResult carried = dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, items(ItemID.SEERS_HEADBAND_HARD),
                Collections.emptyMap(), Collections.emptyMap(), null, true));
        assertEquals(VerifiedTransportState.DATA_REQUIRED, carried.getState());
        assertFalse(carried.isUsableForPlannedRun());
        assertEquals(VerifiedTransportState.DATA_REQUIRED, dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, Collections.emptyMap(),
                Collections.emptyMap(), items(ItemID.SEERS_HEADBAND_HARD), null, true)).getState());
    }

    @Test
    public void knownDiaryBlockerWinsOverUnknownDailyUse()
    {
        assertEquals(VerifiedTransportState.LOCKED, dailyResult(evaluate(
                DiaryTier.HARD, DiaryTier.MEDIUM, items(ItemID.SEERS_HEADBAND_HARD),
                Collections.emptyMap(), Collections.emptyMap(), null, true)).getState());
    }

    @Test
    public void headgear4DoesNotReplaceHeadgear3OrDependOnDailyUse()
    {
        List<VerifiedTransportResult> results = evaluate(
                DiaryTier.ELITE, DiaryTier.ELITE, items(ItemID.SEERS_HEADBAND_ELITE),
                Collections.emptyMap(), Collections.emptyMap(), 0, true);
        assertEquals(VerifiedTransportState.READY_NOW, results.get(0).getState());
        assertEquals(12, results.get(0).getOption().getBaselineRank());
        assertEquals(VerifiedTransportState.MISSING, dailyResult(results).getState());
    }

    @Test
    public void usableDailyRouteSortsBeforeLockedUnlimitedRoute()
    {
        List<VerifiedTransportResult> results = evaluate(
                DiaryTier.HARD, DiaryTier.HARD, items(ItemID.SEERS_HEADBAND_HARD),
                Collections.emptyMap(), Collections.emptyMap(), 1, true);
        assertEquals(13, results.get(0).getOption().getBaselineRank());
        assertEquals(VerifiedTransportState.READY_NOW, results.get(0).getState());
        assertEquals(VerifiedTransportState.LOCKED, results.get(1).getState());
    }

    @Test
    public void unknownBankPreservesVerificationGate()
    {
        assertTrue(evaluate(
                DiaryTier.HARD, DiaryTier.HARD, items(ItemID.SEERS_HEADBAND_HARD),
                Collections.emptyMap(), Collections.emptyMap(), 1, false).isEmpty());
    }

    private VerifiedTransportResult dailyResult(List<VerifiedTransportResult> results)
    {
        return results.stream().filter(result -> result.getOption().getVerificationKind()
                == TransportVerificationKind.KANDARIN_HEADGEAR_3_DAILY).findFirst().get();
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
                Collections.emptyMap(), Collections.singletonMap(DiaryArea.KANDARIN, completed),
                Collections.singletonMap(DiaryArea.KANDARIN, claimed), false, false);
        Map<DailyTransportUseKind, Integer> dailyUses = new EnumMap<>(DailyTransportUseKind.class);
        if (remainingUses != null)
        {
            dailyUses.put(DailyTransportUseKind.KANDARIN_SHERLOCK_TELEPORT, remainingUses);
        }
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(
                true, SpellbookType.STANDARD, null,
                inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())),
                equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())),
                Collections.emptyMap(), Collections.emptyMap(), false, dailyUses);
        StorageSnapshot storage = new StorageSnapshot(inventory, equipment, bank,
                bankVerified, bankVerified ? Instant.now() : null);
        List<TransportOption> options = new HerbPatchTransportService()
                .getOptions(HerbPatchDefinition.ARDOUGNE).stream()
                .filter(option -> option.getVerificationKind() == TransportVerificationKind.KANDARIN_HEADGEAR_3_DAILY
                        || option.getVerificationKind() == TransportVerificationKind.KANDARIN_HEADGEAR_UNLIMITED)
                .collect(Collectors.toList());
        return new TransportVerificationService().evaluateAll(account, travel, storage,
                Collections.singletonMap(HerbPatchDefinition.ARDOUGNE, options))
                .getOrDefault(HerbPatchDefinition.ARDOUGNE, Collections.emptyList());
    }
}
