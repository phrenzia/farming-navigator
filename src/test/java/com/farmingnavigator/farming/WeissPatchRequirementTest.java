package com.farmingnavigator.farming;

import com.farmingnavigator.account.AccountState;
import java.util.EnumMap;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeissPatchRequirementTest
{
    @Test
    public void questCompletionWithoutFireDoesNotUnlockWeiss()
    {
        AccountState accountState =
                createAccountState(
                        true,
                        false
                );

        HerbPatchStatus status =
                findWeissStatus(
                        new HerbPatchService()
                                .evaluate(
                                        accountState
                                )
                );

        assertFalse(
                status.isUnlocked()
        );

        assertTrue(
                status
                        .getMissingRequirements()
                        .contains(
                                "Fire of Nourishment built"
                        )
        );
    }

    @Test
    public void questAndBuiltFireUnlockWeiss()
    {
        AccountState accountState =
                createAccountState(
                        true,
                        true
                );

        HerbPatchStatus status =
                findWeissStatus(
                        new HerbPatchService()
                                .evaluate(
                                        accountState
                                )
                );

        assertTrue(
                status.isUnlocked()
        );
    }

    @Test
    public void missingQuestAndFireReportsBothRequirements()
    {
        AccountState accountState =
                createAccountState(
                        false,
                        false
                );

        HerbPatchStatus status =
                findWeissStatus(
                        new HerbPatchService()
                                .evaluate(
                                        accountState
                                )
                );

        assertFalse(
                status.isUnlocked()
        );

        assertTrue(
                status
                        .getMissingRequirements()
                        .contains(
                                "Quest: Making Friends with My Arm"
                        )
        );

        assertTrue(
                status
                        .getMissingRequirements()
                        .contains(
                                "Fire of Nourishment built"
                        )
        );
    }

    private AccountState createAccountState(
            boolean questComplete,
            boolean fireBuilt
    )
    {
        EnumMap<Quest, QuestState> quests =
                new EnumMap<>(
                        Quest.class
                );

        if (questComplete)
        {
            quests.put(
                    Quest.MAKING_FRIENDS_WITH_MY_ARM,
                    QuestState.FINISHED
            );
        }

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
                fireBuilt
        );
    }

    private HerbPatchStatus findWeissStatus(
            List<HerbPatchStatus> statuses
    )
    {
        for (
                HerbPatchStatus status
                : statuses
        )
        {
            if (
                    status.getPatch()
                            == HerbPatchDefinition.WEISS
            )
            {
                return status;
            }
        }

        throw new AssertionError(
                "Weiss patch status was not produced."
        );
    }
}