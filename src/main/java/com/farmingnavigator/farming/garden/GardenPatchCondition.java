package com.farmingnavigator.farming.garden;

public enum GardenPatchCondition
{
    UNKNOWN("Unknown state"), EMPTY("Empty"), WEEDS("Weeds"), GROWING("Growing"),
    CHECK_HEALTH("Ready to check health"), HEALTH_CHECKED("Health checked; inspect for produce"),
    DISEASED("Diseased"), DEAD("Dead");
    private final String label;
    GardenPatchCondition(String label) { this.label = label; }
    public String getLabel() { return label; }
}
