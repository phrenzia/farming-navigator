package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;

public class DiaryRequirement
        implements RouteRequirement
{
    private final DiaryArea area;

    private final DiaryTier tier;

    private final boolean rewardMustBeClaimed;

    public DiaryRequirement(
            DiaryArea area,
            DiaryTier tier,
            boolean rewardMustBeClaimed
    )
    {
        this.area = area;
        this.tier = tier;
        this.rewardMustBeClaimed =
                rewardMustBeClaimed;
    }

    @Override
    public String getDisplayName()
    {
        return area.getDisplayName()
                + " "
                + tier.getDisplayName()
                + " Diary"
                + (
                rewardMustBeClaimed
                        ? " reward claimed"
                        : " completed"
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
                    "Log in so Farming Navigator can verify achievement diary state."
            );
        }

        AccountState accountState =
                context.getAccountState();

        boolean satisfied =
                rewardMustBeClaimed
                        ? accountState
                        .hasClaimedDiaryTier(
                                area,
                                tier
                        )
                        : accountState
                        .hasCompletedDiaryTier(
                                area,
                                tier
                        );

        if (satisfied)
        {
            return RequirementResult.satisfied(
                    getDisplayName()
            );
        }

        if (
                rewardMustBeClaimed
                        && accountState
                        .isDiaryRewardClaimPending(
                                area,
                                tier
                        )
        )
        {
            return RequirementResult.blocked(
                    getDisplayName(),
                    area.getDisplayName()
                            + " "
                            + tier.getDisplayName()
                            + " Diary is complete, but its reward has not been claimed."
            );
        }

        return RequirementResult.blocked(
                getDisplayName(),
                "Requires "
                        + area.getDisplayName()
                        + " "
                        + tier.getDisplayName()
                        + " Diary"
                        + (
                        rewardMustBeClaimed
                                ? " reward to be claimed."
                                : " completion."
                )
        );
    }
}