package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.loadout.*;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombinedRunPlannerTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final Set<CombinedRunKind> selected=EnumSet.of(CombinedRunKind.GARDEN,CombinedRunKind.FIELD);
    private RequirementContext context(Map<Integer,Long> inventory,Map<Integer,Long> bank,SpellbookType book)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(Quest.LUNAR_DIPLOMACY,QuestState.FINISHED),Map.of(),Map.of()).withRealAttackLevel(99);
        return new RequirementContext(account,new FarmingNavigatorConfig(){},new TravelCapabilitySnapshot(true,book,null,
                inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().intValue())),Map.of(),Map.of()),new StorageSnapshot(inventory,Map.of(),bank,true,now));
    }
    private GardenRunPlan garden(RequirementContext context,GardenRunCompost compost)
    {
        Map<GardenPatchDefinition,List<VerifiedTransportResult>> routes=new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),context.getStorageSnapshot(),new GardenPatchTransportService().getAllOptions());
        return new GardenRunPlanner().plan(context.getAccountState(),Set.of(GardenPatchDefinition.FARMING_GUILD_BUSH),GardenRunCrop.REDBERRIES,GardenRunMode.REPLACE_MATURE,true,compost,Map.of(),context.getStorageSnapshot(),now)
                .withTransports(routes,HerbTransportPreference.CATALOGUE,context);
    }
    private FieldRunPlan field(RequirementContext context,FieldRunCompost compost)
    {
        Map<FieldPatchDefinition,List<VerifiedTransportResult>> routes=new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),context.getStorageSnapshot(),new FieldPatchTransportService().getAllOptions());
        return new FieldRunPlanner().plan(context.getAccountState(),Set.of(FieldPatchDefinition.FARMING_GUILD_NORTH),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,true,compost,Map.of(),context.getStorageSnapshot(),now)
                .withTransports(routes,HerbTransportPreference.CATALOGUE,context);
    }
    private CombinedRunPlan combined(RequirementContext context,GardenRunCompost garden,FieldRunCompost field)
    {
        return new CombinedRunPlanner().plan(selected,null,null,garden(context,garden),field(context,field),context);
    }
    @Test public void applicationsAndFarmerPaymentCannotDoubleCountCompostWhileToolsAreShared()
    {
        for(boolean bankExtra:new boolean[]{false,true})
        {
            RequirementContext context=context(Map.of(ItemID.BUCKET_COMPOST,3L,ItemID.SPADE,1L),bankExtra?Map.of(ItemID.Cert.BUCKET_COMPOST,1L):Map.of(),SpellbookType.STANDARD);
            CombinedRunPlan result=combined(context,GardenRunCompost.COMPOST,FieldRunCompost.COMPOST); assertTrue(result.getBlockers().isEmpty());
            List<SupplyCheck> compost=result.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==ItemID.BUCKET_COMPOST).collect(Collectors.toList());
            assertEquals(2,compost.size()); assertEquals(2,compost.get(0).getRequirement().getQuantity()); assertEquals(SupplyState.ALREADY_CARRIED,compost.get(0).getState());
            assertEquals(2,compost.get(1).getRequirement().getQuantity());
            assertEquals(bankExtra?SupplyState.AVAILABLE_FROM_BANK:SupplyState.MISSING,compost.get(1).getState());
            assertEquals(bankExtra?1:0,compost.get(1).getWithdrawQuantity()); assertEquals(bankExtra?0:1,compost.get(1).getMissingQuantity());
            SupplyCheck spade=result.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==ItemID.SPADE).findFirst().get();
            assertEquals(1,spade.getRequirement().getQuantity()); assertEquals(SupplyState.ALREADY_CARRIED,spade.getState());
        }
    }
    @Test public void individuallyUsablePendantChargeDoesNotCoverTwoComponentArrivals()
    {
        RequirementContext context=context(Map.of(ItemID.JEWL_NECKLACE_OF_SKILLS_1,1L),Map.of(),SpellbookType.STANDARD);
        GardenRunPlan garden=garden(context,GardenRunCompost.NONE); FieldRunPlan field=field(context,FieldRunCompost.NONE);
        assertTrue(garden.getTransportResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        assertTrue(field.getTransportResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        CombinedRunPlan result=new CombinedRunPlanner().plan(selected,null,null,garden,field,context);
        assertEquals(2,result.getStops().size()); assertTrue(result.getResources().getChecks().stream().anyMatch(c -> c.getState()==RequirementState.BLOCKED));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("All component arrival legs remain budgeted")));
    }
    @Test public void careSpellsAcrossComponentsShareOneRunePool()
    {
        RequirementContext context=context(Map.of(ItemID.EARTHRUNE,30L,ItemID.ASTRALRUNE,5L,ItemID.NATURERUNE,4L),Map.of(),SpellbookType.LUNAR);
        GardenRunPlan garden=garden(context,GardenRunCompost.FERTILE_SOIL_SUPER); FieldRunPlan field=field(context,FieldRunCompost.FERTILE_SOIL_SUPER);
        assertTrue(garden.getTransportResources().getChecks().get(0).isReadyNow()); assertTrue(field.getTransportResources().getChecks().get(0).isReadyNow());
        CombinedRunPlan result=new CombinedRunPlanner().plan(selected,null,null,garden,field,context);
        assertEquals(RequirementState.BLOCKED,result.getResources().getChecks().get(0).getState());
    }
    @Test public void sameBottomlessTypeSharesOneBucketButDifferentTypesCannotUseTheSameItem()
    {
        RequirementContext context=context(Map.of(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED,1L),Map.of(),SpellbookType.STANDARD);
        CombinedRunPlan same=combined(context,GardenRunCompost.BOTTOMLESS_ULTRACOMPOST,FieldRunCompost.BOTTOMLESS_ULTRACOMPOST);
        List<SupplyCheck> one=same.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED).collect(Collectors.toList());
        assertEquals(1,one.size()); assertEquals(1,one.get(0).getRequirement().getQuantity()); assertEquals(SupplyState.ALREADY_CARRIED,one.get(0).getState());
        assertTrue(same.getWarnings().stream().anyMatch(w -> w.contains("at least 2 total uses")));
        CombinedRunPlan different=combined(context,GardenRunCompost.BOTTOMLESS_SUPERCOMPOST,FieldRunCompost.BOTTOMLESS_ULTRACOMPOST);
        List<SupplyCheck> two=different.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED).collect(Collectors.toList());
        assertEquals(2,two.size()); assertEquals(1,two.stream().filter(c -> c.getState()==SupplyState.MISSING).count());
    }
    @Test public void incompleteContextOrMissingComponentCannotProduceAPartialReadyPlan()
    {
        assertFalse(new CombinedRunPlanner().plan(selected,null,null,null,null,null).getBlockers().isEmpty());
        RequirementContext context=context(Map.of(),Map.of(),SpellbookType.STANDARD);
        CombinedRunPlan missing=new CombinedRunPlanner().plan(selected,null,null,garden(context,GardenRunCompost.NONE),null,context);
        assertFalse(missing.getBlockers().isEmpty()); assertTrue(missing.getStops().isEmpty()); assertNull(missing.getResources());
        CombinedRunPlan single=new CombinedRunPlanner().plan(Set.of(CombinedRunKind.GARDEN),null,null,garden(context,GardenRunCompost.NONE),null,context);
        assertFalse(single.getBlockers().isEmpty());
    }
    @Test public void combinedSnapshotsAreImmutableAndRetainComponentWarnings()
    {
        CombinedRunPlan result=combined(context(Map.of(),Map.of(),SpellbookType.STANDARD),GardenRunCompost.NONE,FieldRunCompost.NONE);
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("No compost application supplies")));
        try { result.getStops().clear(); fail("Mutable sequence"); } catch(UnsupportedOperationException expected) { }
        try { result.getSupplies().clear(); fail("Mutable supply checks"); } catch(UnsupportedOperationException expected) { }
    }
    @Test public void allFourComponentsRetainTheirMaterialsWithoutConsumingSharedReusableTools()
    {
        RequirementContext context=context(Map.of(ItemID.SPADE,1L,ItemID.FAIRY_ENCHANTED_SECATEURS,1L),Map.of(),SpellbookType.STANDARD);
        HerbRunPlan herb=new HerbRunPlan(HerbRunCrop.GUAM,List.of(new HerbRunStop(com.farmingnavigator.farming.HerbPatchDefinition.FALADOR,"Inspect",null,null,true)),
                new LoadoutService().evaluate(List.of(new SupplyRequirement(ItemID.GUAM_SEED,"Guam seed",1,false,"Plant"),
                        new SupplyRequirement(ItemID.SPADE,"Spade",1,false,"Harvest"),new SupplyRequirement(ItemID.FAIRY_ENCHANTED_SECATEURS,"Magic secateurs",1,true,"Optional yield")),context.getStorageSnapshot()),Map.of(),"");
        herb=herb.withCareResources(HerbRunCareResources.evaluate(herb,HerbRunCure.PLANT_CURE,context))
                .withTransportResources(new RunTransportResourcePlanner().evaluate(herb.getStops(),context));
        TreeRunPlan tree=new TreeRunPlanner().plan(context.getAccountState(),Set.of(com.farmingnavigator.farming.tree.TreePatchDefinition.VARROCK_TREE),TreeRunCrop.OAK,true,Map.of(),context.getStorageSnapshot(),now)
                .withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        com.farmingnavigator.farming.garden.GardenPatchObservation sick=new com.farmingnavigator.farming.garden.GardenPatchObservation(
                new com.farmingnavigator.farming.garden.GardenPatchStateDecoder().decode(com.farmingnavigator.farming.garden.GardenFamily.BUSH,70),now,com.farmingnavigator.farming.state.FarmingGrowthRate.NORMAL);
        GardenRunPlan garden=new GardenRunPlanner().plan(context.getAccountState(),Set.of(GardenPatchDefinition.FARMING_GUILD_BUSH),GardenRunCrop.REDBERRIES,
                GardenRunMode.KEEP_MATURE,true,Map.of(GardenPatchDefinition.FARMING_GUILD_BUSH,sick),context.getStorageSnapshot(),now).withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        CombinedRunPlan result=new CombinedRunPlanner().plan(EnumSet.allOf(CombinedRunKind.class),herb,tree,garden,field(context,FieldRunCompost.NONE),context);
        assertTrue(result.getBlockers().isEmpty()); assertEquals(4,result.getComponents().size()); assertEquals(4,result.getStops().size());
        assertTrue(result.getSupplies().stream().anyMatch(c -> c.getRequirement().getItemId()==ItemID.GUAM_SEED));
        assertTrue(result.getSupplies().stream().anyMatch(c -> c.getRequirement().getItemId()==TreeRunCrop.OAK.getSaplingId()));
        assertTrue(result.getSupplies().stream().filter(c -> Set.of(ItemID.SECATEURS,ItemID.FAIRY_ENCHANTED_SECATEURS,ItemID.SPADE).contains(c.getRequirement().getItemId())).allMatch(c -> c.getState()==SupplyState.ALREADY_CARRIED));
        assertEquals(3,herb.getSupplies().size());
    }

    @Test public void fieldWaterBudgetRemainsVisibleInCombinedPreparation()
    {
        RequirementContext context=context(Map.of(),Map.of(),SpellbookType.STANDARD);
        FieldRunPlan field=new FieldRunPlanner().plan(context.getAccountState(),Set.of(FieldPatchDefinition.FARMING_GUILD_NORTH),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,true,
                FieldRunCompost.NONE,HerbRunCure.PLANT_CURE,FieldRunWatering.AFTER_PLANTING,Map.of(),context.getStorageSnapshot(),now).withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        CombinedRunPlan result=new CombinedRunPlanner().plan(selected,null,null,garden(context,GardenRunCompost.NONE),field,context);
        assertTrue(result.getCareChecks().stream().anyMatch(c -> c.getRequirement().contains("Water for 1") && c.getState()==RequirementState.BLOCKED));
    }

    @Test public void treeSoilCastsJoinTheSameCombinedRuneBudget()
    {
        RequirementContext context=context(Map.of(ItemID.EARTHRUNE,45L,ItemID.ASTRALRUNE,8L,ItemID.NATURERUNE,6L),Map.of(),SpellbookType.LUNAR);
        TreeRunPlan tree=new TreeRunPlanner().plan(context.getAccountState(),Set.of(com.farmingnavigator.farming.tree.TreePatchDefinition.VARROCK_TREE),TreeRunCrop.OAK,true,
                TreeRunCompost.FERTILE_SOIL_SUPER,TreeRunCure.PRUNE,Map.of(),context.getStorageSnapshot(),now).withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        CombinedRunPlan result=new CombinedRunPlanner().plan(Set.of(CombinedRunKind.TREE,CombinedRunKind.GARDEN,CombinedRunKind.FIELD),null,tree,
                garden(context,GardenRunCompost.FERTILE_SOIL_SUPER),field(context,FieldRunCompost.FERTILE_SOIL_SUPER),context);
        assertTrue(tree.getTransportResources().getChecks().get(0).isReadyNow());
        assertEquals(RequirementState.BLOCKED,result.getResources().getChecks().get(0).getState());
    }

}
