package com.farmingnavigator.routing.run;

public enum TreeRunCure
{
    PRUNE("Prune with secateurs"), CURE_PLANT("Cure Plant spell");
    private final String label;
    TreeRunCure(String label) { this.label = label; }
    public HerbRunCure asHerbCure() { return this == CURE_PLANT ? HerbRunCure.CURE_PLANT : HerbRunCure.PLANT_CURE; }
    @Override public String toString() { return label; }
}
