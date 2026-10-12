package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

public class QuestRequirement
        implements RouteRequirement
{
    private final Quest quest;

    private final QuestState requiredState;

    public QuestRequirement(
            Quest quest
    )
    {
        this(
                quest,
                QuestState.FINISHED
        );
    }

    public QuestRequirement(
            Quest quest,
            QuestState requiredState
    )
    {
        this.quest = quest;
        this.requiredState =
                requiredState;
    }

    @Override
    public String getDisplayName()
    {
        if (
                requiredState
                        == QuestState.FINISHED
        )
        {
            return "Quest: "
                    + quest.getName();
        }

        if (
                requiredState
                        == QuestState.IN_PROGRESS
        )
        {
            return "Started quest: "
                    + quest.getName();
        }

        return "Quest state: "
                + quest.getName();
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        if (
                context == null
                        || context.getAccountState() == null
                        || !context
                        .getAccountState()
                        .isLoggedIn()
        )
        {
            return RequirementResult.dataRequired(
                    getDisplayName(),
                    "Log in so Farming Navigator can verify quest progress."
            );
        }

        AccountState accountState =
                context.getAccountState();

        QuestState currentState =
                accountState.getQuestState(
                        quest
                );

        boolean satisfied;

        switch (requiredState)
        {
            case FINISHED:
                satisfied =
                        currentState
                                == QuestState.FINISHED;
                break;

            case IN_PROGRESS:
                satisfied =
                        currentState
                                == QuestState.IN_PROGRESS
                                || currentState
                                == QuestState.FINISHED;
                break;

            case NOT_STARTED:
            default:
                satisfied =
                        currentState
                                == QuestState.NOT_STARTED;
                break;
        }

        if (satisfied)
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    quest.getName()
                            + " state verified as "
                            + currentState
                            + "."
            );
        }

        return RequirementResult.blocked(
                getDisplayName(),
                "Requires "
                        + formatRequiredState()
                        + " "
                        + quest.getName()
                        + ". Current state: "
                        + currentState
                        + "."
        );
    }

    private String formatRequiredState()
    {
        switch (requiredState)
        {
            case FINISHED:
                return "completion of";

            case IN_PROGRESS:
                return "starting";

            case NOT_STARTED:
            default:
                return "the unstarted state for";
        }
    }
}