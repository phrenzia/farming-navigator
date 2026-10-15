package com.farmingnavigator.farming.tree;

public enum TreePatchCondition
{
    UNKNOWN("Data required"), EMPTY("Empty"), WEEDS("Weeds present"), GROWING("Growing"),
    CHECK_HEALTH("Ready to check health"), HEALTH_CHECKED("Health checked"), STUMP("Stump"),
    DISEASED("Diseased"), DEAD("Dead"), HARVESTABLE("Bark available to harvest"), HARVESTED("Bark harvested; tree remains");
    private final String label;
    TreePatchCondition(String label) { this.label = label; }
    public String getLabel() { return label; }
}
