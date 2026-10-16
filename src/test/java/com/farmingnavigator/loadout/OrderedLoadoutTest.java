package com.farmingnavigator.loadout;

import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class OrderedLoadoutTest
{
    private final List<SupplyRequirement> requirements = List.of(
            new SupplyRequirement(ItemID.BUCKET_COMPOST, "Apply", 1, false, "Unnoted only"),
            new SupplyRequirement(ItemID.BUCKET_COMPOST, "Pay", 2, false, "Noted accepted", ItemID.Cert.BUCKET_COMPOST));
    private List<SupplyCheck> checks(Map<Integer, Long> inventory, Map<Integer, Long> bank, boolean verified)
    {
        return new LoadoutService().evaluateInOrder(requirements, new StorageSnapshot(inventory, Map.of(), bank, verified, Instant.now()));
    }
    @Test public void theSameTwoBucketsCannotCoverApplicationAndTwoBucketPayment()
    {
        List<SupplyCheck> result = checks(Map.of(ItemID.BUCKET_COMPOST, 2L), Map.of(), true);
        assertEquals(SupplyState.ALREADY_CARRIED, result.get(0).getState()); assertEquals(SupplyState.MISSING, result.get(1).getState());
        assertEquals(1, result.get(1).getMissingQuantity());
    }
    @Test public void unnotedApplicationAndNotedPaymentCanBothBeCarried()
    {
        List<SupplyCheck> result = checks(Map.of(ItemID.BUCKET_COMPOST, 1L, ItemID.Cert.BUCKET_COMPOST, 2L), Map.of(), true);
        assertTrue(result.stream().allMatch(c -> c.getState() == SupplyState.ALREADY_CARRIED));
    }
    @Test public void notedBucketsCannotBeAppliedEvenWhenTotalQuantityIsEnough()
    {
        List<SupplyCheck> result = checks(Map.of(ItemID.Cert.BUCKET_COMPOST, 3L), Map.of(), true);
        assertEquals(SupplyState.MISSING, result.get(0).getState()); assertEquals(1, result.get(0).getMissingQuantity());
        assertEquals(SupplyState.ALREADY_CARRIED, result.get(1).getState());
    }
    @Test public void bankedStrictFormAndCarriedPaymentFormsAreAllocatedSeparately()
    {
        List<SupplyCheck> result = checks(Map.of(ItemID.Cert.BUCKET_COMPOST, 2L), Map.of(ItemID.BUCKET_COMPOST, 1L), true);
        assertEquals(SupplyState.AVAILABLE_FROM_BANK, result.get(0).getState()); assertEquals(1, result.get(0).getWithdrawQuantity());
        assertEquals(SupplyState.ALREADY_CARRIED, result.get(1).getState());
    }
    @Test public void bankCannotSupplyTheSameBucketsTwice()
    {
        List<SupplyCheck> result = checks(Map.of(), Map.of(ItemID.BUCKET_COMPOST, 2L), true);
        assertEquals(1, result.get(0).getWithdrawQuantity()); assertEquals(SupplyState.MISSING, result.get(1).getState());
        assertEquals(1, result.get(1).getWithdrawQuantity()); assertEquals(1, result.get(1).getMissingQuantity());
    }
    @Test public void unknownBankStaysUnknownAndDoesNotExposeItsItems()
    {
        List<SupplyCheck> result = checks(Map.of(ItemID.BUCKET_COMPOST, 2L), Map.of(ItemID.BUCKET_COMPOST, 100L), false);
        assertEquals(SupplyState.ALREADY_CARRIED, result.get(0).getState()); assertEquals(SupplyState.DATA_REQUIRED, result.get(1).getState());
        assertEquals(0, result.get(1).getWithdrawQuantity()); assertEquals(0, result.get(1).getMissingQuantity());
        assertTrue(new LoadoutService().evaluateInOrder(requirements, null).stream().allMatch(c -> c.getState() == SupplyState.DATA_REQUIRED));
    }
    @Test public void allocationDoesNotMutateSnapshotsAndIndependentChecksKeepTheirExistingMeaning()
    {
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.BUCKET_COMPOST, 2L), Map.of(), Map.of(), true, Instant.now());
        List<SupplyCheck> checks = new LoadoutService().evaluateInOrder(requirements, storage);
        assertEquals(2, storage.getInventoryQuantity(ItemID.BUCKET_COMPOST));
        assertTrue(new LoadoutService().evaluate(requirements, storage).stream().allMatch(c -> c.getState() == SupplyState.ALREADY_CARRIED));
        try { checks.clear(); fail("Mutable checks"); } catch (UnsupportedOperationException expected) { }
    }
}
