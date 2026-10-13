package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
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

public class EctophialHarmonyRouteRequirementTest
{
    @Test
    public void carriedFilledEctophialWithQuestAccessIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.ECTOPHIAL,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        QuestState.IN_PROGRESS,
                        inventory,
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
    public void finishedGreatBrainRobberyAlsoSatisfiesRoute()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.ECTOPHIAL,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        QuestState.FINISHED,
                        inventory,
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
    public void bankedFilledEctophialIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.ECTOPHIAL,
                1L
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        QuestState.IN_PROGRESS,
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
                                "Filled Ectophial"
                        )
        );
    }

    @Test
    public void missingFilledEctophialIsBlocked()
    {
        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        QuestState.IN_PROGRESS,
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
    public void emptyEctophialDoesNotSatisfyRoute()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.ECTOPHIAL_EMPTY,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        QuestState.IN_PROGRESS,
                        inventory,
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
                                "Filled Ectophial"
                        )
        );
    }

    @Test
    public void cabinFeverMustBeFinished()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.ECTOPHIAL,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.IN_PROGRESS,
                        QuestState.IN_PROGRESS,
                        inventory,
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
                                "Cabin Fever"
                        )
        );
    }

    @Test
    public void greatBrainRobberyMustBeStarted()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.ECTOPHIAL,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        QuestState.NOT_STARTED,
                        inventory,
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
                                "The Great Brain Robbery"
                        )
        );
    }

    @Test
    public void unknownBankRequiresDataWhenNoFilledEctophialIsCarried()
    {
        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        QuestState.IN_PROGRESS,
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
                                "Bank verification required"
                        )
        );
    }

    @Test
    public void definiteQuestBlockerWinsOverUnknownBank()
    {
        RequirementResult result =
                evaluate(
                        QuestState.NOT_STARTED,
                        QuestState.NOT_STARTED,
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
    }

    private RequirementResult evaluate(
            QuestState cabinFeverState,
            QuestState greatBrainRobberyState,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.CABIN_FEVER,
                cabinFeverState
        );

        questStates.put(
                Quest.THE_GREAT_BRAIN_ROBBERY,
                greatBrainRobberyState
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
                        questStates,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.STANDARD,
                        null,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );

        StorageSnapshot storageSnapshot =
                new StorageSnapshot(
                        convertToLongMap(
                                inventory
                        ),
                        Collections.emptyMap(),
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

        return new EctophialHarmonyRouteRequirement()
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