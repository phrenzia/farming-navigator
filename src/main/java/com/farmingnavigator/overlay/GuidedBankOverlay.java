package com.farmingnavigator.overlay;

import com.farmingnavigator.FarmingNavigatorConfig;
import javax.inject.Inject;

public final class GuidedBankOverlay extends GuidedItemOverlay
{
    @Inject
    public GuidedBankOverlay(FarmingNavigatorConfig config, GuidedRunOverlayController controller)
    {
        super(config, controller, true);
    }
}
