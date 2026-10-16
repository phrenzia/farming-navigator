package com.farmingnavigator.farming.field;

public enum FieldPatchCondition
{
    UNKNOWN("Unknown"), EMPTY("Empty"), WEEDS("Weeds"), GROWING("Growing"),
    HARVESTABLE("Harvestable; quantity unknown"), COLLECTABLE("Collect option observed; quantity unknown"), DISEASED("Diseased"), DEAD("Dead"),
    SCARECROW("Scarecrow present; inspect weeds manually");
    private final String label;
    FieldPatchCondition(String label) { this.label = label; }
    public String getLabel() { return label; }
}
