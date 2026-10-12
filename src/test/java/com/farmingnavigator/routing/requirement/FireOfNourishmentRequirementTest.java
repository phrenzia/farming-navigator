package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import java.util.EnumMap;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FireOfNourishmentRequirementTest
{
    @Test
    public void builtFireSatisfiesRequirement()
    {
        AccountState accountState =
                createAccountState(
                        true
                );

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        null
                );

        RequirementResult result =
                new FireOfNourishmentRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );
    }

    @Test
    public void missingFireBlocksRequirement()
    {
        AccountState accountState =
                createAccountState(
                        false
                );

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        null
                );

        RequirementResult result =
                new FireOfNourishmentRequirement()
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
                                "has not been built"
                        )
        );
    }

    @Test
    public void loggedOutStateRequiresData()
    {
        RequirementContext context =
                new RequirementContext(
                        AccountState.loggedOut(),
                        null
                );

        RequirementResult result =
                new FireOfNourishmentRequirement()
                        .evaluate(
                                context
                        );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );
    }

    private AccountState createAccountState(
            boolean fireOfNourishmentBuilt
    )
    {
        EnumMap<Quest, QuestState> quests =
                new EnumMap<>(
                        Quest.class
                );

        quests.put(
                Quest.MAKING_FRIENDS_WITH_MY_ARM,
                QuestState.FINISHED
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
                quests,
                null,
                null,
                fireOfNourishmentBuilt
        );
    }
}