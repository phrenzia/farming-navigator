package com.farmingnavigator.farming.field;

public enum FieldPatchCondition
{
    UNKNOWN("Unknown"), EMPTY("Empty"), WEEDS("Weeds"), GROWING("Growing"),
    HARVESTABLE("Harvestable; quantity unknown"), COLLECTABLE("Collect option observed; quantity unknown"), DISEASED("Diseased"), DEAD("Dead"),
    LIVING_ANIMA("Living anima; effects unchecked"), WITHERING_ANIMA("Withering anima; not an ordinary crop disease"),
    MATURE_HESPORI("Mature Hespori patch; inspect manually"),
    SCARECROW("Scarecrow present; inspect weeds manually");
    private final String label;
    FieldPatchCondition(String label) { this.label = label; }
    public String getLabel() { return label; }
}
