package com.farmingnavigator.routing.tablet;

import com.farmingnavigator.routing.requirement.AllOfRequirement;
import com.farmingnavigator.routing.requirement.BankBackedItemRequirement;
import com.farmingnavigator.routing.requirement.PohFeature;
import com.farmingnavigator.routing.requirement.PohFeatureRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RouteRequirement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeleportTabletRequirement
        implements RouteRequirement
{
    private final TeleportTabletDefinition definition;

    private final List<RouteRequirement>
            componentRequirements;

    private final AllOfRequirement
            completeRequirement;

    public TeleportTabletRequirement(
            TeleportTabletDefinition definition
    )
    {
        if (definition == null)
        {
            throw new IllegalArgumentException(
                    "Teleport tablet definition cannot be null."
            );
        }

        this.definition =
                definition;

        List<RouteRequirement> requirements =
                new ArrayList<>();

        if (
                definition
                        .requiresConfiguredHouse()
        )
        {
            requirements.add(
                    new PohFeatureRequirement(
                            PohFeature.HOUSE
                    )
            );
        }

        requirements.add(
                new BankBackedItemRequirement(
                        definition
                                .getDisplayName(),
                        definition
                                .getItemId(),
                        definition
                                .getDisplayName(),
                        1
                )
        );

        this.componentRequirements =
                Collections.unmodifiableList(
                        requirements
                );

        this.completeRequirement =
                new AllOfRequirement(
                        definition
                                .getDisplayName(),
                        requirements.toArray(
                                new RouteRequirement[0]
                        )
                );
    }

    public TeleportTabletDefinition
    getDefinition()
    {
        return definition;
    }

    public List<RouteRequirement>
    getComponentRequirements()
    {
        return componentRequirements;
    }

    @Override
    public String getDisplayName()
    {
        return definition
                .getDisplayName();
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