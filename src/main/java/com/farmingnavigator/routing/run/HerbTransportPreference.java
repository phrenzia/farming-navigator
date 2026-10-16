package com.farmingnavigator.routing.run;

public enum HerbTransportPreference
{
    CATALOGUE("Catalogue ranking"),
    REUSABLE_FIRST("Prefer reusable transport"),
    SHARED_BUDGET("Repair shared budget (one arrival)"),
    SHARED_BUDGET_PAIR("Repair shared budget (up to two arrivals)");

    private final String label;

    HerbTransportPreference(String label)
    {
        this.label = label;
    }

    public static boolean repairsSharedBudget(HerbTransportPreference preference)
    {
        return preference==SHARED_BUDGET || preference==SHARED_BUDGET_PAIR;
    }

    @Override public String toString() { return label; }
}
