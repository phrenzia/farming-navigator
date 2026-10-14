package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.poh.PohHouseLocation;

public class PohHouseLocationRequirement
        implements RouteRequirement
{
    private final PohHouseLocation
            requiredLocation;

    private final String displayName;

    public PohHouseLocationRequirement(
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
                "POH location: "
                        + requiredLocation
                        .getDisplayName();
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

        PohHouseLocation currentLocation =
                config.pohHouseLocation();

        if (currentLocation == null)
        {
            return RequirementResult
                    .configurationRequired(
                            displayName,
                            "Configure your POH house location in Farming Navigator."
                    );
        }

        if (
                currentLocation
                        == requiredLocation
        )
        {
            return RequirementResult.satisfied(
                    displayName,
                    "READY NOW - POH is configured in "
                            + requiredLocation
                            .getDisplayName()
                            + "."
            );
        }

        if (
                currentLocation
                        == PohHouseLocation.NONE
        )
        {
            return RequirementResult.blocked(
                    displayName,
                    "MISSING - no player-owned house is configured. "
                            + "This route requires a POH in "
                            + requiredLocation
                            .getDisplayName()
                            + "."
            );
        }

        return RequirementResult.blocked(
                displayName,
                "MISSING - POH is configured in "
                        + currentLocation
                        .getDisplayName()
                        + ", but this route requires "
                        + requiredLocation
                        .getDisplayName()
                        + "."
        );
    }
}