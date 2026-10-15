package com.farmingnavigator.overlay;

import com.farmingnavigator.FarmingNavigatorConfig;
import javax.inject.Inject;

public final class GuidedInventoryOverlay extends GuidedItemOverlay
{
    @Inject
    public GuidedInventoryOverlay(FarmingNavigatorConfig config, GuidedRunOverlayController controller)
    {
        super(config, controller, false);
    }
}
