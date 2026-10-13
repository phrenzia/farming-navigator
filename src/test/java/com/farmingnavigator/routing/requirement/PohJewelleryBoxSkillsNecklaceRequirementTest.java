package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.poh.PohJewelleryBoxTier;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PohJewelleryBoxSkillsNecklaceRequirementTest
{
    @Test
    public void missingPohConfigurationRequiresConfiguration()
    {
        RequirementResult result =
                evaluate(
                        null
                );

        assertEquals(
                RequirementState.CONFIGURATION_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Configure your POH profile"
                        )
        );
    }

    @Test
    public void noJewelleryBoxIsMissing()
    {
        RequirementResult result =
                evaluate(
                        createConfig(
                                PohJewelleryBoxTier.NONE
                        )
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Fancy or Ornate jewellery box"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "None"
                        )
        );
    }

    @Test
    public void basicJewelleryBoxDoesNotProvideSkillsNecklaceTeleports()
    {
        RequirementResult result =
                evaluate(
                        createConfig(
                                PohJewelleryBoxTier.BASIC
                        )
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Basic jewellery box"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Fancy or Ornate jewellery box"
                        )
        );
    }

    @Test
    public void fancyJewelleryBoxIsReadyNow()
    {
        RequirementResult result =
                evaluate(
                        createConfig(
                                PohJewelleryBoxTier.FANCY
                        )
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.READY_NOW,
                result.getReadiness()
        );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Fancy jewellery box"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "unlimited Skills necklace teleports"
                        )
        );
    }

    @Test
    public void ornateJewelleryBoxIsReadyNow()
    {
        RequirementResult result =
                evaluate(
                        createConfig(
                                PohJewelleryBoxTier.ORNATE
                        )
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.READY_NOW,
                result.getReadiness()
        );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Ornate jewellery box"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "unlimited Skills necklace teleports"
                        )
        );
    }

    private RequirementResult evaluate(
            FarmingNavigatorConfig config
    )
    {
        RequirementContext context =
                new RequirementContext(
                        null,
                        config
                );

        return new PohJewelleryBoxSkillsNecklaceRequirement()
                .evaluate(
                        context
                );
    }

    private FarmingNavigatorConfig createConfig(
            PohJewelleryBoxTier tier
    )
    {
        return new FarmingNavigatorConfig()
        {
            @Override
            public PohJewelleryBoxTier
            pohJewelleryBox()
            {
                return tier;
            }
        };
    }
}