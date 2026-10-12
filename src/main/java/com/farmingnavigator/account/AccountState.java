package com.farmingnavigator.account;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

public class AccountState
{
    private final boolean loggedIn;

    private final int realFarmingLevel;
    private final int boostedFarmingLevel;

    private final int realMagicLevel;
    private final int boostedMagicLevel;

    private final int realAgilityLevel;
    private final int boostedAgilityLevel;

    private final int realHunterLevel;
    private final int boostedHunterLevel;

    private final EnumMap<Quest, QuestState> questStates;

    private final EnumMap<DiaryArea, DiaryTier>
            completedDiaryTiers;

    private final EnumMap<DiaryArea, DiaryTier>
            claimedDiaryTiers;

    private final boolean fireOfNourishmentBuilt;

    /*
     * Full constructor including verified account unlock state.
     */
    public AccountState(
            boolean loggedIn,
            int realFarmingLevel,
            int boostedFarmingLevel,
            int realMagicLevel,
            int boostedMagicLevel,
            int realAgilityLevel,
            int boostedAgilityLevel,
            int realHunterLevel,
            int boostedHunterLevel,
            Map<Quest, QuestState> questStates,
            Map<DiaryArea, DiaryTier> completedDiaryTiers,
            Map<DiaryArea, DiaryTier> claimedDiaryTiers,
            boolean fireOfNourishmentBuilt
    )
    {
        this.loggedIn =
                loggedIn;

        this.realFarmingLevel =
                realFarmingLevel;

        this.boostedFarmingLevel =
                boostedFarmingLevel;

        this.realMagicLevel =
                realMagicLevel;

        this.boostedMagicLevel =
                boostedMagicLevel;

        this.realAgilityLevel =
                realAgilityLevel;

        this.boostedAgilityLevel =
                boostedAgilityLevel;

        this.realHunterLevel =
                realHunterLevel;

        this.boostedHunterLevel =
                boostedHunterLevel;

        this.questStates =
                new EnumMap<>(
                        Quest.class
                );

        if (questStates != null)
        {
            this.questStates.putAll(
                    questStates
            );
        }

        this.completedDiaryTiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (completedDiaryTiers != null)
        {
            this.completedDiaryTiers.putAll(
                    completedDiaryTiers
            );
        }

        this.claimedDiaryTiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (claimedDiaryTiers != null)
        {
            this.claimedDiaryTiers.putAll(
                    claimedDiaryTiers
            );
        }

        this.fireOfNourishmentBuilt =
                fireOfNourishmentBuilt;
    }

    /*
     * Compatibility constructor used by existing code and tests
     * created before exact Weiss fire-state tracking was added.
     */
    public AccountState(
            boolean loggedIn,
            int realFarmingLevel,
            int boostedFarmingLevel,
            int realMagicLevel,
            int boostedMagicLevel,
            int realAgilityLevel,
            int boostedAgilityLevel,
            int realHunterLevel,
            int boostedHunterLevel,
            Map<Quest, QuestState> questStates,
            Map<DiaryArea, DiaryTier> completedDiaryTiers,
            Map<DiaryArea, DiaryTier> claimedDiaryTiers
    )
    {
        this(
                loggedIn,
                realFarmingLevel,
                boostedFarmingLevel,
                realMagicLevel,
                boostedMagicLevel,
                realAgilityLevel,
                boostedAgilityLevel,
                realHunterLevel,
                boostedHunterLevel,
                questStates,
                completedDiaryTiers,
                claimedDiaryTiers,
                false
        );
    }

    /*
     * Original compatibility constructor.
     */
    public AccountState(
            boolean loggedIn,
            int realFarmingLevel,
            int boostedFarmingLevel,
            EnumSet<Quest> completedQuests,
            boolean morytaniaEliteRewardClaimed
    )
    {
        this(
                loggedIn,
                realFarmingLevel,
                boostedFarmingLevel,
                0,
                0,
                0,
                0,
                0,
                0,
                createLegacyQuestStates(
                        completedQuests
                ),
                createLegacyCompletedDiaryTiers(
                        morytaniaEliteRewardClaimed
                ),
                createLegacyClaimedDiaryTiers(
                        morytaniaEliteRewardClaimed
                ),
                false
        );
    }

    public static AccountState loggedOut()
    {
        return new AccountState(
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                null,
                null,
                null,
                false
        );
    }

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    public int getRealFarmingLevel()
    {
        return realFarmingLevel;
    }

    public int getBoostedFarmingLevel()
    {
        return boostedFarmingLevel;
    }

    public int getRealMagicLevel()
    {
        return realMagicLevel;
    }

    public int getBoostedMagicLevel()
    {
        return boostedMagicLevel;
    }

    public int getRealAgilityLevel()
    {
        return realAgilityLevel;
    }

    public int getBoostedAgilityLevel()
    {
        return boostedAgilityLevel;
    }

    public int getRealHunterLevel()
    {
        return realHunterLevel;
    }

    public int getBoostedHunterLevel()
    {
        return boostedHunterLevel;
    }

    public QuestState getQuestState(
            Quest quest
    )
    {
        if (quest == null)
        {
            return QuestState.NOT_STARTED;
        }

        return questStates.getOrDefault(
                quest,
                QuestState.NOT_STARTED
        );
    }

    public boolean hasStartedQuest(
            Quest quest
    )
    {
        return getQuestState(
                quest
        ) != QuestState.NOT_STARTED;
    }

    public boolean isQuestInProgress(
            Quest quest
    )
    {
        return getQuestState(
                quest
        ) == QuestState.IN_PROGRESS;
    }

    public boolean hasCompletedQuest(
            Quest quest
    )
    {
        return getQuestState(
                quest
        ) == QuestState.FINISHED;
    }

    public DiaryTier getCompletedDiaryTier(
            DiaryArea area
    )
    {
        if (area == null)
        {
            return DiaryTier.NONE;
        }

        return completedDiaryTiers.getOrDefault(
                area,
                DiaryTier.NONE
        );
    }

    public DiaryTier getClaimedDiaryTier(
            DiaryArea area
    )
    {
        if (area == null)
        {
            return DiaryTier.NONE;
        }

        return claimedDiaryTiers.getOrDefault(
                area,
                DiaryTier.NONE
        );
    }

    public boolean hasCompletedDiaryTier(
            DiaryArea area,
            DiaryTier requiredTier
    )
    {
        return getCompletedDiaryTier(
                area
        )
                .isAtLeast(
                        requiredTier
                );
    }

    public boolean hasClaimedDiaryTier(
            DiaryArea area,
            DiaryTier requiredTier
    )
    {
        return getClaimedDiaryTier(
                area
        )
                .isAtLeast(
                        requiredTier
                );
    }

    public boolean isDiaryRewardClaimPending(
            DiaryArea area,
            DiaryTier tier
    )
    {
        return hasCompletedDiaryTier(
                area,
                tier
        )
                && !hasClaimedDiaryTier(
                area,
                tier
        );
    }

    public boolean isMorytaniaEliteRewardClaimed()
    {
        return hasClaimedDiaryTier(
                DiaryArea.MORYTANIA,
                DiaryTier.ELITE
        );
    }

    public boolean isFireOfNourishmentBuilt()
    {
        return fireOfNourishmentBuilt;
    }

    private static Map<Quest, QuestState>
    createLegacyQuestStates(
            EnumSet<Quest> completedQuests
    )
    {
        EnumMap<Quest, QuestState> states =
                new EnumMap<>(
                        Quest.class
                );

        if (completedQuests != null)
        {
            for (
                    Quest quest
                    : completedQuests
            )
            {
                states.put(
                        quest,
                        QuestState.FINISHED
                );
            }
        }

        return states;
    }

    private static Map<DiaryArea, DiaryTier>
    createLegacyCompletedDiaryTiers(
            boolean morytaniaEliteRewardClaimed
    )
    {
        EnumMap<DiaryArea, DiaryTier> tiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (morytaniaEliteRewardClaimed)
        {
            tiers.put(
                    DiaryArea.MORYTANIA,
                    DiaryTier.ELITE
            );
        }

        return tiers;
    }

    private static Map<DiaryArea, DiaryTier>
    createLegacyClaimedDiaryTiers(
            boolean morytaniaEliteRewardClaimed
    )
    {
        EnumMap<DiaryArea, DiaryTier> tiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (morytaniaEliteRewardClaimed)
        {
            tiers.put(
                    DiaryArea.MORYTANIA,
                    DiaryTier.ELITE
            );
        }

        return tiers;
    }
}