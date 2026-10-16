package com.farmingnavigator.routing.run;

public enum CombinedRunKind
{
    HERB("Herb"), TREE("Tree"), GARDEN("Bush/cactus"), FIELD("Field/special");
    private final String label;
    CombinedRunKind(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
