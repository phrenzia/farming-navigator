package com.farmingnavigator.routing.requirement;

public class RequirementResult
{
    private final RequirementState state;

    private final RequirementReadiness readiness;

    private final String requirement;

    private final String detail;

    private RequirementResult(
            RequirementState state,
            RequirementReadiness readiness,
            String requirement,
            String detail
    )
    {
        this.state =
                state;

        this.readiness =
                readiness;

        this.requirement =
                requirement;

        this.detail =
                detail;
    }

    public static RequirementResult satisfied(
            String requirement
    )
    {
        return new RequirementResult(
                RequirementState.SATISFIED,
                RequirementReadiness.READY_NOW,
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
                RequirementReadiness.READY_NOW,
                requirement,
                detail
        );
    }

    public static RequirementResult availableFromBank(
            String requirement,
            String detail
    )
    {
        return new RequirementResult(
                RequirementState.SATISFIED,
                RequirementReadiness.AVAILABLE_FROM_BANK,
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
                RequirementReadiness.NOT_AVAILABLE,
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
                RequirementReadiness.NOT_AVAILABLE,
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
                RequirementReadiness.NOT_AVAILABLE,
                requirement,
                detail
        );
    }

    public RequirementState getState()
    {
        return state;
    }

    public RequirementReadiness getReadiness()
    {
        return readiness;
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

    public boolean isReadyNow()
    {
        return isSatisfied()
                && readiness.isReadyNow();
    }

    public boolean requiresBankWithdrawal()
    {
        return isSatisfied()
                && readiness.requiresBankWithdrawal();
    }
}