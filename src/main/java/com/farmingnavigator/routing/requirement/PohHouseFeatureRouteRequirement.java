package com.farmingnavigator.routing.requirement;

public class PohHouseFeatureRouteRequirement
        implements RouteRequirement
{
    private final PohFeature feature;

    private final String displayName;

    private final AllOfRequirement
            completeRequirement;

    public PohHouseFeatureRouteRequirement(
            PohFeature feature
    )
    {
        if (feature == null)
        {
            throw new IllegalArgumentException(
                    "POH feature cannot be null."
            );
        }

        this.feature =
                feature;

        this.displayName =
                "POH route via "
                        + feature.getDisplayName();

        this.completeRequirement =
                new AllOfRequirement(
                        displayName,
                        new PohHouseEntryRequirement(),
                        new PohFeatureRequirement(
                                feature
                        )
                );
    }

    public PohFeature getFeature()
    {
        return feature;
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