package com.farmingnavigator.routing.spirit;

/** Player-declared current access only. Observation history never sets these declarations. */
public enum PlantedSpiritTree
{
    PORT_SARIM("Port Sarim"), HOSIDIUS("Hosidius"), BRIMHAVEN("Brimhaven"), FARMING_GUILD("Farming Guild");
    private final String label;
    PlantedSpiritTree(String label) { this.label=label; }
    @Override public String toString() { return label; }
}
