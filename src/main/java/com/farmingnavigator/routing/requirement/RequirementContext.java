package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;

public class RequirementContext
{
    private final AccountState accountState;

    private final FarmingNavigatorConfig config;

    public RequirementContext(
            AccountState accountState,
            FarmingNavigatorConfig config
    )
    {
        this.accountState =
                accountState;

        this.config =
                config;
    }

    public AccountState getAccountState()
    {
        return accountState;
    }

    public FarmingNavigatorConfig getConfig()
    {
        return config;
    }
}