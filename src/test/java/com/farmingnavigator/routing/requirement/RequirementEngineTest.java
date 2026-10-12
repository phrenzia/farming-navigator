package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.poh.PohJewelleryBoxTier;
import com.farmingnavigator.poh.PohPortalDestination;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequirementEngineTest
{
    @Test
    public void boostableSkillUsesBoostedLevel()
    {
        AccountState state =
                createAccountState();

        SkillRequirement requirement =
                new SkillRequirement(
                        AccountSkill.FARMING,
                        65,
                        true
                );

        RequirementResult result =
                requirement.evaluate(
                        new RequirementContext(
                                state,
                                null
                        )
                );

        assertTrue(
                result.isSatisfied()
        );
    }

    @Test
    public void nonBoostableSkillUsesRealLevel()
    {
        AccountState state =
                createAccountState();

        SkillRequirement requirement =
                new SkillRequirement(
                        AccountSkill.FARMING,
                        65,
                        false
                );

        RequirementResult result =
                requirement.evaluate(
                        new RequirementContext(
                                state,
                                null
                        )
                );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );
    }

    @Test
    public void questRequirementDistinguishesProgress()
    {
        AccountState state =
                createAccountState();

        QuestRequirement startedRequirement =
                new QuestRequirement(
                        Quest.FAIRYTALE_II__CURE_A_QUEEN,
                        QuestState.IN_PROGRESS
                );

        QuestRequirement completedRequirement =
                new QuestRequirement(
                        Quest.FAIRYTALE_II__CURE_A_QUEEN
                );

        RequirementContext context =
                new RequirementContext(
                        state,
                        null
                );

        assertTrue(
                startedRequirement
                        .evaluate(context)
                        .isSatisfied()
        );

        assertFalse(
                completedRequirement
                        .evaluate(context)
                        .isSatisfied()
        );
    }

    @Test
    public void diaryRequirementCanRequireClaimedReward()
    {
        AccountState state =
                createAccountState();

        DiaryRequirement completion =
                new DiaryRequirement(
                        DiaryArea.KOUREND_AND_KEBOS,
                        DiaryTier.HARD,
                        false
                );

        DiaryRequirement claimedReward =
                new DiaryRequirement(
                        DiaryArea.KOUREND_AND_KEBOS,
                        DiaryTier.HARD,
                        true
                );

        RequirementContext context =
                new RequirementContext(
                        state,
                        null
                );

        assertTrue(
                completion
                        .evaluate(context)
                        .isSatisfied()
        );

        RequirementResult rewardResult =
                claimedReward.evaluate(
                        context
                );

        assertFalse(
                rewardResult.isSatisfied()
        );

        assertTrue(
                rewardResult
                        .getDetail()
                        .contains(
                                "reward has not been claimed"
                        )
        );
    }

    @Test
    public void pohFeaturesUseConfiguredProfile()
    {
        FarmingNavigatorConfig config =
                createConfig();

        RequirementContext context =
                new RequirementContext(
                        createAccountState(),
                        config
                );

        assertTrue(
                new PohFeatureRequirement(
                        PohFeature.FAIRY_RING
                )
                        .evaluate(context)
                        .isSatisfied()
        );

        assertTrue(
                new PohFeatureRequirement(
                        PohFeature
                                .SKILLS_NECKLACE_JEWELLERY_BOX
                )
                        .evaluate(context)
                        .isSatisfied()
        );

        assertFalse(
                new PohFeatureRequirement(
                        PohFeature
                                .MOUNTED_XERICS_TALISMAN
                )
                        .evaluate(context)
                        .isSatisfied()
        );
    }

    @Test
    public void pohPortalRequirementChecksExactDestination()
    {
        FarmingNavigatorConfig config =
                createConfig();

        RequirementContext context =
                new RequirementContext(
                        createAccountState(),
                        config
                );

        assertTrue(
                new PohPortalRequirement(
                        PohPortalDestination.WEISS
                )
                        .evaluate(context)
                        .isSatisfied()
        );

        assertFalse(
                new PohPortalRequirement(
                        PohPortalDestination.HARMONY_ISLAND
                )
                        .evaluate(context)
                        .isSatisfied()
        );
    }

    @Test
    public void allOfAndAnyOfComposeRequirements()
    {
        RequirementContext context =
                new RequirementContext(
                        createAccountState(),
                        createConfig()
                );

        RouteRequirement farming65 =
                new SkillRequirement(
                        AccountSkill.FARMING,
                        65,
                        true
                );

        RouteRequirement farming99 =
                new SkillRequirement(
                        AccountSkill.FARMING,
                        99,
                        true
                );

        RouteRequirement fairyRing =
                new PohFeatureRequirement(
                        PohFeature.FAIRY_RING
                );

        AllOfRequirement allOf =
                new AllOfRequirement(
                        "All route requirements",
                        farming65,
                        fairyRing
                );

        AnyOfRequirement anyOf =
                new AnyOfRequirement(
                        "At least one route option",
                        farming99,
                        fairyRing
                );

        assertTrue(
                allOf.evaluate(context)
                        .isSatisfied()
        );

        assertTrue(
                anyOf.evaluate(context)
                        .isSatisfied()
        );
    }

    private AccountState createAccountState()
    {
        Map<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.FAIRYTALE_II__CURE_A_QUEEN,
                QuestState.IN_PROGRESS
        );

        questStates.put(
                Quest.MAKING_FRIENDS_WITH_MY_ARM,
                QuestState.FINISHED
        );

        Map<DiaryArea, DiaryTier> completed =
                new EnumMap<>(
                        DiaryArea.class
                );

        completed.put(
                DiaryArea.KOUREND_AND_KEBOS,
                DiaryTier.HARD
        );

        Map<DiaryArea, DiaryTier> claimed =
                new EnumMap<>(
                        DiaryArea.class
                );

        claimed.put(
                DiaryArea.KOUREND_AND_KEBOS,
                DiaryTier.MEDIUM
        );

        return new AccountState(
                true,
                60,
                66,
                70,
                72,
                73,
                73,
                60,
                60,
                questStates,
                completed,
                claimed
        );
    }

    private FarmingNavigatorConfig createConfig()
    {
        return new FarmingNavigatorConfig()
        {
            @Override
            public PohHouseLocation pohHouseLocation()
            {
                return PohHouseLocation.RIMMINGTON;
            }

            @Override
            public PohJewelleryBoxTier pohJewelleryBox()
            {
                return PohJewelleryBoxTier.ORNATE;
            }

            @Override
            public boolean pohFairyRing()
            {
                return true;
            }

            @Override
            public boolean pohSpiritTree()
            {
                return true;
            }

            @Override
            public boolean pohMountedXericsTalisman()
            {
                return false;
            }

            @Override
            public boolean pohMountedDigsitePendant()
            {
                return false;
            }

            @Override
            public Set<PohPortalDestination>
            pohPortalDestinations()
            {
                return Collections.unmodifiableSet(
                        EnumSet.of(
                                PohPortalDestination.WEISS,
                                PohPortalDestination.CATHERBY
                        )
                );
            }
        };
    }
}