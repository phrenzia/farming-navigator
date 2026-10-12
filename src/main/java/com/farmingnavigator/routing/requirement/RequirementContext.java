package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;

public class RequirementContext
{
    private final AccountState accountState;

    private final FarmingNavigatorConfig config;

    private final TravelCapabilitySnapshot
            travelCapabilitySnapshot;

    private final StorageSnapshot
            storageSnapshot;

    /*
     * Compatibility constructor for the original
     * account/config-only requirement tests.
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
                        .loggedOut(),
                null
        );
    }

    /*
     * Compatibility constructor for the live-travel
     * capability milestone.
     */
    public RequirementContext(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot
    )
    {
        this(
                accountState,
                config,
                travelCapabilitySnapshot,
                null
        );
    }

    public RequirementContext(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot
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

        this.storageSnapshot =
                storageSnapshot;
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

    public StorageSnapshot getStorageSnapshot()
    {
        return storageSnapshot;
    }
}