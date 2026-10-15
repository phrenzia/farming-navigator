package com.farmingnavigator.routing.run;

public enum HerbTransportPreference
{
    CATALOGUE("Catalogue ranking"),
    REUSABLE_FIRST("Prefer reusable transport");

    private final String label;

    HerbTransportPreference(String label)
    {
        this.label = label;
    }

    @Override public String toString() { return label; }
}
