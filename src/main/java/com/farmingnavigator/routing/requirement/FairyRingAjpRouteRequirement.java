package com.farmingnavigator.routing.requirement;

public class FairyRingAjpRouteRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Fairy ring AJP to Varlamore";

    private final AllOfRequirement
            completeRequirement;

    public FairyRingAjpRouteRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        new FairyRingAccessRequirement(),
                        new VarlamoreVisitedRequirement()
                );
    }

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
        return completeRequirement.evaluate(
                context
        );
    }
}