package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.economics.*;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.loadout.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenCompostTest
{
    private final Instant now = Instant.parse("2026-10-16T12:00:00Z");
    private final AccountState account = new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
    private final GardenPatchDefinition patch = GardenPatchDefinition.RIMMINGTON_BUSH;
    private GardenRunPlan plan(GardenRunCompost compost, GardenRunMode mode, Map<GardenPatchDefinition,GardenPatchObservation> observations, StorageSnapshot storage)
    {
        return new GardenRunPlanner().plan(account,EnumSet.of(patch,GardenPatchDefinition.CHAMPIONS_GUILD_BUSH),
                GardenRunCrop.REDBERRIES,mode,true,compost,observations,storage,now);
    }
    private GardenPatchObservation observed(int raw)
    {
        return new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH,raw),now,FarmingGrowthRate.NORMAL);
    }
    private SupplyCheck supply(GardenRunPlan plan,int id)
    {
        return plan.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==id).findFirst().get();
    }
    @Test public void standardBucketsReserveEachPossiblePlantingAndAcceptOnlyUnnotedFilledBuckets()
    {
        for(GardenRunCompost compost:List.of(GardenRunCompost.COMPOST,GardenRunCompost.SUPERCOMPOST,GardenRunCompost.ULTRACOMPOST))
        {
            GardenRunPlan plan=plan(compost,GardenRunMode.REPLACE_MATURE,Map.of(),null);
            SupplyCheck check=supply(plan,compost.getItemId());
            assertEquals(2,check.getRequirement().getQuantity()); assertEquals(Set.of(compost.getItemId()),check.getRequirement().getAcceptedItemIds());
            assertEquals(compost,plan.withTransports(Map.of(),HerbTransportPreference.CATALOGUE,null).getCompost());
        }
    }
    @Test public void keptMatureAndDiseaseOnlyStopsReserveNoApplicationBucket()
    {
        for(int raw:new int[]{250,14,70})
        {
            Map<GardenPatchDefinition,GardenPatchObservation> observations=Map.of(patch,observed(raw),GardenPatchDefinition.CHAMPIONS_GUILD_BUSH,observed(raw));
            GardenRunPlan plan=plan(GardenRunCompost.ULTRACOMPOST,GardenRunMode.KEEP_MATURE,observations,null);
            assertFalse(plan.getSupplies().stream().anyMatch(c -> c.getRequirement().getItemId()==ItemID.BUCKET_ULTRACOMPOST));
        }
        GardenRunPlan replacements=plan(GardenRunCompost.ULTRACOMPOST,GardenRunMode.REPLACE_MATURE,Map.of(patch,observed(14)),null);
        assertEquals(2,supply(replacements,ItemID.BUCKET_ULTRACOMPOST).getRequirement().getQuantity());
    }
    @Test public void bottomlessRequiresOneFilledBucketAndManualTypeAndUseCheck()
    {
        for(GardenRunCompost compost:List.of(GardenRunCompost.BOTTOMLESS_COMPOST,GardenRunCompost.BOTTOMLESS_SUPERCOMPOST,GardenRunCompost.BOTTOMLESS_ULTRACOMPOST))
        {
            StorageSnapshot empty=new StorageSnapshot(Map.of(ItemID.BOTTOMLESS_COMPOST_BUCKET,1L),Map.of(),Map.of(),true,now);
            GardenRunPlan plan=plan(compost,GardenRunMode.REPLACE_MATURE,Map.of(),empty);
            SupplyCheck check=supply(plan,ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);
            assertEquals(1,check.getRequirement().getQuantity()); assertTrue(check.getRequirement().getNote().contains("at least 2 uses"));
            assertFalse(check.getRequirement().getAcceptedItemIds().contains(ItemID.BOTTOMLESS_COMPOST_BUCKET));
            assertEquals(1,check.getMissingQuantity());
        }
    }
    @Test public void valuesIncludeStandardUseButExcludeReusableBottomlessBucketPrice()
    {
        FarmingMaterialEstimator estimator=new FarmingMaterialEstimator();
        FarmingMaterialEstimate ordinary=estimator.gardens(plan(GardenRunCompost.ULTRACOMPOST,GardenRunMode.REPLACE_MATURE,Map.of(),null),id -> 10);
        assertTrue(ordinary.getLines().stream().anyMatch(l -> l.getItemId()==ItemID.BUCKET_ULTRACOMPOST && l.getQuantity()==2));
        FarmingMaterialEstimate bottomless=estimator.gardens(plan(GardenRunCompost.BOTTOMLESS_ULTRACOMPOST,GardenRunMode.REPLACE_MATURE,Map.of(),null),id -> 10);
        assertFalse(bottomless.getLines().stream().anyMatch(l -> l.getItemId()==ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED));
        assertTrue(bottomless.getNotes().stream().anyMatch(n -> n.contains("use value is unknown")));
    }
}

