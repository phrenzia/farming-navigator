package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.poh.PohPortalDestination;
import java.util.Set;

public class PohPortalDestinationRequirement
        implements RouteRequirement
{
    private final PohPortalDestination destination;

    private final String displayName;

    public PohPortalDestinationRequirement(
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
                "POH portal destination: "
                        + destination.getDisplayName();
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
        if (
                context == null
                        || context.getConfig() == null
        )
        {
            return RequirementResult
                    .configurationRequired(
                            displayName,
                            "Configure your POH profile in Farming Navigator."
                    );
        }

        FarmingNavigatorConfig config =
                context.getConfig();

        Set<PohPortalDestination> destinations =
                config.pohPortalDestinations();

        if (
                destinations != null
                        && destinations.contains(
                        destination
                )
        )
        {
            return RequirementResult.satisfied(
                    displayName,
                    "READY NOW - "
                            + destination.getDisplayName()
                            + " is installed as a POH portal destination."
            );
        }

        return RequirementResult.blocked(
                displayName,
                "MISSING - "
                        + destination.getDisplayName()
                        + " is not configured as an installed POH portal destination."
        );
    }
}