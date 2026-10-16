package com.farmingnavigator.farming.field;

public enum FieldFamily
{
    ALLOTMENT("Allotments"), FLOWER("Flowers"), HOPS("Hops and fibres"),
    SEAWEED("Seaweed"), MUSHROOM("Mushrooms"), BELLADONNA("Belladonna");
    private final String label;
    FieldFamily(String label) { this.label = label; }
    public String getLabel() { return label; }
}
