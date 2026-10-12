package com.farmingnavigator.routing.requirement;

public class RequirementResult
{
    private final RequirementState state;

    private final String requirement;

    private final String detail;

    private RequirementResult(
            RequirementState state,
            String requirement,
            String detail
    )
    {
        this.state = state;
        this.requirement = requirement;
        this.detail = detail;
    }

    public static RequirementResult satisfied(
            String requirement
    )
    {
        return new RequirementResult(
                RequirementState.SATISFIED,
                requirement,
                "Requirement satisfied."
        );
    }

    public static RequirementResult satisfied(
            String requirement,
            String detail
    )
    {
        return new RequirementResult(
                RequirementState.SATISFIED,
                requirement,
                detail
        );
    }

    public static RequirementResult blocked(
            String requirement,
            String detail
    )
    {
        return new RequirementResult(
                RequirementState.BLOCKED,
                requirement,
                detail
        );
    }

    public static RequirementResult configurationRequired(
            String requirement,
            String detail
    )
    {
        return new RequirementResult(
                RequirementState.CONFIGURATION_REQUIRED,
                requirement,
                detail
        );
    }

    public static RequirementResult dataRequired(
            String requirement,
            String detail
    )
    {
        return new RequirementResult(
                RequirementState.DATA_REQUIRED,
                requirement,
                detail
        );
    }

    public RequirementState getState()
    {
        return state;
    }

    public String getRequirement()
    {
        return requirement;
    }

    public String getDetail()
    {
        return detail;
    }

    public boolean isSatisfied()
    {
        return state.isSatisfied();
    }
}