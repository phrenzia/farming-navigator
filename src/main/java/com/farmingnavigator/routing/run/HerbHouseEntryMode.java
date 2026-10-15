package com.farmingnavigator.routing.run;

/** Explicitly chooses what the run-wide budget reserves for each POH visit. */
public enum HerbHouseEntryMode
{
    TABLETS("House tablets"),
    SPELL("Teleport to House spell");

    private final String label;

    HerbHouseEntryMode(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
