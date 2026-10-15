package com.farmingnavigator.routing.run;

public enum GardenRunMode
{
    KEEP_MATURE("Keep mature plants"), REPLACE_MATURE("Replace mature plants");
    private final String label;
    GardenRunMode(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
