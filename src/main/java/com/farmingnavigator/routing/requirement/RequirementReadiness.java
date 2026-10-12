package com.farmingnavigator.routing.requirement;

public enum RequirementReadiness
{
    READY_NOW,
    AVAILABLE_FROM_BANK,
    NOT_AVAILABLE;

    public boolean isReadyNow()
    {
        return this == READY_NOW;
    }

    public boolean requiresBankWithdrawal()
    {
        return this == AVAILABLE_FROM_BANK;
    }
}