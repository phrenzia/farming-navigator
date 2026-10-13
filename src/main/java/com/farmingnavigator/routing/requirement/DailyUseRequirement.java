package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.DailyTransportUseKind;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;

public class DailyUseRequirement
        implements RouteRequirement
{
    private final String displayName;

    private final DailyTransportUseKind kind;

    private final int requiredUses;

    public DailyUseRequirement(
            DailyTransportUseKind kind
    )
    {
        this(
                kind == null
                        ? null
                        : kind.getDisplayName(),
                kind,
                1
        );
    }

    public DailyUseRequirement(
            String displayName,
            DailyTransportUseKind kind,
            int requiredUses
    )
    {
        if (kind == null)
        {
            throw new IllegalArgumentException(
                    "Daily transport use kind cannot be null."
            );
        }

        if (requiredUses <= 0)
        {
            throw new IllegalArgumentException(
                    "Required daily uses must be greater than zero."
            );
        }

        this.displayName =
                displayName == null
                        || displayName.trim().isEmpty()
                        ? kind.getDisplayName()
                        : displayName;

        this.kind =
                kind;

        this.requiredUses =
                requiredUses;
    }

    public DailyTransportUseKind getKind()
    {
        return kind;
    }

    public int getRequiredUses()
    {
        return requiredUses;
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
        if (context == null)
        {
            return RequirementResult.dataRequired(
                    displayName,
                    "DATA REQUIRED - log in so Farming Navigator can verify remaining daily transport uses."
            );
        }

        TravelCapabilitySnapshot snapshot =
                context.getTravelCapabilitySnapshot();

        if (
                snapshot == null
                        || !snapshot.isLoggedIn()
        )
        {
            return RequirementResult.dataRequired(
                    displayName,
                    "DATA REQUIRED - log in so Farming Navigator can verify remaining daily transport uses."
            );
        }

        if (
                !snapshot.hasDailyUseData(
                        kind
                )
        )
        {
            return RequirementResult.dataRequired(
                    displayName,
                    "DATA REQUIRED - remaining daily uses for "
                            + kind.getDisplayName()
                            + " have not been verified."
            );
        }

        int remaining =
                snapshot.getRemainingDailyUses(
                        kind
                );

        if (remaining >= requiredUses)
        {
            return RequirementResult.satisfied(
                    displayName,
                    "READY NOW - "
                            + remaining
                            + " daily use"
                            + (remaining == 1
                            ? ""
                            : "s")
                            + " remaining for "
                            + kind.getDisplayName()
                            + "."
            );
        }

        return RequirementResult.blocked(
                displayName,
                "MISSING - requires at least "
                        + requiredUses
                        + " remaining daily use"
                        + (requiredUses == 1
                        ? ""
                        : "s")
                        + " for "
                        + kind.getDisplayName()
                        + ", but "
                        + remaining
                        + " remain today."
        );
    }
}