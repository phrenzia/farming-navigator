package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.economics.*;
import com.farmingnavigator.farming.tree.*;
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

public class TreeCarePreparationTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RequirementContext context(boolean tome)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(Quest.LUNAR_DIPLOMACY,QuestState.FINISHED,Quest.SONG_OF_THE_ELVES,QuestState.FINISHED),Map.of(),Map.of()).withRealAttackLevel(99).withBoostedWoodcuttingLevel(61);
        return new RequirementContext(account,new FarmingNavigatorConfig(){ @Override public boolean treeAshTomeRead() { return tome; } },
                new TravelCapabilitySnapshot(true,SpellbookType.LUNAR,null,Map.of(ItemID.ASTRALRUNE,20,ItemID.NATURERUNE,20),Map.of(),Map.of()),
                new StorageSnapshot(Map.of(ItemID.ASTRALRUNE,20L,ItemID.NATURERUNE,20L),Map.of(),Map.of(ItemID.STAFF_OF_EARTH,1L),true,now));
    }
    private TreePatchObservation observation(TreeFamily family,int raw) { return new TreePatchObservation(new TreePatchStateDecoder().decode(family,raw),now,FarmingGrowthRate.NORMAL); }
    private TreeRunPlan plan(RequirementContext context,TreeRunCrop crop,TreeRunCompost compost,TreeRunCure cure,Map<TreePatchDefinition,TreePatchObservation> history)
    {
        return new TreeRunPlanner().plan(context.getAccountState(),EnumSet.allOf(TreePatchDefinition.class),crop,true,compost,cure,history,context.getStorageSnapshot(),now)
                .withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
    }
    @Test public void standardBucketsAndBottomlessCountsFollowPotentialPlantings()
    {
        for(TreeRunCompost compost:List.of(TreeRunCompost.COMPOST,TreeRunCompost.ULTRACOMPOST,TreeRunCompost.BOTTOMLESS_ULTRACOMPOST))
        {
            TreeRunPlan plan=plan(context(true),TreeRunCrop.OAK,compost,TreeRunCure.PRUNE,Map.of());
            long replants=plan.getStops().stream().filter(TreeRunStop::isReplant).count(); assertTrue(replants>1);
            com.farmingnavigator.loadout.SupplyRequirement requirement=plan.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==compost.getItemId()).findFirst().get().getRequirement();
            assertEquals(compost.isBottomless()?1:replants,requirement.getQuantity()); assertEquals(Set.of(compost.getItemId()),requirement.getAcceptedItemIds());
            assertEquals(compost,plan.withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context(true)).getCompost());
        }
    }
    @Test public void diseaseOnlyStopsReserveExactCureCastsAndNoReplacementCompost()
    {
        RequirementContext context=context(true); Map<TreePatchDefinition,TreePatchObservation> sick=new EnumMap<>(TreePatchDefinition.class);
        for(TreePatchDefinition patch:TreePatchDefinition.values()) { if(patch.getFamily()==TreeFamily.TREE) { sick.put(patch,observation(TreeFamily.TREE,73)); } }
        TreeRunPlan plan=plan(context,TreeRunCrop.OAK,TreeRunCompost.FERTILE_SOIL_ULTRA,TreeRunCure.CURE_PLANT,sick);
        assertEquals(plan.getStops().size()*8,plan.getCareResources().getRunes().getQuantity(RuneKind.EARTH));
        assertEquals(plan.getStops().size(),plan.getCareResources().getRunes().getQuantity(RuneKind.ASTRAL));
        assertTrue(plan.getSupplies().stream().noneMatch(c -> Set.of(ItemID.SECATEURS,ItemID.FOSSIL_VOLCANIC_ASH,TreeRunCrop.OAK.getSaplingId()).contains(c.getRequirement().getItemId())));
        assertTrue(plan.getTransportResources().getBankItems().contains(ItemID.STAFF_OF_EARTH));
    }
    @Test public void matureRedwoodHasNoCompostCastOrReplacementEvenWithUltraModeSelected()
    {
        TreeRunPlan plan=plan(context(true),TreeRunCrop.REDWOOD,TreeRunCompost.FERTILE_SOIL_ULTRA,TreeRunCure.CURE_PLANT,
                Map.of(TreePatchDefinition.FARMING_GUILD_REDWOOD,observation(TreeFamily.REDWOOD,37)));
        assertEquals(1,plan.getStops().size()); assertFalse(plan.getStops().get(0).isReplant());
        assertTrue(plan.getCareResources().getRunes().isEmpty()); assertTrue(plan.getCareResources().getChecks().isEmpty());
        assertTrue(plan.getSupplies().stream().noneMatch(c -> c.getRequirement().getItemId()==ItemID.FOSSIL_VOLCANIC_ASH));
    }
    @Test public void crystalCompostingRemainsExcludedUntilVerified()
    {
        TreeRunPlan blocked=plan(context(true),TreeRunCrop.CRYSTAL,TreeRunCompost.ULTRACOMPOST,TreeRunCure.PRUNE,Map.of());
        assertTrue(blocked.getBlocker().contains("unverified")); assertTrue(blocked.getStops().isEmpty());
        assertEquals(1,plan(context(true),TreeRunCrop.CRYSTAL,TreeRunCompost.NONE,TreeRunCure.PRUNE,Map.of()).getStops().size());
    }
    @Test public void ultraUsesTwoAshPerPlantAndRequiresItsOwnTomeDeclaration()
    {
        for(boolean tome:new boolean[]{false,true})
        {
            TreeRunPlan plan=plan(context(tome),TreeRunCrop.CELASTRUS,TreeRunCompost.FERTILE_SOIL_ULTRA,TreeRunCure.PRUNE,Map.of());
            assertEquals(2,plan.getSupplies().stream().filter(c -> c.getRequirement().getItemId()==ItemID.FOSSIL_VOLCANIC_ASH).findFirst().get().getRequirement().getQuantity());
            assertEquals(tome,plan.getCareResources().getChecks().stream().filter(c -> c.getRequirement().equals("Ash-covered tome")).findFirst().get().isReadyNow());
            assertTrue(new FarmingMaterialEstimator().trees(plan,true,id -> 10).getLines().stream().anyMatch(l -> l.getItemId()==ItemID.FOSSIL_VOLCANIC_ASH));
        }
        TreeRunPlan bottomless=plan(context(true),TreeRunCrop.CELASTRUS,TreeRunCompost.BOTTOMLESS_ULTRACOMPOST,TreeRunCure.PRUNE,Map.of());
        FarmingMaterialEstimate value=new FarmingMaterialEstimator().trees(bottomless,true,id -> 10);
        assertTrue(value.getLines().stream().noneMatch(l -> l.getItemId()==ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED));
        assertTrue(value.getNotes().stream().anyMatch(n -> n.contains("use value is unknown")));
    }
}
