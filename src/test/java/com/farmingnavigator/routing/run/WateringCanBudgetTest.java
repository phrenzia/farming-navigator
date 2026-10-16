package com.farmingnavigator.routing.run;

import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.routing.requirement.RequirementState;
import java.time.Instant;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class WateringCanBudgetTest
{
    private StorageSnapshot storage(Map<Integer,Long> carried,Map<Integer,Long> bank,boolean verified)
    { return new StorageSnapshot(carried,Map.of(),bank,verified,Instant.parse("2026-10-16T12:00:00Z")); }
    @Test public void mixedFilledCanFormsContributeTheirActualDoses()
    {
        assertTrue(WateringCanBudget.evaluate(8,storage(Map.of(ItemID.WATERING_CAN_7,1L,ItemID.WATERING_CAN_1,1L),Map.of(),true)).getCheck().isReadyNow());
        assertEquals(RequirementState.BLOCKED,WateringCanBudget.evaluate(9,storage(Map.of(ItemID.WATERING_CAN_8,1L),Map.of(),true)).getCheck().getState());
        assertEquals(RequirementState.BLOCKED,WateringCanBudget.evaluate(2,storage(Map.of(ItemID.WATERING_CAN_1,1L),Map.of(),true)).getCheck().getState());
    }
    @Test public void verifiedBankWaterCanCoverOnlyTheRemainingApplications()
    {
        WateringCanBudget result=WateringCanBudget.evaluate(9,storage(Map.of(ItemID.WATERING_CAN_8,1L),Map.of(ItemID.WATERING_CAN_1,1L),true));
        assertTrue(result.getCheck().requiresBankWithdrawal()); assertFalse(result.getCheck().isReadyNow()); assertEquals(Set.of(ItemID.WATERING_CAN_1),result.getBankItems());
        assertTrue(result.getCheck().getDetail().contains("at least 1 more"));
        assertEquals(RequirementState.DATA_REQUIRED,WateringCanBudget.evaluate(9,storage(Map.of(ItemID.WATERING_CAN_8,1L),Map.of(ItemID.WATERING_CAN_1,1L),false)).getCheck().getState());
    }
    @Test public void emptyCansGricollerAndEquipmentDoNotInventWater()
    {
        StorageSnapshot storage=new StorageSnapshot(Map.of(ItemID.WATERING_CAN_0,1L,ItemID.ZEAH_WATERINGCAN,1L),Map.of(ItemID.WATERING_CAN_8,1L),Map.of(),true,Instant.now());
        assertEquals(RequirementState.BLOCKED,WateringCanBudget.evaluate(1,storage).getCheck().getState());
        assertFalse(WateringCanBudget.getFilledCanIds().contains(ItemID.WATERING_CAN_0)); assertFalse(WateringCanBudget.getFilledCanIds().contains(ItemID.ZEAH_WATERINGCAN));
    }
    @Test public void noApplicationsNeedsNoWaterAndHugeStacksCannotOverflow()
    {
        assertTrue(WateringCanBudget.evaluate(0,null).getCheck().isReadyNow());
        assertTrue(WateringCanBudget.evaluate(Integer.MAX_VALUE,storage(Map.of(ItemID.WATERING_CAN_8,Long.MAX_VALUE),Map.of(),true)).getCheck().isReadyNow());
        assertTrue(WateringCanBudget.evaluate(9,storage(Map.of(),Map.of(ItemID.WATERING_CAN_8,Long.MAX_VALUE),true)).getCheck().requiresBankWithdrawal());
        try { WateringCanBudget.evaluate(-1,null); fail("Negative budget"); } catch(IllegalArgumentException expected) { }
        try { WateringCanBudget.getFilledCanIds().clear(); fail("Mutable can IDs"); } catch(UnsupportedOperationException expected) { }
    }
}
