package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;

public class SpellbookRequirement
        implements RouteRequirement
{
    private final SpellbookType requiredSpellbook;

    public SpellbookRequirement(
            SpellbookType requiredSpellbook
    )
    {
        this.requiredSpellbook =
                requiredSpellbook;
    }

    @Override
    public String getDisplayName()
    {
        return requiredSpellbook
                .getDisplayName()
                + " spellbook";
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        if (
                context == null
                        || context
                        .getTravelCapabilitySnapshot()
                        == null
                        || !context
                        .getTravelCapabilitySnapshot()
                        .isLoggedIn()
        )
        {
            return RequirementResult.dataRequired(
                    getDisplayName(),
                    "Log in so Farming Navigator can verify the active spellbook."
            );
        }

        TravelCapabilitySnapshot snapshot =
                context
                        .getTravelCapabilitySnapshot();

        SpellbookType currentSpellbook =
                snapshot.getActiveSpellbook();

        if (
                currentSpellbook
                        == SpellbookType.UNKNOWN
        )
        {
            return RequirementResult.dataRequired(
                    getDisplayName(),
                    "Farming Navigator could not verify the active spellbook."
            );
        }

        if (
                currentSpellbook
                        == requiredSpellbook
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    requiredSpellbook
                            .getDisplayName()
                            + " spellbook is active."
            );
        }

        return RequirementResult.blocked(
                getDisplayName(),
                "Requires the "
                        + requiredSpellbook
                        .getDisplayName()
                        + " spellbook. Current verified spellbook: "
                        + currentSpellbook
                        .getDisplayName()
                        + "."
        );
    }
}