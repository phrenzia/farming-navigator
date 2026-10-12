package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;

public class SkillRequirement
        implements RouteRequirement
{
    private final AccountSkill skill;

    private final int requiredLevel;

    private final boolean boostable;

    public SkillRequirement(
            AccountSkill skill,
            int requiredLevel,
            boolean boostable
    )
    {
        this.skill = skill;
        this.requiredLevel = requiredLevel;
        this.boostable = boostable;
    }

    @Override
    public String getDisplayName()
    {
        return requiredLevel
                + " "
                + skill.getDisplayName()
                + (
                boostable
                        ? " (boostable)"
                        : ""
        );
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        if (
                context == null
                        || context.getAccountState() == null
                        || !context
                        .getAccountState()
                        .isLoggedIn()
        )
        {
            return RequirementResult.dataRequired(
                    getDisplayName(),
                    "Log in so Farming Navigator can verify your "
                            + skill.getDisplayName()
                            + " level."
            );
        }

        AccountState accountState =
                context.getAccountState();

        int level =
                boostable
                        ? skill.getBoostedLevel(
                        accountState
                )
                        : skill.getRealLevel(
                        accountState
                );

        if (level >= requiredLevel)
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    skill.getDisplayName()
                            + " level "
                            + level
                            + " satisfies the requirement."
            );
        }

        return RequirementResult.blocked(
                getDisplayName(),
                "Requires "
                        + requiredLevel
                        + " "
                        + skill.getDisplayName()
                        + (
                        boostable
                                ? " (boostable)"
                                : ""
                )
                        + ". Current verified level: "
                        + level
                        + "."
        );
    }
}