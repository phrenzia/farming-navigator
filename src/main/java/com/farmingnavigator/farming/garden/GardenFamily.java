package com.farmingnavigator.farming.garden;

public enum GardenFamily
{
    BUSH("Bushes"), CACTUS("Cacti");
    private final String label;
    GardenFamily(String label) { this.label = label; }
    public String getLabel() { return label; }
}
