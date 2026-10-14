package com.farmingnavigator.routing.requirement;

import net.runelite.api.Quest;

public class WeissLarryBoatRouteRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Fairy ring DKS and Larry's boat to Weiss";

    private final AllOfRequirement
            completeRequirement;

    public WeissLarryBoatRouteRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        new QuestRequirement(
                                Quest.MAKING_FRIENDS_WITH_MY_ARM
                        ),
                        new FairyRingAccessRequirement()
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