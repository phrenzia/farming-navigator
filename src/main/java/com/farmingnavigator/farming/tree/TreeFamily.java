package com.farmingnavigator.farming.tree;

public enum TreeFamily
{
    TREE("Regular trees"), FRUIT_TREE("Fruit trees"), HARDWOOD_TREE("Hardwood trees"),
    CALQUAT("Calquat trees"), CELASTRUS("Celastrus trees"), REDWOOD("Redwood trees"), CRYSTAL_TREE("Crystal trees");
    private final String label;
    TreeFamily(String label) { this.label = label; }
    public String getLabel() { return label; }
}
