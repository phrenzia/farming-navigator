package com.farmingnavigator.account;

import java.util.EnumSet;
import net.runelite.api.Quest;

public class AccountState
{
    private final boolean loggedIn;
    private final int realFarmingLevel;
    private final int boostedFarmingLevel;
    private final EnumSet<Quest> completedQuests;
    private final boolean morytaniaEliteRewardClaimed;

    public AccountState(
            boolean loggedIn,
            int realFarmingLevel,
            int boostedFarmingLevel,
            EnumSet<Quest> completedQuests,
            boolean morytaniaEliteRewardClaimed
    )
    {
        this.loggedIn = loggedIn;
        this.realFarmingLevel = realFarmingLevel;
        this.boostedFarmingLevel = boostedFarmingLevel;

        if (completedQuests == null || completedQuests.isEmpty())
        {
            this.completedQuests = EnumSet.noneOf(Quest.class);
        }
        else
        {
            this.completedQuests = EnumSet.copyOf(completedQuests);
        }

        this.morytaniaEliteRewardClaimed = morytaniaEliteRewardClaimed;
    }

    public static AccountState loggedOut()
    {
        return new AccountState(
                false,
                0,
                0,
                EnumSet.noneOf(Quest.class),
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

    public boolean hasCompletedQuest(Quest quest)
    {
        return quest != null && completedQuests.contains(quest);
    }

    public boolean isMorytaniaEliteRewardClaimed()
    {
        return morytaniaEliteRewardClaimed;
    }
}