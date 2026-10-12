package com.farmingnavigator.routing.requirement;

public enum LiveItemLocation
{
    INVENTORY(
            "inventory"
    ),

    EQUIPMENT(
            "equipment"
    ),

    INVENTORY_OR_EQUIPMENT(
            "inventory or equipment"
    );

    private final String displayName;

    LiveItemLocation(
            String displayName
    )
    {
        this.displayName =
                displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}