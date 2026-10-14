package com.farmingnavigator.routing.requirement;

import net.runelite.api.gameval.ItemID;

public class ChargedRingOfElementsRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Charged Ring of the elements";

    private final BankBackedItemRequirement
            chargedRingRequirement;

    public ChargedRingOfElementsRequirement()
    {
        this.chargedRingRequirement =
                new BankBackedItemRequirement(
                        DISPLAY_NAME,
                        ItemID.RING_OF_ELEMENTS_CHARGED,
                        "Charged Ring of the elements",
                        1
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
        return chargedRingRequirement.evaluate(
                context
        );
    }
}