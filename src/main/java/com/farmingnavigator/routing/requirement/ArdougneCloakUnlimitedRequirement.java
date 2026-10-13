package com.farmingnavigator.routing.requirement;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

public class ArdougneCloakUnlimitedRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Unlimited Ardougne farm teleport";

    private final BankBackedItemRequirement
            itemRequirement;

    public ArdougneCloakUnlimitedRequirement()
    {
        this.itemRequirement =
                new BankBackedItemRequirement(
                        DISPLAY_NAME,
                        createSupportedVariants(),
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
        return itemRequirement.evaluate(
                context
        );
    }

    private static Map<Integer, String>
    createSupportedVariants()
    {
        Map<Integer, String> variants =
                new LinkedHashMap<>();

        variants.put(
                ItemID.ARDY_CAPE_ELITE,
                "Ardougne cloak 4"
        );

        variants.put(
                ItemID.SKILLCAPE_MAX_ARDY,
                "Ardougne max cape"
        );

        return variants;
    }
}