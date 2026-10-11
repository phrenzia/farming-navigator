package com.farmingnavigator.account;

import java.util.EnumSet;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

public class AccountStateService
{
    private static final Quest[] RELEVANT_QUESTS = {
            Quest.PRIEST_IN_PERIL,
            Quest.MY_ARMS_BIG_ADVENTURE,
            Quest.MAKING_FRIENDS_WITH_MY_ARM,
            Quest.CHILDREN_OF_THE_SUN
    };

    private final Client client;

    @Inject
    public AccountStateService(Client client)
    {
        this.client = client;
    }

    public AccountState createSnapshot()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return AccountState.loggedOut();
        }

        int realFarmingLevel = client.getRealSkillLevel(Skill.FARMING);
        int boostedFarmingLevel = client.getBoostedSkillLevel(Skill.FARMING);

        EnumSet<Quest> completedQuests = EnumSet.noneOf(Quest.class);

        for (Quest quest : RELEVANT_QUESTS)
        {
            if (quest.getState(client) == QuestState.FINISHED)
            {
                completedQuests.add(quest);
            }
        }

        boolean morytaniaEliteRewardClaimed =
                client.getVarbitValue(VarbitID.MORYTANIA_ELITE_REWARD) > 0;

        return new AccountState(
                true,
                realFarmingLevel,
                boostedFarmingLevel,
                completedQuests,
                morytaniaEliteRewardClaimed
        );
    }
}