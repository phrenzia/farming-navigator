package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import com.farmingnavigator.routing.capability.DailyTransportUseKind;
import net.runelite.api.gameval.ItemID;

public class ExplorerRing2CabbagePortRequirement
        implements RouteRequirement
{
    private static final String DISPLAY_NAME =
            "Explorer's ring 2 Cabbage-port";

    private final AllOfRequirement
            completeRequirement;

    public ExplorerRing2CabbagePortRequirement()
    {
        this.completeRequirement =
                new AllOfRequirement(
                        DISPLAY_NAME,
                        new DiaryRequirement(
                                DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                                DiaryTier.MEDIUM,
                                true
                        ),
                        new BankBackedItemRequirement(
                                "Explorer's ring 2",
                                ItemID.LUMBRIDGE_RING_MEDIUM,
                                "Explorer's ring 2",
                                1
                        ),
                        new DailyUseRequirement(
                                "Explorer's ring Cabbage-port daily use",
                                DailyTransportUseKind
                                        .EXPLORER_RING_CABBAGE_PORT,
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