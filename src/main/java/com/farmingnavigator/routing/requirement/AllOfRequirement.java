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

        RequirementState strongestFailure =
                RequirementState.BLOCKED;

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

            if (
                    result.getState()
                            == RequirementState.DATA_REQUIRED
            )
            {
                strongestFailure =
                        RequirementState.DATA_REQUIRED;
            }
            else if (
                    result.getState()
                            == RequirementState.CONFIGURATION_REQUIRED
                            && strongestFailure
                            != RequirementState.DATA_REQUIRED
            )
            {
                strongestFailure =
                        RequirementState.CONFIGURATION_REQUIRED;
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
                return RequirementResult.availableFromBank(
                        displayName,
                        preparationSteps.toString()
                );
            }

            return RequirementResult.satisfied(
                    displayName
            );
        }

        switch (strongestFailure)
        {
            case DATA_REQUIRED:
                return RequirementResult.dataRequired(
                        displayName,
                        blockers.toString()
                );

            case CONFIGURATION_REQUIRED:
                return RequirementResult.configurationRequired(
                        displayName,
                        blockers.toString()
                );

            case BLOCKED:
            case SATISFIED:
            default:
                return RequirementResult.blocked(
                        displayName,
                        blockers.toString()
                );
        }
    }
}