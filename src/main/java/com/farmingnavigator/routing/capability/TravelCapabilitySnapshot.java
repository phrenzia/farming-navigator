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
            inventoryQuantities;

    private final Map<Integer, Integer>
            equipmentQuantities;

    private final Map<Integer, Integer>
            runePouchQuantities;

    /*
     * Compatibility constructor used by the previous milestone
     * and its tests.
     */
    public TravelCapabilitySnapshot(
            boolean loggedIn,
            SpellbookType activeSpellbook,
            WorldPoint currentLocation,
            Map<Integer, Integer> runePouchQuantities
    )
    {
        this(
                loggedIn,
                activeSpellbook,
                currentLocation,
                Collections.emptyMap(),
                Collections.emptyMap(),
                runePouchQuantities
        );
    }

    public TravelCapabilitySnapshot(
            boolean loggedIn,
            SpellbookType activeSpellbook,
            WorldPoint currentLocation,
            Map<Integer, Integer> inventoryQuantities,
            Map<Integer, Integer> equipmentQuantities,
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

        this.inventoryQuantities =
                createImmutableCopy(
                        inventoryQuantities
                );

        this.equipmentQuantities =
                createImmutableCopy(
                        equipmentQuantities
                );

        this.runePouchQuantities =
                createImmutableCopy(
                        runePouchQuantities
                );
    }

    public static TravelCapabilitySnapshot loggedOut()
    {
        return new TravelCapabilitySnapshot(
                false,
                SpellbookType.UNKNOWN,
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
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
    getInventoryQuantities()
    {
        return inventoryQuantities;
    }

    public Map<Integer, Integer>
    getEquipmentQuantities()
    {
        return equipmentQuantities;
    }

    public Map<Integer, Integer>
    getRunePouchQuantities()
    {
        return runePouchQuantities;
    }

    public int getInventoryQuantity(
            int itemId
    )
    {
        return inventoryQuantities.getOrDefault(
                itemId,
                0
        );
    }

    public int getEquipmentQuantity(
            int itemId
    )
    {
        return equipmentQuantities.getOrDefault(
                itemId,
                0
        );
    }

    public int getCarriedQuantity(
            int itemId
    )
    {
        return getInventoryQuantity(
                itemId
        )
                + getEquipmentQuantity(
                itemId
        );
    }

    public boolean hasInInventory(
            int itemId
    )
    {
        return getInventoryQuantity(
                itemId
        ) > 0;
    }

    public boolean isEquipped(
            int itemId
    )
    {
        return getEquipmentQuantity(
                itemId
        ) > 0;
    }

    public boolean hasCarriedItem(
            int itemId
    )
    {
        return getCarriedQuantity(
                itemId
        ) > 0;
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

    private Map<Integer, Integer>
    createImmutableCopy(
            Map<Integer, Integer> source
    )
    {
        Map<Integer, Integer> copy =
                new LinkedHashMap<>();

        if (source != null)
        {
            for (
                    Map.Entry<Integer, Integer> entry
                    : source.entrySet()
            )
            {
                Integer itemId =
                        entry.getKey();

                Integer quantity =
                        entry.getValue();

                if (
                        itemId == null
                                || itemId <= 0
                                || quantity == null
                                || quantity <= 0
                )
                {
                    continue;
                }

                copy.merge(
                        itemId,
                        quantity,
                        Integer::sum
                );
            }
        }

        return Collections.unmodifiableMap(
                copy
        );
    }
}