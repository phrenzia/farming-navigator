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

        StringJoiner preparationSteps =
                new StringJoiner(
                        " "
                );

        boolean hasBlockedRequirement =
                false;

        boolean hasDataRequired =
                false;

        boolean hasConfigurationRequired =
                false;

        for (
                RouteRequirement requirement
                : requirements
        )
        {
            RequirementResult result =
                    requirement.evaluate(
                            context
                    );

            if (result.isSatisfied())
            {
                if (
                        result
                                .requiresBankWithdrawal()
                )
                {
                    preparationSteps.add(
                            result.getDetail()
                    );
                }

                continue;
            }

            blockers.add(
                    result.getDetail()
            );

            switch (
                    result.getState()
            )
            {
                case BLOCKED:
                    hasBlockedRequirement =
                            true;
                    break;

                case DATA_REQUIRED:
                    hasDataRequired =
                            true;
                    break;

                case CONFIGURATION_REQUIRED:
                    hasConfigurationRequired =
                            true;
                    break;

                case SATISFIED:
                default:
                    break;
            }
        }

        if (
                blockers.length() == 0
        )
        {
            if (
                    preparationSteps.length() > 0
            )
            {
                return RequirementResult
                        .availableFromBank(
                                displayName,
                                preparationSteps
                                        .toString()
                        );
            }

            return RequirementResult.satisfied(
                    displayName
            );
        }

        /*
         * ALL OF means every requirement must pass.
         *
         * If even one requirement is definitively blocked,
         * the complete route is definitively blocked too.
         *
         * Missing data only wins when there is no known
         * blocker.
         */
        if (hasBlockedRequirement)
        {
            return RequirementResult.blocked(
                    displayName,
                    blockers.toString()
            );
        }

        if (hasDataRequired)
        {
            return RequirementResult.dataRequired(
                    displayName,
                    blockers.toString()
            );
        }

        if (hasConfigurationRequired)
        {
            return RequirementResult
                    .configurationRequired(
                            displayName,
                            blockers.toString()
                    );
        }

        return RequirementResult.blocked(
                displayName,
                blockers.toString()
        );
    }
}