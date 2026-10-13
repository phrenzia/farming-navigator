package com.farmingnavigator.routing.requirement;

public class PohJewelleryBoxSkillsNecklaceRouteRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "POH jewellery box Skills necklace route";

    private final AllOfRequirement
            completeRequirement;

    public PohJewelleryBoxSkillsNecklaceRouteRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        new PohHouseEntryRequirement(),
                        new PohJewelleryBoxSkillsNecklaceRequirement()
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
        return completeRequirement.evaluate(
                context
        );
    }
}