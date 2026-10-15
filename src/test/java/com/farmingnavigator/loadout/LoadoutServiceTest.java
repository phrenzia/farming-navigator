package com.farmingnavigator.loadout;

import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LoadoutServiceTest
{
    @Test
    public void countsCarriedEquippedAndBankedQuantities()
    {
        assertEquals(SupplyState.ALREADY_CARRIED, check(5, 0, 0, false).getState());
        assertEquals(SupplyState.ALREADY_EQUIPPED, check(0, 5, 0, false).getState());
        assertEquals(SupplyState.ALREADY_EQUIPPED, check(3, 2, 0, true).getState());
        SupplyCheck bank = check(2, 0, 3, true);
        assertEquals(SupplyState.AVAILABLE_FROM_BANK, bank.getState());
        assertEquals(3, bank.getWithdrawQuantity());
        assertEquals(0, bank.getMissingQuantity());
    }

    @Test
    public void partialBankSupplyReportsWithdrawalAndRemainingShortfall()
    {
        SupplyCheck partial = check(1, 0, 2, true);
        assertEquals(SupplyState.MISSING, partial.getState());
        assertEquals(2, partial.getWithdrawQuantity());
        assertEquals(2, partial.getMissingQuantity());
    }

    @Test
    public void unknownBankDoesNotBecomeMissingOrExposeUnverifiedContents()
    {
        SupplyCheck unknown = check(1, 0, 100, false);
        assertEquals(SupplyState.DATA_REQUIRED, unknown.getState());
        assertEquals(0, unknown.getWithdrawQuantity());
        assertEquals(0, unknown.getMissingQuantity());
        assertEquals(SupplyState.DATA_REQUIRED, new LoadoutService()
                .evaluate(Collections.singletonList(requirement()), null).get(0).getState());
    }

    @Test
    public void largeCarriedCountsDoNotOverflow()
    {
        assertEquals(SupplyState.ALREADY_CARRIED, check(Long.MAX_VALUE, Long.MAX_VALUE, 0, true).getState());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void resultListIsImmutable()
    {
        new LoadoutService().evaluate(Collections.singletonList(requirement()), null).clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroSupplyQuantity()
    {
        new SupplyRequirement(ItemID.RANARR_SEED, "Ranarr seed", 0, false, "");
    }

    private SupplyRequirement requirement()
    {
        return new SupplyRequirement(ItemID.RANARR_SEED, "Ranarr seed", 5, false, "One per patch.");
    }

    private SupplyCheck check(long inventory, long equipped, long bank, boolean verified)
    {
        StorageSnapshot storage = new StorageSnapshot(Collections.singletonMap(ItemID.RANARR_SEED, inventory),
                Collections.singletonMap(ItemID.RANARR_SEED, equipped), Collections.singletonMap(ItemID.RANARR_SEED, bank),
                verified, verified ? Instant.now() : null);
        return new LoadoutService().evaluate(Collections.singletonList(requirement()), storage).get(0);
    }
}
