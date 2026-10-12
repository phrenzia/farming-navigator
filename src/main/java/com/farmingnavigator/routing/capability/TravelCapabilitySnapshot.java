package com.farmingnavigator.routing.capability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

public class TravelCapabilitySnapshot
{
    private final boolean loggedIn;

    private final SpellbookType activeSpellbook;

    private final WorldPoint currentLocation;

    private final Map<Integer, Integer>
            runePouchQuantities;

    public TravelCapabilitySnapshot(
            boolean loggedIn,
            SpellbookType activeSpellbook,
            WorldPoint currentLocation,
            Map<Integer, Integer> runePouchQuantities
    )
    {
        this.loggedIn =
                loggedIn;

        this.activeSpellbook =
                activeSpellbook == null
                        ? SpellbookType.UNKNOWN
                        : activeSpellbook;

        this.currentLocation =
                currentLocation;

        Map<Integer, Integer> runePouchCopy =
                new LinkedHashMap<>();

        if (runePouchQuantities != null)
        {
            runePouchCopy.putAll(
                    runePouchQuantities
            );
        }

        this.runePouchQuantities =
                Collections.unmodifiableMap(
                        runePouchCopy
                );
    }

    public static TravelCapabilitySnapshot loggedOut()
    {
        return new TravelCapabilitySnapshot(
                false,
                SpellbookType.UNKNOWN,
                null,
                Collections.emptyMap()
        );
    }

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    public SpellbookType getActiveSpellbook()
    {
        return activeSpellbook;
    }

    public WorldPoint getCurrentLocation()
    {
        return currentLocation;
    }

    public Map<Integer, Integer>
    getRunePouchQuantities()
    {
        return runePouchQuantities;
    }

    public int getRunePouchQuantity(
            int itemId
    )
    {
        return runePouchQuantities.getOrDefault(
                itemId,
                0
        );
    }

    public boolean hasRuneInPouch(
            int itemId
    )
    {
        return getRunePouchQuantity(
                itemId
        ) > 0;
    }
}