package com.farmingnavigator.transport;

public enum VerifiedTransportState
{
    READY_NOW("READY NOW"),
    AVAILABLE_FROM_BANK("AVAILABLE FROM BANK"),
    DATA_REQUIRED("DATA REQUIRED"),
    CONFIGURATION_REQUIRED("CONFIGURATION REQUIRED"),
    MISSING("MISSING"),
    LOCKED("LOCKED");

    private final String displayName;

    VerifiedTransportState(
            String displayName
    )
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}