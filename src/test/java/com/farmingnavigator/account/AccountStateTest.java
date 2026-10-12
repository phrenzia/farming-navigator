package com.farmingnavigator.account;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountStateTest
{
    @Test
    public void tracksQuestProgressDiaryProgressAndSkills()
    {
        Map<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.TWILIGHTS_PROMISE,
                QuestState.FINISHED
        );

        questStates.put(
                Quest.FAIRYTALE_II__CURE_A_QUEEN,
                QuestState.IN_PROGRESS
        );

        Map<DiaryArea, DiaryTier> completed =
                new EnumMap<>(
                        DiaryArea.class
                );

        completed.put(
                DiaryArea.KOUREND_AND_KEBOS,
                DiaryTier.HARD
        );

        completed.put(
                DiaryArea.MORYTANIA,
                DiaryTier.ELITE
        );

        Map<DiaryArea, DiaryTier> claimed =
                new EnumMap<>(
                        DiaryArea.class
                );

        claimed.put(
                DiaryArea.KOUREND_AND_KEBOS,
                DiaryTier.MEDIUM
        );

        claimed.put(
                DiaryArea.MORYTANIA,
                DiaryTier.ELITE
        );

        AccountState state =
                new AccountState(
                        true,
                        80,
                        83,
                        70,
                        75,
                        73,
                        73,
                        60,
                        63,
                        questStates,
                        completed,
                        claimed
                );

        assertTrue(
                state.hasCompletedQuest(
                        Quest.TWILIGHTS_PROMISE
                )
        );

        assertTrue(
                state.hasStartedQuest(
                        Quest.FAIRYTALE_II__CURE_A_QUEEN
                )
        );

        assertTrue(
                state.isQuestInProgress(
                        Quest.FAIRYTALE_II__CURE_A_QUEEN
                )
        );

        assertFalse(
                state.hasCompletedQuest(
                        Quest.FAIRYTALE_II__CURE_A_QUEEN
                )
        );

        assertTrue(
                state.hasCompletedDiaryTier(
                        DiaryArea.KOUREND_AND_KEBOS,
                        DiaryTier.HARD
                )
        );

        assertTrue(
                state.hasClaimedDiaryTier(
                        DiaryArea.KOUREND_AND_KEBOS,
                        DiaryTier.MEDIUM
                )
        );

        assertFalse(
                state.hasClaimedDiaryTier(
                        DiaryArea.KOUREND_AND_KEBOS,
                        DiaryTier.HARD
                )
        );

        assertTrue(
                state.isDiaryRewardClaimPending(
                        DiaryArea.KOUREND_AND_KEBOS,
                        DiaryTier.HARD
                )
        );

        assertTrue(
                state.isMorytaniaEliteRewardClaimed()
        );

        assertEquals(
                80,
                state.getRealFarmingLevel()
        );

        assertEquals(
                75,
                state.getBoostedMagicLevel()
        );

        assertEquals(
                73,
                state.getRealAgilityLevel()
        );

        assertEquals(
                63,
                state.getBoostedHunterLevel()
        );
    }

    @Test
    public void missingStateDefaultsAreSafe()
    {
        AccountState state =
                new AccountState(
                        true,
                        41,
                        41,
                        50,
                        50,
                        30,
                        30,
                        20,
                        20,
                        null,
                        null,
                        null
                );

        assertEquals(
                QuestState.NOT_STARTED,
                state.getQuestState(
                        Quest.LUNAR_DIPLOMACY
                )
        );

        assertEquals(
                DiaryTier.NONE,
                state.getCompletedDiaryTier(
                        DiaryArea.KANDARIN
                )
        );

        assertEquals(
                DiaryTier.NONE,
                state.getClaimedDiaryTier(
                        DiaryArea.KANDARIN
                )
        );

        assertFalse(
                state.hasCompletedQuest(
                        Quest.LUNAR_DIPLOMACY
                )
        );

        assertFalse(
                state.hasClaimedDiaryTier(
                        DiaryArea.KANDARIN,
                        DiaryTier.MEDIUM
                )
        );
    }
}