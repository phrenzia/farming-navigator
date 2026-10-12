package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;

public class RequirementContext
{
    private final AccountState accountState;

    private final FarmingNavigatorConfig config;

    private final TravelCapabilitySnapshot
            travelCapabilitySnapshot;

    /*
     * Compatibility constructor.
     *
     * Existing requirements/tests created before live travel
     * capabilities were added can continue using the old signature.
     */
    public RequirementContext(
            AccountState accountState,
            FarmingNavigatorConfig config
    )
    {
        this(
                accountState,
                config,
                TravelCapabilitySnapshot
                        .loggedOut()
        );
    }

    public RequirementContext(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot
    )
    {
        this.accountState =
                accountState;

        this.config =
                config;

        this.travelCapabilitySnapshot =
                travelCapabilitySnapshot == null
                        ? TravelCapabilitySnapshot
                        .loggedOut()
                        : travelCapabilitySnapshot;
    }

    public AccountState getAccountState()
    {
        return accountState;
    }

    public FarmingNavigatorConfig getConfig()
    {
        return config;
    }

    public TravelCapabilitySnapshot
    getTravelCapabilitySnapshot()
    {
        return travelCapabilitySnapshot;
    }
}