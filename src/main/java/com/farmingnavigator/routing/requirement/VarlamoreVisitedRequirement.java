package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;

public class VarlamoreVisitedRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Varlamore first-visit access";

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
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
                    DISPLAY_NAME,
                    "DATA REQUIRED - log in so Farming Navigator can verify whether Varlamore has been visited."
            );
        }

        AccountState accountState =
                context.getAccountState();

        if (
                !accountState
                        .isVarlamoreVisited()
        )
        {
            return RequirementResult.blocked(
                    DISPLAY_NAME,
                    "LOCKED - fairy ring AJP requires first travelling to Civitas illa Fortis via Regulus Cento after completing Children of the Sun."
            );
        }

        return RequirementResult.satisfied(
                DISPLAY_NAME,
                "READY NOW - Varlamore has been visited at least once."
        );
    }
}