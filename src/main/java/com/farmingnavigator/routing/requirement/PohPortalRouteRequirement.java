package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.poh.PohPortalDestination;

public class PohPortalRouteRequirement
        implements RouteRequirement
{
    private final PohPortalDestination destination;

    private final String displayName;

    private final AllOfRequirement
            completeRequirement;

    public PohPortalRouteRequirement(
            PohPortalDestination destination
    )
    {
        if (destination == null)
        {
            throw new IllegalArgumentException(
                    "POH portal destination cannot be null."
            );
        }

        this.destination =
                destination;

        this.displayName =
                "POH portal route to "
                        + destination.getDisplayName();

        this.completeRequirement =
                new AllOfRequirement(
                        displayName,
                        new PohHouseEntryRequirement(),
                        new PohPortalDestinationRequirement(
                                destination
                        )
                );
    }

    public PohPortalDestination getDestination()
    {
        return destination;
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