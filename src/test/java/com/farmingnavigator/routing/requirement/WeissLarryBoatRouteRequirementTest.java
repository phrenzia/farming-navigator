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

public class WeissLarryBoatRouteRequirementTest
{
    @Test
    public void finishedQuestWithCarriedDramenStaffIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        false,
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
    }

    @Test
    public void finishedQuestWithEquippedLunarStaffIsReadyNow()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        false,
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
    public void finishedQuestWithClaimedLumbridgeEliteNeedsNoStaff()
    {
        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        true,
                        Collections.emptyMap(),
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
                "Fairy ring DKS and Larry's boat to Weiss",
                result.getRequirement()
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

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        false,
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
                                "Dramen staff"
                        )
        );
    }

    @Test
    public void unfinishedQuestIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.IN_PROGRESS,
                        true,
                        false,
                        inventory,
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
                                "Making Friends with My Arm"
                        )
        );
    }

    @Test
    public void lockedFairyRingAccessIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        false,
                        false,
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
                                "fairy-ring access requires"
                        )
        );
    }

    @Test
    public void noStaffAndNoEliteRewardIsBlocked()
    {
        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        false,
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
    public void unknownBankRequiresDataWhenNoStaffIsCarried()
    {
        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        true,
                        false,
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
                                "DATA REQUIRED"
                        )
        );
    }

    @Test
    public void unfinishedQuestWinsOverUnknownBank()
    {
        RequirementResult result =
                evaluate(
                        QuestState.NOT_STARTED,
                        true,
                        false,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
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
                                "Making Friends with My Arm"
                        )
        );
    }

    private RequirementResult evaluate(
            QuestState questState,
            boolean fairyRingAccessUnlocked,
            boolean lumbridgeEliteClaimed,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
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

        return new WeissLarryBoatRouteRequirement()
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