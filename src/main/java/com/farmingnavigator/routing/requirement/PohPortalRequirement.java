package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.poh.PohPortalDestination;
import java.util.Set;

public class PohPortalRequirement
        implements RouteRequirement
{
    private final PohPortalDestination destination;

    public PohPortalRequirement(
            PohPortalDestination destination
    )
    {
        this.destination =
                destination;
    }

    @Override
    public String getDisplayName()
    {
        return "POH portal: "
                + destination.getDisplayName();
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
            return RequirementResult.configurationRequired(
                    getDisplayName(),
                    "Configure your POH portal destinations in Farming Navigator."
            );
        }

        FarmingNavigatorConfig config =
                context.getConfig();

        if (
                !config
                        .pohHouseLocation()
                        .hasHouse()
        )
        {
            return RequirementResult.blocked(
                    getDisplayName(),
                    "No player-owned house is configured."
            );
        }

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
                    getDisplayName()
            );
        }

        return RequirementResult.blocked(
                getDisplayName(),
                destination.getDisplayName()
                        + " is not selected as an installed POH portal destination."
        );
    }
}