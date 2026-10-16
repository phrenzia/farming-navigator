package com.farmingnavigator.farming.tree;

public enum TreeFamily
{
    TREE("Regular trees", "regular tree"), FRUIT_TREE("Fruit trees", "fruit tree"), HARDWOOD_TREE("Hardwood trees", "hardwood tree"),
    CALQUAT("Calquat trees", "calquat tree"), CELASTRUS("Celastrus trees", "celastrus tree"),
    REDWOOD("Redwood trees", "redwood tree"), CRYSTAL_TREE("Crystal trees", "crystal tree"), SPIRIT_TREE("Spirit trees", "spirit tree");
    private final String label;
    private final String patchLabel;
    TreeFamily(String label, String patchLabel) { this.label = label; this.patchLabel = patchLabel; }
    public String getLabel() { return label; }
    public String getPatchLabel() { return patchLabel; }
}
