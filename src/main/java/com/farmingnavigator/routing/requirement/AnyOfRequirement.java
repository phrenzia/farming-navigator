package com.farmingnavigator.routing.requirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class AnyOfRequirement
        implements RouteRequirement
{
    private final String displayName;

    private final List<RouteRequirement> alternatives;

    public AnyOfRequirement(
            String displayName,
            RouteRequirement... alternatives
    )
    {
        this.displayName =
                displayName;

        this.alternatives =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                Arrays.asList(
                                        alternatives
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
    getAlternatives()
    {
        return alternatives;
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        StringJoiner unavailableReasons =
                new StringJoiner(
                        " OR "
                );

        RequirementState strongestFailure =
                RequirementState.BLOCKED;

        RequirementResult
                bestBankAlternative = null;

        RouteRequirement
                bestBankRequirement = null;

        for (
                RouteRequirement alternative
                : alternatives
        )
        {
            RequirementResult result =
                    alternative.evaluate(
                            context
                    );

            if (result.isSatisfied())
            {
                /*
                 * Always prefer an option that is usable immediately.
                 *
                 * This prevents a banked alternative from winning
                 * merely because it appeared first in the list.
                 */
                if (result.isReadyNow())
                {
                    return RequirementResult.satisfied(
                            displayName,
                            "READY NOW via "
                                    + alternative
                                    .getDisplayName()
                                    + ". "
                                    + result.getDetail()
                    );
                }

                if (
                        result.requiresBankWithdrawal()
                                && bestBankAlternative == null
                )
                {
                    bestBankAlternative =
                            result;

                    bestBankRequirement =
                            alternative;
                }

                continue;
            }

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

            unavailableReasons.add(
                    result.getDetail()
            );
        }

        if (
                bestBankAlternative != null
                        && bestBankRequirement != null
        )
        {
            return RequirementResult.availableFromBank(
                    displayName,
                    "AVAILABLE FROM BANK via "
                            + bestBankRequirement
                            .getDisplayName()
                            + ". "
                            + bestBankAlternative
                            .getDetail()
            );
        }

        switch (strongestFailure)
        {
            case DATA_REQUIRED:
                return RequirementResult.dataRequired(
                        displayName,
                        unavailableReasons.toString()
                );

            case CONFIGURATION_REQUIRED:
                return RequirementResult.configurationRequired(
                        displayName,
                        unavailableReasons.toString()
                );

            case BLOCKED:
            case SATISFIED:
            default:
                return RequirementResult.blocked(
                        displayName,
                        unavailableReasons.toString()
                );
        }
    }
}