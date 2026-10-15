package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import net.runelite.api.gameval.ItemID;

public class KandarinHeadgearUnlimitedRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Kandarin headgear 4 to Sherlock";

    private final AllOfRequirement
            completeRequirement;

    public KandarinHeadgearUnlimitedRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        new DiaryRequirement(
                                DiaryArea.KANDARIN,
                                DiaryTier.ELITE,
                                true
                        ),
                        new BankBackedItemRequirement(
                                "Kandarin headgear 4",
                                ItemID.SEERS_HEADBAND_ELITE,
                                "Kandarin headgear 4",
                                1
                        )
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