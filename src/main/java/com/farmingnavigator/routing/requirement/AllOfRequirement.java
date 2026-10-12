package com.farmingnavigator.routing.requirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class AllOfRequirement
        implements RouteRequirement
{
    private final String displayName;

    private final List<RouteRequirement> requirements;

    public AllOfRequirement(
            String displayName,
            RouteRequirement... requirements
    )
    {
        this.displayName =
                displayName;

        this.requirements =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                Arrays.asList(
                                        requirements
                                )
                        )
                );
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    public List<RouteRequirement>
    getRequirements()
    {
        return requirements;
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        StringJoiner blockers =
                new StringJoiner(
                        " "
                );

        for (
                RouteRequirement requirement
                : requirements
        )
        {
            RequirementResult result =
                    requirement.evaluate(
                            context
                    );

            if (!result.isSatisfied())
            {
                blockers.add(
                        result.getDetail()
                );
            }
        }

        if (
                blockers.length() == 0
        )
        {
            return RequirementResult.satisfied(
                    displayName
            );
        }

        return RequirementResult.blocked(
                displayName,
                blockers.toString()
        );
    }
}