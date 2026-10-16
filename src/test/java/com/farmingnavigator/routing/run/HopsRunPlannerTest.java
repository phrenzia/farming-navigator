package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class HopsRunPlannerTest
{
    private final Instant now=Instant.parse("2026-10-15T12:00:00Z");
    private final AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
    private SupplyCheck supply(FieldRunPlan plan,int id)
    {
        return plan.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==id).findFirst().get();
    }
    @Test public void traditionalHopsUseExactSeedAndPaymentCountsAndExcludeUnverifiedIslands()
    {
        FieldRunCrop[] crops={FieldRunCrop.BARLEY,FieldRunCrop.HAMMERSTONE,FieldRunCrop.ASGARNIAN,FieldRunCrop.JUTE,FieldRunCrop.YANILLIAN,FieldRunCrop.KRANDORIAN,FieldRunCrop.WILDBLOOD};
        int[] paymentIds={ItemID.BUCKET_COMPOST,ItemID.MARIGOLD,ItemID.SACK_ONION_10,ItemID.BARLEY_MALT,ItemID.BASKET_TOMATO_5,ItemID.SACK_CABBAGE_10,ItemID.NASTURTIUM};
        int[] amounts={3,1,1,6,1,3,1};
        for(int i=0;i<crops.length;i++)
        {
            FieldRunCrop crop=crops[i];
            FieldRunPlan plan=new FieldRunPlanner().plan(account,EnumSet.allOf(FieldPatchDefinition.class),crop,FieldRunMode.KEEP_MATURE_FLOWERS,true,Map.of(),null,now);
            assertEquals(3,plan.getStops().size()); assertEquals(2,plan.getExcluded().size());
            assertTrue(plan.getExcluded().containsKey(FieldPatchDefinition.ENTRANA_HOPS)); assertTrue(plan.getExcluded().containsKey(FieldPatchDefinition.ALDARIN_HOPS));
            assertEquals(crop==FieldRunCrop.JUTE?9:12,supply(plan,crop.getSeedId()).getRequirement().getQuantity());
            assertEquals(3L*amounts[i],supply(plan,paymentIds[i]).getRequirement().getQuantity());
            assertEquals(2,supply(plan,paymentIds[i]).getRequirement().getAcceptedItemIds().size());
            assertTrue(plan.getStops().stream().allMatch(FieldRunStop::isReplant));
        }
    }
    @Test public void barleyApplicationAndPaymentCompostShareStockWithoutDoubleCounting()
    {
        FieldRunPlan plan=new FieldRunPlanner().plan(account,EnumSet.allOf(FieldPatchDefinition.class),FieldRunCrop.BARLEY,FieldRunMode.HARVEST_ALL,
                true,FieldRunCompost.COMPOST,Map.of(),new StorageSnapshot(Map.of(ItemID.BUCKET_COMPOST,10L),Map.of(),Map.of(),true,now),now);
        java.util.List<SupplyCheck> compost=new java.util.ArrayList<>(); plan.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==ItemID.BUCKET_COMPOST).forEach(compost::add);
        assertEquals(3,compost.get(0).getRequirement().getQuantity()); assertEquals(SupplyState.ALREADY_CARRIED,compost.get(0).getState());
        assertEquals(9,compost.get(1).getRequirement().getQuantity()); assertEquals(SupplyState.MISSING,compost.get(1).getState()); assertEquals(2,compost.get(1).getMissingQuantity());
    }
    @Test public void bottomlessBucketCannotPayBarleyFarmer()
    {
        FieldRunPlan plan=new FieldRunPlanner().plan(account,EnumSet.of(FieldPatchDefinition.LUMBRIDGE_HOPS),FieldRunCrop.BARLEY,FieldRunMode.HARVEST_ALL,
                true,FieldRunCompost.BOTTOMLESS_COMPOST,Map.of(),new StorageSnapshot(Map.of(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED,1L),Map.of(),Map.of(),true,now),now);
        assertEquals(SupplyState.ALREADY_CARRIED,supply(plan,ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED).getState());
        assertEquals(SupplyState.MISSING,supply(plan,ItemID.BUCKET_COMPOST).getState()); assertEquals(3,supply(plan,ItemID.BUCKET_COMPOST).getMissingQuantity());
    }

}
