package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.loadout.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpecialFieldRunPlannerTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final AccountState account=new AccountState(true,99,99,EnumSet.of(Quest.BONE_VOYAGE,Quest.PRIEST_IN_PERIL),false);
    private FieldRunPlan plan(FieldRunCrop crop,boolean protect,Map<FieldPatchDefinition,FieldPatchObservation> observations,StorageSnapshot storage)
    {
        return new FieldRunPlanner().plan(account,EnumSet.allOf(FieldPatchDefinition.class),crop,FieldRunMode.KEEP_MATURE_FLOWERS,protect,FieldRunCompost.ULTRACOMPOST,observations,storage,now);
    }
    private SupplyCheck supply(FieldRunPlan plan,int id) { return plan.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==id).findFirst().get(); }
    @Test public void seaweedReservesTwoSporesAndFourHundredUnnotedNumulites()
    {
        FieldRunPlan result=plan(FieldRunCrop.SEAWEED,true,Map.of(),new StorageSnapshot(Map.of(ItemID.FOSSIL_NUMULITE,200L),Map.of(),Map.of(ItemID.FOSSIL_NUMULITE,150L),true,now));
        assertEquals(2,result.getStops().size()); assertEquals(2,supply(result,ItemID.SEAWEED_SEED).getRequirement().getQuantity());
        SupplyCheck payment=supply(result,ItemID.FOSSIL_NUMULITE);
        assertEquals(400,payment.getRequirement().getQuantity()); assertEquals(Set.of(ItemID.FOSSIL_NUMULITE),payment.getRequirement().getAcceptedItemIds());
        assertEquals(150,payment.getWithdrawQuantity()); assertEquals(50,payment.getMissingQuantity());
        assertEquals(2,supply(result,ItemID.BUCKET_ULTRACOMPOST).getRequirement().getQuantity());
        assertTrue(supply(result,ItemID.HUNDRED_PIRATE_DIVING_HELMET).getRequirement().isOptional());
        assertTrue(supply(result,ItemID.HUNDRED_PIRATE_DIVING_BACKPACK).getRequirement().isOptional());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("air supply")));
        assertFalse(plan(FieldRunCrop.SEAWEED,false,Map.of(),null).getSupplies().stream().anyMatch(c -> c.getRequirement().getItemId()==ItemID.FOSSIL_NUMULITE));
    }
    @Test public void mushroomAndBelladonnaUseOneSeedAndNeverInventProtection()
    {
        for(FieldRunCrop crop:List.of(FieldRunCrop.MUSHROOM,FieldRunCrop.BELLADONNA))
        {
            FieldRunPlan result=plan(crop,true,Map.of(),null); assertEquals(1,result.getStops().size()); assertFalse(crop.hasFarmerProtection());
            assertEquals(1,supply(result,crop.getSeedId()).getRequirement().getQuantity()); assertFalse(crop.protectionPayment(1).isPresent());
            assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("no farmer protection")));
        }
        FieldRunPlan belladonna=plan(FieldRunCrop.BELLADONNA,true,Map.of(),null);
        assertTrue(belladonna.getExcluded().containsKey(FieldPatchDefinition.AUBURNVALE_BELLADONNA));
        assertTrue(supply(belladonna,ItemID.LEATHER_GLOVES).getRequirement().isOptional());
        assertTrue(belladonna.getWarnings().stream().anyMatch(w -> w.contains("poison damage")));
    }
    @Test public void levelsUse53ForMushroom63ForBelladonnaAnd23ForSeaweed()
    {
        FieldRunCrop[] crops={FieldRunCrop.MUSHROOM,FieldRunCrop.BELLADONNA,FieldRunCrop.SEAWEED}; int[] levels={53,63,23};
        for(int i=0;i<crops.length;i++)
        {
            AccountState low=new AccountState(true,1,levels[i]-1,EnumSet.of(Quest.BONE_VOYAGE,Quest.PRIEST_IN_PERIL),false);
            assertFalse(new FieldRunPlanner().plan(low,EnumSet.allOf(FieldPatchDefinition.class),crops[i],FieldRunMode.HARVEST_ALL,true,Map.of(),null,now).getBlocker().isEmpty());
            assertEquals(levels[i],crops[i].getRequiredLevel()); assertEquals(1,crops[i].getSeedsPerPatch());
        }
    }
    @Test public void diseasedSeaweedUsesCureWithoutReplantPaymentOrCompost()
    {
        Map<FieldPatchDefinition,FieldPatchObservation> observations=new EnumMap<>(FieldPatchDefinition.class);
        for(FieldPatchDefinition patch:List.of(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH,FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_SOUTH))
        { observations.put(patch,new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.SEAWEED,11),now,FarmingGrowthRate.NORMAL)); }
        FieldRunPlan result=plan(FieldRunCrop.SEAWEED,true,observations,null);
        assertEquals(2,supply(result,ItemID.PLANT_CURE).getRequirement().getQuantity()); assertTrue(result.getStops().stream().noneMatch(FieldRunStop::isReplant));
        assertFalse(result.getSupplies().stream().anyMatch(c -> Set.of(ItemID.SEAWEED_SEED,ItemID.FOSSIL_NUMULITE,ItemID.BUCKET_ULTRACOMPOST).contains(c.getRequirement().getItemId())));
    }
}
