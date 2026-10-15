package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.skillcalculator.skills.FarmingAction;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeRunPlannerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final TreeRunPlanner planner = new TreeRunPlanner();
    private final TreePatchAccess access = new TreePatchAccess();
    private AccountState account(int real, int boosted, Map<Quest, QuestState> quests)
    {
        return new AccountState(true, real, boosted, 99, 99, 99, 99, 99, 99, quests, Map.of(), Map.of());
    }
    private TreeRunPlan plan(TreeRunCrop crop, Map<TreePatchDefinition, TreePatchObservation> observations)
    {
        return planner.plan(account(99, 99, Map.of()), EnumSet.allOf(TreePatchDefinition.class), crop, true, observations, null, now);
    }
    private SupplyCheck supply(TreeRunPlan plan, int id)
    {
        return plan.getSupplies().stream().filter(check -> check.getRequirement().getItemId() == id).findFirst().get();
    }
    private TreePatchObservation observation(TreeFamily family, int raw, Instant at, FarmingGrowthRate rate)
    {
        return new TreePatchObservation(new TreePatchStateDecoder().decode(family, raw), at, rate);
    }
    @Test public void plantingLevelsAndDistinctSaplingsMatchRuneLite()
    {
        Set<Integer> ids = new HashSet<>();
        for (TreeRunCrop crop : TreeRunCrop.values())
        {
            String action = crop == TreeRunCrop.PINEAPPLE ? "PINEAPPLE_PLANT" : crop.name() + "_TREE";
            assertEquals(FarmingAction.valueOf(action).getLevel(), crop.getRequiredLevel());
            assertTrue(ids.add(crop.getSaplingId()));
        }
        assertEquals(18, ids.size());
    }
    @Test public void includesOnlySelectedMatchingAccessiblePatches()
    {
        TreeRunPlan oak = plan(TreeRunCrop.OAK, Map.of());
        assertEquals(6, oak.getStops().size());
        assertEquals(1, oak.getExcluded().size());
        assertTrue(oak.getExcluded().get(TreePatchDefinition.AUBURNVALE_TREE).contains("Data required"));
        assertEquals(6, supply(oak, ItemID.PLANTPOT_OAK_SAPLING).getRequirement().getQuantity());
        assertEquals(6, supply(oak, ItemID.BASKET_TOMATO_5).getRequirement().getQuantity());
        TreeRunPlan selected = planner.plan(account(99, 99, Map.of()), EnumSet.of(TreePatchDefinition.VARROCK_TREE,
                TreePatchDefinition.CATHERBY_FRUIT), TreeRunCrop.OAK, true, Map.of(), null, now);
        assertEquals(1, selected.getStops().size());
        assertEquals(TreePatchDefinition.VARROCK_TREE, selected.getStops().get(0).getPatch());
    }
    @Test public void loginAndBoostedPlantingLevelGateWholePlan()
    {
        TreeRunPlan loggedOut = planner.plan(AccountState.loggedOut(), EnumSet.allOf(TreePatchDefinition.class),
                TreeRunCrop.OAK, true, Map.of(), null, now);
        assertTrue(loggedOut.getBlocker().contains("Log in"));
        TreeRunPlan low = planner.plan(account(14, 14, Map.of()), EnumSet.allOf(TreePatchDefinition.class),
                TreeRunCrop.OAK, true, Map.of(), null, now);
        assertTrue(low.getStops().isEmpty()); assertTrue(low.getSupplies().isEmpty());
        assertTrue(low.getBlocker().contains("15 Farming"));
        assertFalse(planner.plan(account(14, 15, Map.of()), EnumSet.of(TreePatchDefinition.VARROCK_TREE),
                TreeRunCrop.OAK, true, Map.of(), null, now).getStops().isEmpty());
    }
    @Test public void guildAccessUsesBoosted65And85Boundaries()
    {
        assertFalse(access.missingRequirement(TreePatchDefinition.FARMING_GUILD_TREE, account(64, 64, Map.of())).isEmpty());
        assertEquals("", access.missingRequirement(TreePatchDefinition.FARMING_GUILD_TREE, account(64, 65, Map.of())));
        assertFalse(access.missingRequirement(TreePatchDefinition.FARMING_GUILD_FRUIT, account(84, 84, Map.of())).isEmpty());
        assertEquals("", access.missingRequirement(TreePatchDefinition.FARMING_GUILD_FRUIT, account(84, 85, Map.of())));
    }
    @Test public void lletyaRequiresStartedQuestAndIslandRequiresCompletion()
    {
        assertFalse(access.missingRequirement(TreePatchDefinition.LLETYA_FRUIT, account(99, 99, Map.of())).isEmpty());
        assertEquals("", access.missingRequirement(TreePatchDefinition.LLETYA_FRUIT,
                account(99, 99, Map.of(Quest.MOURNINGS_END_PART_I, QuestState.IN_PROGRESS))));
        for (TreePatchDefinition patch : List.of(TreePatchDefinition.FOSSIL_ISLAND_EAST,
                TreePatchDefinition.FOSSIL_ISLAND_MIDDLE, TreePatchDefinition.FOSSIL_ISLAND_WEST))
        {
            assertFalse(access.missingRequirement(patch, account(99, 99, Map.of(Quest.BONE_VOYAGE, QuestState.IN_PROGRESS))).isEmpty());
            assertEquals("", access.missingRequirement(patch, account(99, 99, Map.of(Quest.BONE_VOYAGE, QuestState.FINISHED))));
        }
    }
    @Test public void aviumRequiresCompletedRibbitingTale()
    {
        Quest quest = Quest.THE_RIBBITING_TALE_OF_A_LILY_PAD_LABOUR_DISPUTE;
        assertFalse(access.missingRequirement(TreePatchDefinition.AVIUM_SAVANNAH_HARDWOOD,
                account(99, 99, Map.of(quest, QuestState.IN_PROGRESS))).isEmpty());
        assertEquals("", access.missingRequirement(TreePatchDefinition.AVIUM_SAVANNAH_HARDWOOD,
                account(99, 99, Map.of(quest, QuestState.FINISHED))));
    }
    @Test public void excludesEarlyGrowingButIncludesEarliestConditionalBoundary()
    {
        TreePatchObservation growing = observation(TreeFamily.TREE, 8, now, FarmingGrowthRate.NORMAL);
        Instant earliest = new TreeGrowthTimer().estimate(growing, now).get().getEarliest();
        Set<TreePatchDefinition> patches = EnumSet.of(TreePatchDefinition.VARROCK_TREE);
        assertTrue(planner.plan(account(99, 99, Map.of()), patches, TreeRunCrop.OAK, true,
                Map.of(TreePatchDefinition.VARROCK_TREE, growing), null, earliest.minusSeconds(1)).getStops().isEmpty());
        TreeRunPlan readyMaybe = planner.plan(account(99, 99, Map.of()), patches, TreeRunCrop.OAK, true,
                Map.of(TreePatchDefinition.VARROCK_TREE, growing), null, earliest);
        assertEquals(1, readyMaybe.getStops().size());
        assertTrue(readyMaybe.getStops().get(0).getAction().contains("Inspect first"));
    }
    @Test public void unknownGrowthRateAndFutureHistoryNeverExcludeAsGrowing()
    {
        assertEquals(6, plan(TreeRunCrop.OAK, Map.of(TreePatchDefinition.VARROCK_TREE,
                observation(TreeFamily.TREE, 8, now, FarmingGrowthRate.UNKNOWN))).getStops().size());
        assertEquals(6, plan(TreeRunCrop.OAK, Map.of(TreePatchDefinition.VARROCK_TREE,
                observation(TreeFamily.TREE, 8, now.plusSeconds(1), FarmingGrowthRate.NORMAL))).getStops().size());
    }
    @Test public void diseaseReservesPruningToolButNoReplacementOrPayment()
    {
        TreeRunPlan result = planner.plan(account(99, 99, Map.of()), EnumSet.of(TreePatchDefinition.VARROCK_TREE),
                TreeRunCrop.OAK, true, Map.of(TreePatchDefinition.VARROCK_TREE,
                        observation(TreeFamily.TREE, 73, now, FarmingGrowthRate.NORMAL)), null, now);
        assertFalse(result.getStops().get(0).isReplant());
        assertTrue(result.getStops().get(0).getAction().contains("prune"));
        assertTrue(result.getSupplies().stream().noneMatch(check -> check.getRequirement().getItemId() == ItemID.PLANTPOT_OAK_SAPLING));
        assertTrue(supply(result, ItemID.SECATEURS).getRequirement().getAcceptedItemIds().contains(ItemID.FAIRY_ENCHANTED_SECATEURS));
    }
    @Test public void fullBasketPaymentDoesNotAcceptLooseProduceOrPartialBasket()
    {
        TreeRunPlan result = planner.plan(account(99, 99, Map.of()), EnumSet.of(TreePatchDefinition.VARROCK_TREE),
                TreeRunCrop.OAK, true, Map.of(), new StorageSnapshot(Map.of(ItemID.BASKET_TOMATO_4, 50L),
                        Map.of(), Map.of(), true, now), now);
        assertEquals(SupplyState.MISSING, supply(result, ItemID.BASKET_TOMATO_5).getState());
        assertEquals(1, supply(result, ItemID.BASKET_TOMATO_5).getMissingQuantity());
    }
    @Test public void notedSaplingsDoNotPlantButNotedPaymentsCount()
    {
        TreeRunPlan result = planner.plan(account(99, 99, Map.of()), EnumSet.of(TreePatchDefinition.VARROCK_TREE),
                TreeRunCrop.MAGIC, true, Map.of(), new StorageSnapshot(Map.of(ItemID.Cert.PLANTPOT_MAGIC_TREE_SAPLING, 1L,
                        ItemID.Cert.COCONUT, 20L, ItemID.COCONUT, 5L), Map.of(), Map.of(), true, now), now);
        assertEquals(SupplyState.MISSING, supply(result, ItemID.PLANTPOT_MAGIC_TREE_SAPLING).getState());
        assertEquals(SupplyState.ALREADY_CARRIED, supply(result, ItemID.COCONUT).getState());
    }
    @Test public void unknownHardwoodPaymentsRemainExplicit()
    {
        for (TreeRunCrop crop : List.of(TreeRunCrop.CAMPHOR, TreeRunCrop.IRONWOOD, TreeRunCrop.ROSEWOOD))
        {
            TreeRunPlan result = planner.plan(account(99, 99, Map.of(Quest.BONE_VOYAGE, QuestState.FINISHED)),
                    EnumSet.of(TreePatchDefinition.FOSSIL_ISLAND_EAST), crop, true, Map.of(), null, now);
            assertEquals(1, result.getStops().size());
            assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("payment is unverified")));
            assertEquals(3, result.getSupplies().size());
        }
    }
    @Test public void protectionDisabledDoesNotReservePayment()
    {
        TreeRunPlan result = planner.plan(account(99, 99, Map.of()), EnumSet.of(TreePatchDefinition.VARROCK_TREE),
                TreeRunCrop.OAK, false, Map.of(), null, now);
        assertEquals(3, result.getSupplies().size());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("protection is disabled")));
    }
    @Test public void healthCheckedFruitDoesNotPromiseSixFruit()
    {
        TreeRunPlan result = plan(TreeRunCrop.APPLE, Map.of(TreePatchDefinition.CATHERBY_FRUIT,
                observation(TreeFamily.FRUIT_TREE, 14, now, FarmingGrowthRate.NORMAL)));
        TreeRunStop catherby = result.getStops().stream().filter(stop -> stop.getPatch() == TreePatchDefinition.CATHERBY_FRUIT).findFirst().get();
        assertTrue(catherby.getAction().contains("any available fruit"));
    }
    @Test public void planCollectionsCannotBeMutated()
    {
        TreeRunPlan result = plan(TreeRunCrop.OAK, Map.of());
        immutable(() -> result.getStops().clear());
        immutable(() -> result.getSupplies().clear());
        immutable(() -> result.getExcluded().clear());
        immutable(() -> result.getWarnings().clear());
    }
    private void immutable(Runnable mutation)
    {
        try { mutation.run(); fail("Expected immutable plan"); }
        catch (UnsupportedOperationException expected) { }
    }
}
