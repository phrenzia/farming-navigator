package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.field.FieldFamily;

public enum FieldRunWatering
{
    NONE("No watering step"), AFTER_PLANTING("Water once after planting (manual)");
    private final String label;
    FieldRunWatering(String label) { this.label = label; }
    public boolean appliesTo(FieldFamily family)
    {
        return this == AFTER_PLANTING && (family == FieldFamily.ALLOTMENT || family == FieldFamily.FLOWER || family == FieldFamily.HOPS);
    }
    @Override public String toString() { return label; }
}
