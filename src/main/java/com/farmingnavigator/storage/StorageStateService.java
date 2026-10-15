package com.farmingnavigator.storage;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;

public class StorageStateService
{
    private final Client client;

    private boolean bankOpen;
    private boolean bankVerified;

    private Map<Integer, Long> verifiedBankItems =
            Collections.emptyMap();

    private Instant bankSyncedAt;

    @Inject
    public StorageStateService(
            Client client
    )
    {
        this.client = client;
    }

    public void invalidateBankSnapshot()
    {
        bankOpen = false;
        bankVerified = false;

        verifiedBankItems =
                Collections.emptyMap();

        bankSyncedAt = null;
    }

    public void setBankOpen(
            boolean bankOpen
    )
    {
        this.bankOpen = bankOpen;
    }

    public boolean isBankOpen()
    {
        return bankOpen;
    }

    public boolean isBankVerified()
    {
        return bankVerified;
    }

    public boolean synchronizeBank()
    {
        if (!bankOpen)
        {
            return false;
        }

        ItemContainer bankContainer =
                client.getItemContainer(
                        InventoryID.BANK
                );

        if (bankContainer == null)
        {
            return false;
        }

        verifiedBankItems =
                Collections.unmodifiableMap(
                        readContainer(
                                bankContainer,
                                true
                        )
                );

        bankVerified = true;
        bankSyncedAt = Instant.now();

        return true;
    }

    public StorageSnapshot createSnapshot()
    {
        Map<Integer, Long> inventory =
                readContainer(
                        client.getItemContainer(
                                InventoryID.INV
                        ),
                        false
                );

        Map<Integer, Long> equipment =
                readContainer(
                        client.getItemContainer(
                                InventoryID.WORN
                        ),
                        false
                );

        return new StorageSnapshot(
                inventory,
                equipment,
                verifiedBankItems,
                bankVerified,
                bankSyncedAt
        );
    }

    public int getFreeInventorySlots()
    {
        ItemContainer container = client.getItemContainer(InventoryID.INV);
        return container == null ? -1 : countFreeInventorySlots(container.getItems());
    }

    static int countFreeInventorySlots(Item[] items)
    {
        if (items == null)
        {
            return -1;
        }
        int occupied = 0;
        for (Item item : items)
        {
            if (item != null && item.getId() > 0 && item.getQuantity() > 0)
            {
                occupied++;
            }
        }
        return Math.max(0, 28 - occupied);
    }

    private Map<Integer, Long> readContainer(
            ItemContainer container,
            boolean ignoreBankFillers
    )
    {
        Map<Integer, Long> quantities =
                new HashMap<>();

        if (container == null)
        {
            return quantities;
        }

        for (
                Item item
                : container.getItems()
        )
        {
            if (item == null)
            {
                continue;
            }

            int itemId =
                    item.getId();

            int quantity =
                    item.getQuantity();

            if (
                    itemId <= 0
                            || quantity <= 0
            )
            {
                continue;
            }

            if (
                    ignoreBankFillers
                            && itemId
                            == ItemID.BANK_FILLER
            )
            {
                continue;
            }

            quantities.merge(
                    itemId,
                    (long) quantity,
                    Long::sum
            );
        }

        return quantities;
    }
}
