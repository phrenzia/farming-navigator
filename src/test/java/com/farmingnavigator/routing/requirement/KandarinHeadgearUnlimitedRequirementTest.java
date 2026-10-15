package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KandarinHeadgearUnlimitedRequirementTest
{
    @Test
    public void claimedEliteAndCarriedHeadgearIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1
        );

        RequirementResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.READY_NOW,
                result.getReadiness()
        );

        assertTrue(
                result.isReadyNow()
        );

        assertEquals(
                "Kandarin headgear 4 to Sherlock",
                result.getRequirement()
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

        RequirementResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        Collections.emptyMap(),
                        equipment,
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertTrue(
                result.isReadyNow()
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

        RequirementResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.AVAILABLE_FROM_BANK,
                result.getReadiness()
        );

        assertTrue(
                result.requiresBankWithdrawal()
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
    public void completedEliteButRewardNotClaimedIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1
        );

        RequirementResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.HARD,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "reward has not been claimed"
                        )
        );
    }

    @Test
    public void eliteDiaryNotCompletedIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1
        );

        RequirementResult result =
                evaluate(
                        DiaryTier.HARD,
                        DiaryTier.HARD,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
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
        RequirementResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.NOT_AVAILABLE,
                result.getReadiness()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void claimedEliteWithUnknownBankRequiresData()
    {
        RequirementResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Open any normal bank once"
                        )
        );
    }

    @Test
    public void headgear3DoesNotCountAsUnlimitedRequirement()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_HARD,
                1
        );

        RequirementResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void carriedEliteHeadgearDoesNotNeedDailyUseData()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SEERS_HEADBAND_ELITE,
                1
        );

        RequirementResult result =
                evaluate(
                        DiaryTier.ELITE,
                        DiaryTier.ELITE,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    private RequirementResult evaluate(
            DiaryTier completedKandarinTier,
            DiaryTier claimedKandarinTier,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
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
                        bankVerified,
                        bankVerified
                                ? Instant.now()
                                : null
                );

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        null,
                        travelSnapshot,
                        storageSnapshot
                );

        return new KandarinHeadgearUnlimitedRequirement()
                .evaluate(
                        context
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