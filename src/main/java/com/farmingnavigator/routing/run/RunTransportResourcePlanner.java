package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.requirement.ChargedTeleportBudgetRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RuneCost;
import com.farmingnavigator.routing.requirement.RuneCostRequirement;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Aggregates the recommended legs only; fallbacks are alternatives, not extra travel. */
public final class RunTransportResourcePlanner
{
    public RunTransportResources evaluate(List<HerbRunStop> stops, RequirementContext context)
    {
        RuneCost.Builder runes = RuneCost.builder();
        Map<ChargedTeleportItemDefinition, Integer> jewellery = new EnumMap<>(ChargedTeleportItemDefinition.class);
        List<String> notes = new ArrayList<>();
        for (HerbRunStop stop : stops)
        {
            if (stop.getRecommended() == null)
            {
                notes.add("Choose transport for " + stop.getPatch().getDisplayName() + ".");
                continue;
            }
            TransportOption option = stop.getRecommended().getOption();
            if (option.getVerificationKind() == TransportVerificationKind.TELEPORT_SPELL
                    && option.getTeleportSpellDefinition() != null)
            {
                option.getTeleportSpellDefinition().getRuneCost().getQuantities().forEach(runes::add);
                continue;
            }
            ChargedTeleportItemDefinition family = jewelleryFamily(option.getVerificationKind());
            if (family != null)
            {
                jewellery.merge(family, 1, Integer::sum);
            }
            else if (option.getCategory() == com.farmingnavigator.transport.TransportCategory.PLAYER_OWNED_HOUSE)
            {
                notes.add("Bring a house entry teleport for " + stop.getPatch().getDisplayName()
                        + "; POH entry supplies are not included in this total.");
            }
        }
        List<RequirementResult> checks = new ArrayList<>();
        RuneCost totalRunes = runes.build();
        if (!totalRunes.isEmpty())
        {
            checks.add(new RuneCostRequirement("All recommended direct-spell runes", totalRunes).evaluate(context));
        }
        jewellery.forEach((family, uses) -> checks.add(new ChargedTeleportBudgetRequirement(family, uses).evaluate(context)));
        return new RunTransportResources(checks, notes);
    }

    private ChargedTeleportItemDefinition jewelleryFamily(TransportVerificationKind kind)
    {
        switch (kind)
        {
            case SKILLS_NECKLACE: return ChargedTeleportItemDefinition.SKILLS_NECKLACE;
            case AMULET_OF_GLORY: return ChargedTeleportItemDefinition.AMULET_OF_GLORY;
            case RING_OF_WEALTH: return ChargedTeleportItemDefinition.RING_OF_WEALTH;
            case COMBAT_BRACELET: return ChargedTeleportItemDefinition.COMBAT_BRACELET;
            default: return null;
        }
    }
}
