package com.farmingnavigator.routing.requirement;

public enum RequirementState
{
    SATISFIED,
    BLOCKED,
    CONFIGURATION_REQUIRED,
    DATA_REQUIRED;

    public boolean isSatisfied()
    {
        return this == SATISFIED;
    }
}