package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.poh.PohHouseLocation;

public class PohHouseLocationRouteRequirement
        implements RouteRequirement
{
    private final PohHouseLocation
            requiredLocation;

    private final String displayName;

    private final AllOfRequirement
            completeRequirement;

    public PohHouseLocationRouteRequirement(
            PohHouseLocation requiredLocation
    )
    {
        if (
                requiredLocation == null
                        || requiredLocation
                        == PohHouseLocation.NONE
        )
        {
            throw new IllegalArgumentException(
                    "A usable POH house location is required."
            );
        }

        this.requiredLocation =
                requiredLocation;

        this.displayName =
                "POH route via "
                        + requiredLocation
                        .getDisplayName()
                        + " house location";

        this.completeRequirement =
                new AllOfRequirement(
                        displayName,
                        new PohHouseEntryRequirement(),
                        new PohHouseLocationRequirement(
                                requiredLocation
                        )
                );
    }

    public PohHouseLocation getRequiredLocation()
    {
        return requiredLocation;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
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