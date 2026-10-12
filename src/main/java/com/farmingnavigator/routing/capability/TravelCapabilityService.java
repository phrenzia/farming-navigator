package com.farmingnavigator.routing.capability;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

public class TravelCapabilityService
{
    private static final int[] RUNE_POUCH_AMOUNT_VARBITS = {
            VarbitID.RUNE_POUCH_QUANTITY_1,
            VarbitID.RUNE_POUCH_QUANTITY_2,
            VarbitID.RUNE_POUCH_QUANTITY_3,
            VarbitID.RUNE_POUCH_QUANTITY_4
    };

    private static final int[] RUNE_POUCH_RUNE_VARBITS = {
            VarbitID.RUNE_POUCH_TYPE_1,
            VarbitID.RUNE_POUCH_TYPE_2,
            VarbitID.RUNE_POUCH_TYPE_3,
            VarbitID.RUNE_POUCH_TYPE_4
    };

    private final Client client;

    @Inject
    public TravelCapabilityService(
            Client client
    )
    {
        this.client =
                client;
    }

    public TravelCapabilitySnapshot createSnapshot()
    {
        if (
                client.getGameState()
                        != GameState.LOGGED_IN
        )
        {
            return TravelCapabilitySnapshot
                    .loggedOut();
        }

        SpellbookType activeSpellbook =
                SpellbookType.fromVarbitValue(
                        client.getVarbitValue(
                                VarbitID.SPELLBOOK
                        )
                );

        WorldPoint currentLocation =
                readCurrentLocation();

        Map<Integer, Integer>
                inventoryQuantities =
                readContainerQuantities(
                        InventoryID.INV
                );

        Map<Integer, Integer>
                equipmentQuantities =
                readContainerQuantities(
                        InventoryID.WORN
                );

        Map<Integer, Integer>
                runePouchQuantities =
                readRunePouchQuantities(
                        inventoryQuantities
                );

        return new TravelCapabilitySnapshot(
                true,
                activeSpellbook,
                currentLocation,
                inventoryQuantities,
                equipmentQuantities,
                runePouchQuantities
        );
    }

    private WorldPoint readCurrentLocation()
    {
        Player localPlayer =
                client.getLocalPlayer();

        if (localPlayer == null)
        {
            return null;
        }

        return localPlayer
                .getWorldLocation();
    }

    private Map<Integer, Integer>
    readContainerQuantities(
            int containerId
    )
    {
        Map<Integer, Integer> quantities =
                new LinkedHashMap<>();

        ItemContainer container =
                client.getItemContainer(
                        containerId
                );

        if (container == null)
        {
            return quantities;
        }

        for (
                Item item
                : container.getItems()
        )
        {
            if (
                    item == null
                            || item.getId() <= 0
                            || item.getQuantity() <= 0
            )
            {
                continue;
            }

            quantities.merge(
                    item.getId(),
                    item.getQuantity(),
                    Integer::sum
            );
        }

        return quantities;
    }

    private Map<Integer, Integer>
    readRunePouchQuantities(
            Map<Integer, Integer> inventoryQuantities
    )
    {
        Map<Integer, Integer> quantities =
                new LinkedHashMap<>();

        if (
                !hasCarriedRunePouch(
                        inventoryQuantities
                )
        )
        {
            return quantities;
        }

        EnumComposition runePouchEnum =
                client.getEnum(
                        EnumID.RUNEPOUCH_RUNE
                );

        if (runePouchEnum == null)
        {
            return quantities;
        }

        for (
                int index = 0;
                index
                        < RUNE_POUCH_AMOUNT_VARBITS.length;
                index++
        )
        {
            int quantity =
                    client.getVarbitValue(
                            RUNE_POUCH_AMOUNT_VARBITS[
                                    index
                                    ]
                    );

            if (quantity <= 0)
            {
                continue;
            }

            int runeType =
                    client.getVarbitValue(
                            RUNE_POUCH_RUNE_VARBITS[
                                    index
                                    ]
                    );

            if (runeType <= 0)
            {
                continue;
            }

            int itemId =
                    runePouchEnum.getIntValue(
                            runeType
                    );

            if (itemId <= 0)
            {
                continue;
            }

            quantities.merge(
                    itemId,
                    quantity,
                    Integer::sum
            );
        }

        return quantities;
    }

    private boolean hasCarriedRunePouch(
            Map<Integer, Integer> inventoryQuantities
    )
    {
        return hasItem(
                inventoryQuantities,
                ItemID.BH_RUNE_POUCH
        )
                || hasItem(
                inventoryQuantities,
                ItemID.BH_RUNE_POUCH_TROUVER
        )
                || hasItem(
                inventoryQuantities,
                ItemID.DIVINE_RUNE_POUCH
        )
                || hasItem(
                inventoryQuantities,
                ItemID.DIVINE_RUNE_POUCH_TROUVER
        );
    }

    private boolean hasItem(
            Map<Integer, Integer> quantities,
            int itemId
    )
    {
        return quantities.getOrDefault(
                itemId,
                0
        ) > 0;
    }
}