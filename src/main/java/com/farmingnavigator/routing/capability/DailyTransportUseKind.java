package com.farmingnavigator.routing.capability;

public enum DailyTransportUseKind
{
    EXPLORER_RING_CABBAGE_PORT(
            "Explorer's ring Cabbage-port"
    ),

    ARDOUGNE_FARM_TELEPORT(
            "Ardougne farm teleport"
    );

    private final String displayName;

    DailyTransportUseKind(
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

    @Override
    public String toString()
    {
        return displayName;
    }
}