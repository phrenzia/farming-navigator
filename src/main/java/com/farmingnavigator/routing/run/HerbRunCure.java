package com.farmingnavigator.routing.run;

public enum HerbRunCure
{
    PLANT_CURE("Plant cure potion"),
    CURE_PLANT("Cure Plant spell");

    private final String label;
    HerbRunCure(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
