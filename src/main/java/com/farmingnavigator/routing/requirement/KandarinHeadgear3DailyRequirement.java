package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import com.farmingnavigator.routing.capability.DailyTransportUseKind;
import net.runelite.api.gameval.ItemID;

public class KandarinHeadgear3DailyRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Kandarin headgear 3 to Sherlock";

    private final AllOfRequirement
            completeRequirement;

    public KandarinHeadgear3DailyRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        new DiaryRequirement(
                                DiaryArea.KANDARIN,
                                DiaryTier.HARD,
                                true
                        ),
                        new BankBackedItemRequirement(
                                "Kandarin headgear 3",
                                ItemID.SEERS_HEADBAND_HARD,
                                "Kandarin headgear 3",
                                1
                        ),
                        new DailyUseRequirement(
                                "Kandarin headgear 3 Sherlock daily teleport",
                                DailyTransportUseKind
                                        .KANDARIN_SHERLOCK_TELEPORT,
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