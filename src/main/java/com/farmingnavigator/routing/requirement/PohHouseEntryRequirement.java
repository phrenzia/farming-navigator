package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.routing.spell.TeleportSpellRequirement;
import com.farmingnavigator.routing.tablet.TeleportTabletDefinition;
import com.farmingnavigator.routing.tablet.TeleportTabletRequirement;

public class PohHouseEntryRequirement
        implements RouteRequirement
{
    private final TeleportSpellRequirement
            houseTeleportSpellRequirement;

    private final TeleportTabletRequirement
            houseTeleportTabletRequirement;

    private final AnyOfRequirement
            completeRequirement;

    public PohHouseEntryRequirement()
    {
        this.houseTeleportSpellRequirement =
                TeleportSpellDefinition
                        .TELEPORT_TO_HOUSE
                        .createRequirement();

        this.houseTeleportTabletRequirement =
                TeleportTabletDefinition
                        .TELEPORT_TO_HOUSE
                        .createRequirement();

        this.completeRequirement =
                new AnyOfRequirement(
                        "Player-owned house entry",
                        houseTeleportSpellRequirement,
                        houseTeleportTabletRequirement
                );
    }

    public TeleportSpellRequirement
    getHouseTeleportSpellRequirement()
    {
        return houseTeleportSpellRequirement;
    }

    public TeleportTabletRequirement
    getHouseTeleportTabletRequirement()
    {
        return houseTeleportTabletRequirement;
    }

    @Override
    public String getDisplayName()
    {
        return completeRequirement
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