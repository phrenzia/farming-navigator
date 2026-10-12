package com.farmingnavigator.routing.spell;

import com.farmingnavigator.routing.requirement.AccountSkill;
import com.farmingnavigator.routing.requirement.AllOfRequirement;
import com.farmingnavigator.routing.requirement.PohFeature;
import com.farmingnavigator.routing.requirement.PohFeatureRequirement;
import com.farmingnavigator.routing.requirement.QuestRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RouteRequirement;
import com.farmingnavigator.routing.requirement.RuneCostRequirement;
import com.farmingnavigator.routing.requirement.SkillRequirement;
import com.farmingnavigator.routing.requirement.SpellbookRequirement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeleportSpellRequirement
        implements RouteRequirement
{
    private final TeleportSpellDefinition definition;

    private final List<RouteRequirement>
            componentRequirements;

    private final AllOfRequirement
            completeRequirement;

    public TeleportSpellRequirement(
            TeleportSpellDefinition definition
    )
    {
        if (definition == null)
        {
            throw new IllegalArgumentException(
                    "Teleport spell definition cannot be null."
            );
        }

        this.definition =
                definition;

        List<RouteRequirement> requirements =
                new ArrayList<>();

        requirements.add(
                new SkillRequirement(
                        AccountSkill.MAGIC,
                        definition
                                .getRequiredMagicLevel(),
                        true
                )
        );

        requirements.add(
                new SpellbookRequirement(
                        definition.getSpellbook()
                )
        );

        if (
                definition.getRequiredQuest()
                        != null
        )
        {
            requirements.add(
                    new QuestRequirement(
                            definition
                                    .getRequiredQuest()
                    )
            );
        }

        if (
                definition
                        .requiresConfiguredHouse()
        )
        {
            requirements.add(
                    new PohFeatureRequirement(
                            PohFeature.HOUSE
                    )
            );
        }

        requirements.add(
                new RuneCostRequirement(
                        definition
                                .getDisplayName()
                                + " runes",
                        definition
                                .getRuneCost()
                )
        );

        this.componentRequirements =
                Collections.unmodifiableList(
                        requirements
                );

        this.completeRequirement =
                new AllOfRequirement(
                        definition
                                .getDisplayName(),
                        requirements.toArray(
                                new RouteRequirement[0]
                        )
                );
    }

    public TeleportSpellDefinition
    getDefinition()
    {
        return definition;
    }

    public List<RouteRequirement>
    getComponentRequirements()
    {
        return componentRequirements;
    }

    @Override
    public String getDisplayName()
    {
        return definition
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