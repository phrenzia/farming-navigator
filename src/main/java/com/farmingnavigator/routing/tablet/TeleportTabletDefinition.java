package com.farmingnavigator.routing.tablet;

import net.runelite.api.gameval.ItemID;

public enum TeleportTabletDefinition
{
    TELEPORT_TO_HOUSE(
            "Teleport to house tablet",
            ItemID.POH_TABLET_TELEPORTTOHOUSE,
            true
    );

    private final String displayName;

    private final int itemId;

    private final boolean requiresConfiguredHouse;

    TeleportTabletDefinition(
            String displayName,
            int itemId,
            boolean requiresConfiguredHouse
    )
    {
        this.displayName =
                displayName;

        this.itemId =
                itemId;

        this.requiresConfiguredHouse =
                requiresConfiguredHouse;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public int getItemId()
    {
        return itemId;
    }

    public boolean requiresConfiguredHouse()
    {
        return requiresConfiguredHouse;
    }

    public TeleportTabletRequirement
    createRequirement()
    {
        return new TeleportTabletRequirement(
                this
        );
    }
}