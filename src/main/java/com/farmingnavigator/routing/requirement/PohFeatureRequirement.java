package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;

public class PohFeatureRequirement
        implements RouteRequirement
{
    private final PohFeature feature;

    public PohFeatureRequirement(
            PohFeature feature
    )
    {
        this.feature = feature;
    }

    @Override
    public String getDisplayName()
    {
        return feature.getDisplayName();
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
                    "Configure your POH profile in Farming Navigator."
            );
        }

        FarmingNavigatorConfig config =
                context.getConfig();

        if (
                feature.isPresent(
                        config
                )
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName()
            );
        }

        return RequirementResult.blocked(
                getDisplayName(),
                getDisplayName()
                        + " is not enabled in your Farming Navigator POH profile."
        );
    }
}