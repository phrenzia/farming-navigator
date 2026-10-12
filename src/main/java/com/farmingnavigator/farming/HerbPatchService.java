package com.farmingnavigator.farming;

import com.farmingnavigator.account.AccountState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Quest;

public class HerbPatchService
{
    @Inject
    public HerbPatchService()
    {
    }

    public List<HerbPatchStatus> evaluate(
            AccountState accountState
    )
    {
        if (
                accountState == null
                        || !accountState.isLoggedIn()
        )
        {
            return Collections.emptyList();
        }

        List<HerbPatchStatus> statuses =
                new ArrayList<>();

        for (
                HerbPatchDefinition patch
                : HerbPatchDefinition.values()
        )
        {
            List<String> missingRequirements =
                    new ArrayList<>();

            checkFarmingRequirement(
                    accountState,
                    patch,
                    missingRequirements
            );

            checkQuestRequirement(
                    accountState,
                    patch,
                    missingRequirements
            );

            checkDiaryRequirement(
                    accountState,
                    patch,
                    missingRequirements
            );

            checkSpecialPatchRequirements(
                    accountState,
                    patch,
                    missingRequirements
            );

            statuses.add(
                    new HerbPatchStatus(
                            patch,
                            missingRequirements
                    )
            );
        }

        return Collections.unmodifiableList(
                statuses
        );
    }

    private void checkFarmingRequirement(
            AccountState accountState,
            HerbPatchDefinition patch,
            List<String> missingRequirements
    )
    {
        int requiredLevel =
                patch.getMinimumFarmingLevel();

        if (requiredLevel <= 0)
        {
            return;
        }

        if (
                accountState
                        .getBoostedFarmingLevel()
                        < requiredLevel
        )
        {
            missingRequirements.add(
                    requiredLevel
                            + " Farming (boostable)"
            );
        }
    }

    private void checkQuestRequirement(
            AccountState accountState,
            HerbPatchDefinition patch,
            List<String> missingRequirements
    )
    {
        Quest requiredQuest =
                patch.getRequiredQuest();

        if (requiredQuest == null)
        {
            return;
        }

        if (
                !accountState.hasCompletedQuest(
                        requiredQuest
                )
        )
        {
            missingRequirements.add(
                    "Quest: "
                            + requiredQuest.getName()
            );
        }
    }

    private void checkDiaryRequirement(
            AccountState accountState,
            HerbPatchDefinition patch,
            List<String> missingRequirements
    )
    {
        if (
                !patch
                        .requiresMorytaniaEliteReward()
        )
        {
            return;
        }

        if (
                !accountState
                        .isMorytaniaEliteRewardClaimed()
        )
        {
            missingRequirements.add(
                    "Morytania Elite Diary reward"
            );
        }
    }

    private void checkSpecialPatchRequirements(
            AccountState accountState,
            HerbPatchDefinition patch,
            List<String> missingRequirements
    )
    {
        if (
                patch
                        != HerbPatchDefinition.WEISS
        )
        {
            return;
        }

        if (
                !accountState
                        .isFireOfNourishmentBuilt()
        )
        {
            missingRequirements.add(
                    "Fire of Nourishment built"
            );
        }
    }
}