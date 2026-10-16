package com.farmingnavigator.routing.run;

public enum FieldRunMode
{
    KEEP_MATURE_FLOWERS("Keep mature flowers"), HARVEST_ALL("Harvest and replant flowers too");
    private final String label;
    FieldRunMode(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
