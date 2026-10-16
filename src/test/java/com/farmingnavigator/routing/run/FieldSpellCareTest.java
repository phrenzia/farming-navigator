package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.economics.FarmingMaterialEstimator;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
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

public class FieldSpellCareTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RequirementContext context(int magic,QuestState lunar,SpellbookType book,Map<Integer,Long> inventory,Map<Integer,Long> bank,boolean tome)
    {
        AccountState account=new AccountState(true,99,99,magic,magic,99,99,99,99,Map.of(Quest.LUNAR_DIPLOMACY,lunar),Map.of(),Map.of()).withRealAttackLevel(99);
        return new RequirementContext(account,new FarmingNavigatorConfig(){ @Override public boolean fieldAshTomeRead() { return tome; } },
                new TravelCapabilitySnapshot(true,book,null,inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().intValue())),Map.of(),Map.of()),
                new StorageSnapshot(inventory,Map.of(),bank,true,now));
    }
    private RequirementContext eligible() { return context(99,QuestState.FINISHED,SpellbookType.LUNAR,Map.of(),Map.of(),true); }
    private FieldPatchObservation observed(FieldFamily family,int raw) { return new FieldPatchObservation(new FieldPatchStateDecoder().decode(family,raw),now,FarmingGrowthRate.NORMAL); }
    private FieldRunPlan plan(RequirementContext context,FieldRunCrop crop,FieldRunCompost compost,HerbRunCure cure,Map<FieldPatchDefinition,FieldPatchObservation> observations)
    {
        Set<FieldPatchDefinition> selected=crop==FieldRunCrop.MARIGOLD?Set.of(FieldPatchDefinition.FALADOR_FLOWER)
                : Set.of(FieldPatchDefinition.CATHERBY_NORTH,FieldPatchDefinition.CATHERBY_SOUTH);
        return new FieldRunPlanner().plan(context.getAccountState(),selected,crop,FieldRunMode.KEEP_MATURE_FLOWERS,true,compost,cure,observations,context.getStorageSnapshot(),now);
    }
    @Test public void exactDiseaseCountsRemovePotionAndNeverCountKeptFlowersAsCureCasts()
    {
        RequirementContext context=eligible(); Map<FieldPatchDefinition,FieldPatchObservation> sick=Map.of(
                FieldPatchDefinition.CATHERBY_NORTH,observed(FieldFamily.ALLOTMENT,135),FieldPatchDefinition.CATHERBY_SOUTH,observed(FieldFamily.ALLOTMENT,135));
        FieldRunPlan plan=plan(context,FieldRunCrop.POTATO,FieldRunCompost.FERTILE_SOIL_ULTRA,HerbRunCure.CURE_PLANT,sick).withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        assertEquals(16,plan.getCareResources().getRunes().getQuantity(RuneKind.EARTH)); assertEquals(2,plan.getCareResources().getRunes().getQuantity(RuneKind.ASTRAL));
        assertTrue(plan.getSupplies().stream().noneMatch(c -> c.getRequirement().getItemId()==ItemID.PLANT_CURE || c.getRequirement().getItemId()==ItemID.FOSSIL_VOLCANIC_ASH));
        FieldRunPlan kept=plan(context,FieldRunCrop.MARIGOLD,FieldRunCompost.FERTILE_SOIL_ULTRA,HerbRunCure.CURE_PLANT,Map.of(FieldPatchDefinition.FALADOR_FLOWER,observed(FieldFamily.FLOWER,12)))
                .withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        assertTrue(kept.getCareResources().getRunes().isEmpty()); assertTrue(kept.getCareResources().getChecks().isEmpty());
        FieldRunPlan refreshed=plan.withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
        assertEquals(16,refreshed.getCareResources().getRunes().getQuantity(RuneKind.EARTH));
    }
    @Test public void plantingCureAndTravelSpendOneSharedAstralPool()
    {
        Map<Integer,Long> inventory=Map.of(ItemID.ASTRALRUNE,6L,ItemID.EARTHRUNE,23L,ItemID.NATURERUNE,2L,ItemID.WATERRUNE,10L,ItemID.LAWRUNE,3L);
        for(boolean bankExtra:new boolean[]{false,true})
        {
            RequirementContext context=context(99,QuestState.FINISHED,SpellbookType.LUNAR,inventory,bankExtra?Map.of(ItemID.ASTRALRUNE,1L):Map.of(),true);
            FieldRunPlan base=plan(context,FieldRunCrop.POTATO,FieldRunCompost.FERTILE_SOIL_SUPER,HerbRunCure.CURE_PLANT,
                    Map.of(FieldPatchDefinition.CATHERBY_SOUTH,observed(FieldFamily.ALLOTMENT,135)));
            TransportOption spell=new TransportOption(1,"Catherby","Test verified arrival",TransportCategory.SPELL,TransportVerificationKind.TELEPORT_SPELL,TeleportSpellDefinition.CATHERBY_TELEPORT);
            FieldRunPlan plan=base.withTransports(Map.of(FieldPatchDefinition.CATHERBY_NORTH,List.of(new VerifiedTransportResult(spell,VerifiedTransportState.READY_NOW,"Verified spell"))),HerbTransportPreference.CATALOGUE,context);
            assertTrue(plan.getStops().get(1).isLocalWalk());
            RequirementResult runes=plan.getTransportResources().getChecks().get(0);
            if (bankExtra) { assertTrue(runes.requiresBankWithdrawal()); } else { assertEquals(RequirementState.BLOCKED,runes.getState()); }
            assertFalse(runes.isReadyNow());
        }
    }
    @Test public void accessChecksRejectWrongBookLowMagicAndUnfinishedLunarDiplomacy()
    {
        for(RequirementContext context:List.of(context(82,QuestState.FINISHED,SpellbookType.LUNAR,Map.of(),Map.of(),true),
                context(99,QuestState.IN_PROGRESS,SpellbookType.LUNAR,Map.of(),Map.of(),true),context(99,QuestState.FINISHED,SpellbookType.STANDARD,Map.of(),Map.of(),true)))
        {
            FieldRunPlan plan=plan(context,FieldRunCrop.POTATO,FieldRunCompost.FERTILE_SOIL_SUPER,HerbRunCure.PLANT_CURE,Map.of()).withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
            assertTrue(plan.getCareResources().getChecks().stream().anyMatch(c -> !c.isReadyNow()));
        }
    }
    @Test public void ultraReservesTwoAshPerPlantingAndNeedsExplicitFieldTomeDeclaration()
    {
        for(boolean tome:new boolean[]{false,true})
        {
            RequirementContext context=context(99,QuestState.FINISHED,SpellbookType.LUNAR,Map.of(),Map.of(),tome);
            FieldRunPlan plan=plan(context,FieldRunCrop.POTATO,FieldRunCompost.FERTILE_SOIL_ULTRA,HerbRunCure.PLANT_CURE,Map.of()).withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
            assertEquals(4,plan.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==ItemID.FOSSIL_VOLCANIC_ASH).findFirst().get().getRequirement().getQuantity());
            assertTrue(plan.getSupplies().stream().noneMatch(c -> c.getRequirement().getItemId()==ItemID.BUCKET_ULTRACOMPOST));
            assertEquals(tome,plan.getCareResources().getChecks().stream().filter(c -> c.getRequirement().equals("Ash-covered tome")).findFirst().get().isReadyNow());
            assertTrue(new FarmingMaterialEstimator().fields(plan,id -> 10).getLines().stream().anyMatch(l -> l.getItemId()==ItemID.FOSSIL_VOLCANIC_ASH && l.getQuantity()==4));
        }
    }
    @Test public void underwaterSpellsAreExcludedWithoutClaimingTheirGameBehavior()
    {
        AccountState account=new AccountState(true,99,99,EnumSet.of(Quest.BONE_VOYAGE),false);
        FieldRunPlan compost=new FieldRunPlanner().plan(account,Set.of(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH),FieldRunCrop.SEAWEED,FieldRunMode.HARVEST_ALL,true,
                FieldRunCompost.FERTILE_SOIL_SUPER,HerbRunCure.PLANT_CURE,Map.of(),null,now);
        assertTrue(compost.getBlocker().contains("unverified")); assertTrue(compost.getStops().isEmpty());
        FieldRunPlan cure=new FieldRunPlanner().plan(account,Set.of(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH),FieldRunCrop.SEAWEED,FieldRunMode.HARVEST_ALL,true,
                FieldRunCompost.NONE,HerbRunCure.CURE_PLANT,Map.of(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH,observed(FieldFamily.SEAWEED,11)),null,now);
        assertFalse(cure.getBlocker().isEmpty());
    }
}
