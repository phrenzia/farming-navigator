package com.farmingnavigator.routing.requirement;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;

public class MosLeHarmlessHarmonyRouteRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Mos Le'Harmless scroll to Harmony Island";

    private final AllOfRequirement
            completeRequirement;

    public MosLeHarmlessHarmonyRouteRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        new QuestRequirement(
                                Quest.CABIN_FEVER
                        ),
                        new QuestRequirement(
                                Quest.THE_GREAT_BRAIN_ROBBERY,
                                QuestState.IN_PROGRESS
                        ),
                        new BankBackedItemRequirement(
                                "Mos Le'Harmless teleport scroll",
                                ItemID.TELEPORTSCROLL_MOSLES,
                                "Mos Le'Harmless teleport scroll",
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