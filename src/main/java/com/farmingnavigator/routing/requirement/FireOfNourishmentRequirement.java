package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;

public class FireOfNourishmentRequirement
        implements RouteRequirement
{
    @Override
    public String getDisplayName()
    {
        return "Fire of Nourishment built";
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        if (
                context == null
                        || context.getAccountState()
                        == null
                        || !context
                        .getAccountState()
                        .isLoggedIn()
        )
        {
            return RequirementResult.dataRequired(
                    getDisplayName(),
                    "Log in so Farming Navigator can verify the Fire of Nourishment."
            );
        }

        AccountState accountState =
                context.getAccountState();

        if (
                accountState
                        .isFireOfNourishmentBuilt()
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    "Fire of Nourishment is built in Weiss."
            );
        }

        return RequirementResult.blocked(
                getDisplayName(),
                "The Fire of Nourishment has not been built in Weiss. "
                        + "Build it before using the Weiss herb patch."
        );
    }
}