package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenSpellCareTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RequirementContext context(boolean tome,SpellbookType book,int magic)
    {
        AccountState account=new AccountState(true,99,99,magic,magic,99,99,99,99,Map.of(Quest.LUNAR_DIPLOMACY,QuestState.FINISHED),Map.of(),Map.of()).withRealAttackLevel(99);
        return new RequirementContext(account,new FarmingNavigatorConfig(){ @Override public boolean gardenAshTomeRead() { return tome; } },
                new TravelCapabilitySnapshot(true,book,null,Map.of(ItemID.ASTRALRUNE,20,ItemID.NATURERUNE,20),Map.of(),Map.of()),
                new StorageSnapshot(Map.of(ItemID.ASTRALRUNE,20L,ItemID.NATURERUNE,20L),Map.of(),Map.of(ItemID.STAFF_OF_EARTH,1L),true,now));
    }
    private GardenRunPlan plan(RequirementContext context,GardenRunCompost compost,GardenRunMode mode,Map<GardenPatchDefinition,GardenPatchObservation> history)
    {
        return new GardenRunPlanner().plan(context.getAccountState(),EnumSet.allOf(GardenPatchDefinition.class),GardenRunCrop.REDBERRIES,mode,true,compost,HerbRunCure.CURE_PLANT,history,context.getStorageSnapshot(),now)
                .withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
    }
    private Map<GardenPatchDefinition,GardenPatchObservation> observations(int raw)
    {
        Map<GardenPatchDefinition,GardenPatchObservation> result=new EnumMap<>(GardenPatchDefinition.class);
        for(GardenPatchDefinition patch:GardenPatchDefinition.values())
        { if(patch.getFamily()==GardenFamily.BUSH) { result.put(patch,new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH,raw),now,FarmingGrowthRate.NORMAL)); } }
        return result;
    }
    @Test public void multipleDiseasedBushesCountCastsRatherThanOneReusablePruningTool()
    {
        RequirementContext context=context(true,SpellbookType.LUNAR,99);
        GardenRunPlan plan=plan(context,GardenRunCompost.FERTILE_SOIL_ULTRA,GardenRunMode.REPLACE_MATURE,observations(70));
        assertEquals(4,plan.getStops().size()); assertEquals(32,plan.getCareResources().getRunes().getQuantity(RuneKind.EARTH));
        assertEquals(4,plan.getCareResources().getRunes().getQuantity(RuneKind.ASTRAL));
        assertTrue(plan.getSupplies().stream().noneMatch(c -> Set.of(ItemID.SECATEURS,ItemID.PLANT_CURE,ItemID.FOSSIL_VOLCANIC_ASH).contains(c.getRequirement().getItemId())));
        assertTrue(plan.getTransportResources().getBankItems().contains(ItemID.STAFF_OF_EARTH));
        assertTrue(plan.getStops().stream().allMatch(stop -> stop.getAction().contains("Cure Plant")));
        GardenRunPlan refreshed=plan.withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        assertEquals(32,refreshed.getCareResources().getRunes().getQuantity(RuneKind.EARTH));
    }
    @Test public void keptMatureBushesReserveNeitherCureNorCompostCasts()
    {
        GardenRunPlan plan=plan(context(true,SpellbookType.LUNAR,99),GardenRunCompost.FERTILE_SOIL_ULTRA,GardenRunMode.KEEP_MATURE,observations(14));
        assertTrue(plan.getCareResources().getRunes().isEmpty()); assertTrue(plan.getCareResources().getChecks().isEmpty());
        assertEquals(1,plan.getSupplies().size()); assertEquals(ItemID.SPADE,plan.getSupplies().get(0).getRequirement().getItemId());
    }
    @Test public void replacementPlantsReserveExactSoilCastsAndAshWithExplicitTome()
    {
        for(boolean tome:new boolean[]{false,true})
        {
            GardenRunPlan plan=plan(context(tome,SpellbookType.LUNAR,99),GardenRunCompost.FERTILE_SOIL_ULTRA,GardenRunMode.REPLACE_MATURE,observations(14));
            assertEquals(60,plan.getCareResources().getRunes().getQuantity(RuneKind.EARTH)); assertEquals(12,plan.getCareResources().getRunes().getQuantity(RuneKind.ASTRAL));
            assertEquals(8,plan.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==ItemID.FOSSIL_VOLCANIC_ASH).findFirst().get().getRequirement().getQuantity());
            assertEquals(tome,plan.getCareResources().getChecks().stream().filter(c -> c.getRequirement().equals("Ash-covered tome")).findFirst().get().isReadyNow());
        }
    }
    @Test public void gardenSpellsCheckBookAndBoostedMagic()
    {
        for(RequirementContext context:List.of(context(true,SpellbookType.STANDARD,99),context(true,SpellbookType.LUNAR,82)))
        {
            GardenRunPlan plan=plan(context,GardenRunCompost.FERTILE_SOIL_SUPER,GardenRunMode.REPLACE_MATURE,Map.of());
            assertTrue(plan.getCareResources().getChecks().stream().anyMatch(c -> !c.isReadyNow()));
        }
    }
}
