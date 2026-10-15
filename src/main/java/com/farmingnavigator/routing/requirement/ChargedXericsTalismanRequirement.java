package com.farmingnavigator.routing.requirement;

import net.runelite.api.gameval.ItemID;

/*
 * The charged and inert talismans have distinct item IDs. Glade is a default
 * destination; no charge count or quest unlock is inferred from a varbit.
 * https://github.com/Skretzo/shortest-path/blob/99735452c7c257d3fe5ae79970429cdce907d571/src/main/resources/transports/teleportation_items.tsv
 */
public class ChargedXericsTalismanRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Charged Xeric's talisman";

    private final BankBackedItemRequirement
            chargedTalismanRequirement;

    public ChargedXericsTalismanRequirement()
    {
        this.chargedTalismanRequirement =
                new BankBackedItemRequirement(
                        DISPLAY_NAME,
                        ItemID.XERIC_TALISMAN,
                        "Charged Xeric's talisman",
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
        return chargedTalismanRequirement.evaluate(
                context
        );
    }
}