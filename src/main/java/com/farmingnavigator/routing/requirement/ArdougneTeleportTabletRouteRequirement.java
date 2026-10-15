package com.farmingnavigator.routing.requirement;

import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;

public class ArdougneTeleportTabletRouteRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Ardougne teleport tablet to East Ardougne";

    private final AllOfRequirement
            completeRequirement;

    public ArdougneTeleportTabletRouteRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        new QuestRequirement(
                                Quest.PLAGUE_CITY
                        ),
                        new BankBackedItemRequirement(
                                "Ardougne teleport tablet",
                                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                                "Ardougne teleport tablet",
                                1
                        )
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