package com.farmingnavigator.routing.requirement;

public interface RouteRequirement
{
    String getDisplayName();

    RequirementResult evaluate(
            RequirementContext context
    );
}