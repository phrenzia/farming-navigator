package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.poh.PohJewelleryBoxTier;

public class PohJewelleryBoxSkillsNecklaceRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "POH jewellery box Skills necklace teleports";

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
        if (
                context == null
                        || context.getConfig() == null
        )
        {
            return RequirementResult
                    .configurationRequired(
                            DISPLAY_NAME,
                            "Configure your POH profile in Farming Navigator."
                    );
        }

        FarmingNavigatorConfig config =
                context.getConfig();

        PohJewelleryBoxTier tier =
                config.pohJewelleryBox();

        if (
                tier != null
                        && tier
                        .hasSkillsNecklaceTeleports()
        )
        {
            return RequirementResult.satisfied(
                    DISPLAY_NAME,
                    "READY NOW - "
                            + tier.getDisplayName()
                            + " provides unlimited Skills necklace teleports."
            );
        }

        String configuredTier =
                tier == null
                        ? "No jewellery box"
                        : tier.getDisplayName();

        return RequirementResult.blocked(
                DISPLAY_NAME,
                "MISSING - Fancy or Ornate jewellery box required for Skills necklace teleports. Configured: "
                        + configuredTier
                        + "."
        );
    }
}