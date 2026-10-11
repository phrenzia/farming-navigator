package com.farmingnavigator.storage;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StorageSnapshot
{
    private final Map<Integer, Long> inventoryItems;
    private final Map<Integer, Long> equipmentItems;
    private final Map<Integer, Long> bankItems;
    private final boolean bankVerified;
    private final Instant bankSyncedAt;

    public StorageSnapshot(
            Map<Integer, Long> inventoryItems,
            Map<Integer, Long> equipmentItems,
            Map<Integer, Long> bankItems,
            boolean bankVerified,
            Instant bankSyncedAt
    )
    {
        this.inventoryItems =
                immutableCopy(inventoryItems);

        this.equipmentItems =
                immutableCopy(equipmentItems);

        this.bankItems =
                immutableCopy(bankItems);

        this.bankVerified =
                bankVerified;

        this.bankSyncedAt =
                bankSyncedAt;
    }

    public Map<Integer, Long> getInventoryItems()
    {
        return inventoryItems;
    }

    public Map<Integer, Long> getEquipmentItems()
    {
        return equipmentItems;
    }

    public Map<Integer, Long> getBankItems()
    {
        return bankItems;
    }

    public boolean isBankVerified()
    {
        return bankVerified;
    }

    public Instant getBankSyncedAt()
    {
        return bankSyncedAt;
    }

    public long getInventoryQuantity(
            int itemId
    )
    {
        return inventoryItems.getOrDefault(
                itemId,
                0L
        );
    }

    public long getEquipmentQuantity(
            int itemId
    )
    {
        return equipmentItems.getOrDefault(
                itemId,
                0L
        );
    }

    public long getCarriedQuantity(
            int itemId
    )
    {
        return getInventoryQuantity(itemId)
                + getEquipmentQuantity(itemId);
    }

    public long getVerifiedBankQuantity(
            int itemId
    )
    {
        requireVerifiedBank();

        return bankItems.getOrDefault(
                itemId,
                0L
        );
    }

    public long getVerifiedTotalQuantity(
            int itemId
    )
    {
        requireVerifiedBank();

        return getInventoryQuantity(itemId)
                + getEquipmentQuantity(itemId)
                + getVerifiedBankQuantity(itemId);
    }

    public boolean hasCarriedItem(
            int itemId
    )
    {
        return getCarriedQuantity(itemId) > 0;
    }

    public boolean hasVerifiedItemAnywhere(
            int itemId
    )
    {
        requireVerifiedBank();

        return getVerifiedTotalQuantity(itemId) > 0;
    }

    public int getInventoryDistinctItemCount()
    {
        return inventoryItems.size();
    }

    public int getEquipmentDistinctItemCount()
    {
        return equipmentItems.size();
    }

    public int getBankDistinctItemCount()
    {
        requireVerifiedBank();

        return bankItems.size();
    }

    private void requireVerifiedBank()
    {
        if (!bankVerified)
        {
            throw new IllegalStateException(
                    "Bank data has not been verified for the current login session."
            );
        }
    }

    private static Map<Integer, Long> immutableCopy(
            Map<Integer, Long> source
    )
    {
        if (
                source == null
                        || source.isEmpty()
        )
        {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(
                new HashMap<>(source)
        );
    }
}