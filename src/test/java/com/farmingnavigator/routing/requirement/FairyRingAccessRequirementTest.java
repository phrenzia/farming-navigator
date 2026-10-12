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
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FairyRingAccessRequirementTest
{
    @Test
    public void inProgressQuestBeforeGodfatherIsLocked()
    {
        RequirementContext context =
                createContext(
                        false,
                        false,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        RequirementResult result =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Fairy Godfather"
                        )
        );
    }

    @Test
    public void partialQuestProgressWithEquippedDramenStaffIsReady()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        RequirementContext context =
                createContext(
                        true,
                        false,
                        Collections.emptyMap(),
                        equipment,
                        Collections.emptyMap(),
                        true
                );

        RequirementResult result =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Dramen staff is equipped"
                        )
        );
    }

    @Test
    public void partialQuestProgressWithInventoryLunarStaffIsReady()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF,
                1
        );

        RequirementContext context =
                createContext(
                        true,
                        false,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        RequirementResult result =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "equip the Lunar staff from inventory"
                        )
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

        RequirementContext context =
                createContext(
                        true,
                        false,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true
                );

        RequirementResult result =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result.isSatisfied()
        );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertEquals(
                RequirementReadiness.AVAILABLE_FROM_BANK,
                result.getReadiness()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "withdraw and equip a Dramen staff"
                        )
        );
    }

    @Test
    public void claimedLumbridgeEliteNeedsNoStaff()
    {
        RequirementContext context =
                createContext(
                        true,
                        true,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        RequirementResult result =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "removes the staff requirement"
                        )
        );
    }

    @Test
    public void completedButUnclaimedEliteDoesNotBypassStaff()
    {
        EnumMap<DiaryArea, DiaryTier>
                completedDiaries =
                new EnumMap<>(
                        DiaryArea.class
                );

        completedDiaries.put(
                DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                DiaryTier.ELITE
        );

        AccountState accountState =
                createAccountState(
                        true,
                        completedDiaries,
                        Collections.emptyMap()
                );

        RequirementContext context =
                createContext(
                        accountState,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        RequirementResult result =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void unlockedButNoStaffIsMissing()
    {
        RequirementContext context =
                createContext(
                        true,
                        false,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        RequirementResult result =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void unlockedWithoutCarriedStaffAndUnverifiedBankNeedsData()
    {
        RequirementContext context =
                createContext(
                        true,
                        false,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        RequirementResult result =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Open any normal bank once"
                        )
        );
    }

    private RequirementContext createContext(
            boolean fairyRingAccessUnlocked,
            boolean lumbridgeEliteClaimed,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        EnumMap<DiaryArea, DiaryTier>
                claimedDiaries =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (lumbridgeEliteClaimed)
        {
            claimedDiaries.put(
                    DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                    DiaryTier.ELITE
            );
        }

        AccountState accountState =
                createAccountState(
                        fairyRingAccessUnlocked,
                        claimedDiaries,
                        claimedDiaries
                );

        return createContext(
                accountState,
                inventory,
                equipment,
                bank,
                bankVerified
        );
    }

    private RequirementContext createContext(
            AccountState accountState,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
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
                        bankVerified,
                        bankVerified
                                ? Instant.now()
                                : null
                );

        return new RequirementContext(
                accountState,
                null,
                travelSnapshot,
                storageSnapshot
        );
    }

    private AccountState createAccountState(
            boolean fairyRingAccessUnlocked,
            Map<DiaryArea, DiaryTier> completedDiaries,
            Map<DiaryArea, DiaryTier> claimedDiaries
    )
    {
        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.FAIRYTALE_II__CURE_A_QUEEN,
                QuestState.IN_PROGRESS
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