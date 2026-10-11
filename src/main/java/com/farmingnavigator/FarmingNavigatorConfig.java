package com.farmingnavigator;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(FarmingNavigatorConfig.GROUP)
public interface FarmingNavigatorConfig extends Config
{
    String GROUP = "farming-navigator";

    @ConfigItem(
            keyName = "guidedRunOverlays",
            name = "Guided run overlays",
            description = "Enable visual guidance such as patch, item, NPC, and tile highlighting during farming runs"
    )
    default boolean guidedRunOverlays()
    {
        return true;
    }
}