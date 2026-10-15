package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;

public class CombatBraceletRouteRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Combat bracelet to Ranging Guild";

    private final AllOfRequirement
            completeRequirement;

    public CombatBraceletRouteRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        ChargedTeleportItemDefinition
                                .COMBAT_BRACELET
                                .createRequirement()
                );
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
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