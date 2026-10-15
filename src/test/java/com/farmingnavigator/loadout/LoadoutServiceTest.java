package com.farmingnavigator.loadout;

import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
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

    @Test
    public void countsNotedAndUnnotedEquivalentPaymentFormsTogether()
    {
        SupplyRequirement requirement = new SupplyRequirement(ItemID.COCONUT, "Coconuts", 25, false, "Protection payment",
                ItemID.Cert.COCONUT);
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.COCONUT, 5L, ItemID.Cert.COCONUT, 10L),
                Collections.emptyMap(), Map.of(ItemID.COCONUT, 10L), true, Instant.now());
        SupplyCheck check = new LoadoutService().evaluate(Collections.singletonList(requirement), storage).get(0);
        assertEquals(SupplyState.AVAILABLE_FROM_BANK, check.getState());
        assertEquals(10, check.getWithdrawQuantity());
        assertEquals(0, check.getMissingQuantity());
    }

    @Test
    public void duplicateEquivalentIdsDoNotCountTheSameStackTwice()
    {
        SupplyRequirement requirement = new SupplyRequirement(ItemID.COCONUT, "Coconuts", 25, false, "",
                ItemID.COCONUT, ItemID.Cert.COCONUT, ItemID.Cert.COCONUT);
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.COCONUT, 10L, ItemID.Cert.COCONUT, 5L),
                Collections.emptyMap(), Collections.emptyMap(), true, Instant.now());
        SupplyCheck check = new LoadoutService().evaluate(Collections.singletonList(requirement), storage).get(0);
        assertEquals(10, check.getMissingQuantity());
    }

    @Test
    public void equivalentPaymentTotalsSaturateInsteadOfOverflowing()
    {
        SupplyRequirement requirement = new SupplyRequirement(ItemID.COCONUT, "Coconuts", Long.MAX_VALUE, false, "",
                ItemID.Cert.COCONUT);
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.COCONUT, Long.MAX_VALUE, ItemID.Cert.COCONUT, Long.MAX_VALUE),
                Collections.emptyMap(), Collections.emptyMap(), true, Instant.now());
        assertEquals(SupplyState.ALREADY_CARRIED, new LoadoutService().evaluate(Collections.singletonList(requirement), storage).get(0).getState());
    }

    @Test
    public void notedSaplingDoesNotCountUnlessExplicitlyDeclaredEquivalent()
    {
        SupplyRequirement sapling = new SupplyRequirement(ItemID.PLANTPOT_OAK_SAPLING, "Oak sapling", 1, false, "Plantable form only");
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.Cert.PLANTPOT_OAK_SAPLING, 1L),
                Collections.emptyMap(), Collections.emptyMap(), true, Instant.now());
        assertEquals(SupplyState.MISSING, new LoadoutService().evaluate(Collections.singletonList(sapling), storage).get(0).getState());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidEquivalentItemId()
    {
        new SupplyRequirement(ItemID.COCONUT, "Coconuts", 1, false, "", 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void acceptedVariantsAreImmutable()
    {
        new SupplyRequirement(ItemID.COCONUT, "Coconuts", 1, false, "").getAcceptedItemIds().clear();
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
