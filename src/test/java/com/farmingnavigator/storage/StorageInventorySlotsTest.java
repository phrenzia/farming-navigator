package com.farmingnavigator.storage;

import java.util.Arrays;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StorageInventorySlotsTest
{
    @Test
    public void fullInventoryAndUnknownContainerAreDifferent()
    {
        Item[] full = new Item[28];
        Arrays.fill(full, new Item(ItemID.UNIDENTIFIED_RANARR, 1));
        assertEquals(0, StorageStateService.countFreeInventorySlots(full));
        assertEquals(-1, StorageStateService.countFreeInventorySlots(null));
    }

    @Test
    public void stacksOccupyOneSlotAndEmptySlotsAreIgnored()
    {
        assertEquals(27, StorageStateService.countFreeInventorySlots(new Item[] {
                new Item(ItemID.RANARR_SEED, 100), null, new Item(-1, 0)}));
        assertEquals(28, StorageStateService.countFreeInventorySlots(new Item[0]));
    }
}
