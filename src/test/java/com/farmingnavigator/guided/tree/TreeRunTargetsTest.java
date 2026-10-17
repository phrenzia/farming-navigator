package com.farmingnavigator.guided.tree;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.overlay.GuidedRunOverlayController;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeRunTargetsTest
{
    private TreeRunPlan plan(TreeRunCrop crop)
    {
        return new TreeRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.noneOf(net.runelite.api.Quest.class), false),
                EnumSet.of(TreePatchDefinition.VARROCK_TREE), crop, true, Map.of(),
                new StorageSnapshot(Map.of(ItemID.SPADE, 1L), Map.of(), Map.of(crop.getSaplingId(), 1L, ItemID.Cert.COCONUT, 25L), true, Instant.now()), Instant.now());
    }
    private TreeRunSnapshot snapshot(TreeRunStep step, TreeRunPlan plan)
    {
        return new TreeRunSnapshot(step, plan.getCrop(), plan.getStops().get(0), 1, 0, 0, 0, 0, Duration.ZERO);
    }
    private GuidedRunTargets targets(TreeRunStep step, TreeRunCrop crop)
    {
        TreeRunPlan plan = plan(crop); return GuidedRunTargets.fromTree(snapshot(step, plan), plan, null);
    }
    @Test public void preparationHighlightsPaymentFormsAndOnlyNeededBankSupplies()
    {
        GuidedRunTargets targets = targets(TreeRunStep.PREPARE, TreeRunCrop.MAGIC);
        assertTrue(targets.getInventoryItems().containsAll(Set.of(ItemID.SPADE, ItemID.PLANTPOT_MAGIC_TREE_SAPLING, ItemID.COCONUT, ItemID.Cert.COCONUT)));
        assertFalse(targets.getBankItems().contains(ItemID.SPADE));
        assertTrue(targets.getBankItems().containsAll(Set.of(ItemID.PLANTPOT_MAGIC_TREE_SAPLING, ItemID.COCONUT, ItemID.Cert.COCONUT)));
        assertFalse(targets.getInventoryItems().contains(ItemID.Cert.PLANTPOT_MAGIC_TREE_SAPLING));
    }
    @Test public void plantingPruningAndProtectionHighlightDistinctItems()
    {
        assertEquals(Set.of(ItemID.SPADE, ItemID.PLANTPOT_OAK_SAPLING), targets(TreeRunStep.PLANT, TreeRunCrop.OAK).getInventoryItems());
        assertEquals(Set.of(ItemID.SECATEURS, ItemID.FAIRY_ENCHANTED_SECATEURS), targets(TreeRunStep.PRUNE, TreeRunCrop.OAK).getInventoryItems());
        assertEquals(Set.of(ItemID.BASKET_TOMATO_5, ItemID.Cert.BASKET_TOMATO_5), targets(TreeRunStep.PROTECT, TreeRunCrop.OAK).getInventoryItems());
        assertFalse(targets(TreeRunStep.PROTECT, TreeRunCrop.OAK).getInventoryItems().contains(ItemID.BASKET_TOMATO_4));
        assertTrue(targets(TreeRunStep.CHECK_HEALTH, TreeRunCrop.OAK).getInventoryItems().isEmpty());
        assertTrue(targets(TreeRunStep.WAIT, TreeRunCrop.OAK).getInventoryItems().isEmpty());
    }
    @Test public void travelHighlightsSelectedBankedRuneStaffAndMapArea()
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of()).withRealAttackLevel(99);
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                Map.of(ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1), Map.of(), Map.of());
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.FIRERUNE, 1L, ItemID.LAWRUNE, 1L), Map.of(), Map.of(ItemID.STAFF_OF_AIR, 1L), true, Instant.now());
        RequirementContext context = new RequirementContext(account, new FarmingNavigatorConfig() {}, travel, storage);
        TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.VARROCK_TREE), TreeRunCrop.OAK,
                true, Map.of(), storage, Instant.now()).withTransports(new TransportVerificationService().evaluateAll(account,
                        context.getConfig(), travel, storage, new TreePatchTransportService().getAllOptions()), HerbTransportPreference.CATALOGUE, context);
        GuidedRunTargets targets = GuidedRunTargets.fromTree(snapshot(TreeRunStep.TRAVEL, plan), plan, travel);
        assertTrue(targets.getInventoryItems().contains(ItemID.STAFF_OF_AIR));
        assertEquals(TreePatchDestination.VARROCK_TREE.getLocation(), targets.getDestination());
        assertTrue(targets.getDestinationName().contains("tree farming area"));
        assertTrue(GuidedRunTargets.fromTree(snapshot(TreeRunStep.PREPARE, plan), plan, travel).getBankItems().contains(ItemID.STAFF_OF_AIR));
    }
    @Test public void cancelledAndCompletedTreeRunsClearEveryTarget()
    {
        TreeRunPlan plan = plan(TreeRunCrop.OAK);
        for (TreeRunStep step : List.of(TreeRunStep.COMPLETE, TreeRunStep.INACTIVE))
        {
            GuidedRunTargets targets = GuidedRunTargets.fromTree(snapshot(step, plan), plan, null);
            assertTrue(targets.getInventoryItems().isEmpty()); assertTrue(targets.getBankItems().isEmpty());
            assertNull(targets.getDestination()); assertNull(targets.getScenePatch());
        }
    }

    @Test public void crystalTravelSharesChargedItemTargetsButRemovalHasNoFarmerOrCoins()
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Map.of(net.runelite.api.Quest.SONG_OF_THE_ELVES, net.runelite.api.QuestState.FINISHED), Map.of(), Map.of()).withBoostedWoodcuttingLevel(41);
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_1, 1), Map.of(), Map.of());
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_1, 1L), Map.of(), Map.of(), true, Instant.now());
        RequirementContext context = new RequirementContext(account, new FarmingNavigatorConfig() {}, travel, storage);
        TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.PRIFDDINAS_CRYSTAL), TreeRunCrop.CRYSTAL,
                true, Map.of(), storage, Instant.now()).withTransports(new TransportVerificationService().evaluateAll(account,
                        context.getConfig(), travel, storage, new TreePatchTransportService().getAllOptions()), HerbTransportPreference.CATALOGUE, context);
        assertNotNull(plan.getStops().get(0).getRecommended());
        GuidedRunTargets targets = GuidedRunTargets.fromTree(snapshot(TreeRunStep.TRAVEL, plan), plan, travel);
        assertTrue(targets.getInventoryItems().contains(ItemID.MOURNING_TELEPORT_CRYSTAL_1));
        assertTrue(targets.getInventoryItems().contains(ItemID.PRIF_TELEPORT_CRYSTAL));
        assertEquals(TreePatchDestination.PRIFDDINAS_CRYSTAL.getLocation(), targets.getDestination());
        GuidedRunTargets removal = GuidedRunTargets.fromTree(snapshot(TreeRunStep.REMOVE, plan), plan, travel);
        assertEquals(TreePatchDefinition.PRIFDDINAS_CRYSTAL, removal.getTreeScenePatch());
        assertFalse(removal.isTreeFarmer());
        assertFalse(removal.getInventoryItems().contains(ItemID.COINS));
        assertTrue(removal.getInventoryItems().contains(ItemID.RUNE_AXE));
        assertFalse(removal.getInventoryItems().contains(ItemID.DRAGON_AXE));
        assertFalse(plan.getTransportResources().getChecks().isEmpty());
        assertTrue(plan.getTransportResources().getChecks().stream().allMatch(check -> check.isReadyNow()));
    }

    @Test public void celastrusBarkHighlightsLevelEligibleAxesAndPreparationIncludesBankAlternatives()
    {
        Instant now = Instant.now();
        AccountState account = new AccountState(true, 99, 99, EnumSet.noneOf(net.runelite.api.Quest.class), false)
                .withBoostedWoodcuttingLevel(41);
        TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.FARMING_GUILD_CELASTRUS),
                TreeRunCrop.CELASTRUS, true, Map.of(), new StorageSnapshot(Map.of(), Map.of(), Map.of(ItemID.RUNE_AXE, 1L), true, now), now);
        GuidedRunTargets bark = GuidedRunTargets.fromTree(snapshot(TreeRunStep.HARVEST_BARK, plan), plan, null);
        assertTrue(bark.getInventoryItems().contains(ItemID.RUNE_AXE));
        assertFalse(bark.getInventoryItems().contains(ItemID.DRAGON_AXE));
        assertFalse(bark.isTreeFarmer());
        GuidedRunTargets preparation = GuidedRunTargets.fromTree(snapshot(TreeRunStep.PREPARE, plan), plan, null);
        assertTrue(preparation.getBankItems().contains(ItemID.RUNE_AXE));
        assertFalse(preparation.getBankItems().contains(ItemID.Cert.RUNE_AXE));
    }
    @Test public void verifiedFellingAndGildedToolsReachPreparationAndBarkHighlights()
    {
        for (int level : new int[]{40, 41, 60, 61})
        {
            AccountState account = new AccountState(true, 99, 99, EnumSet.noneOf(net.runelite.api.Quest.class), false)
                    .withBoostedWoodcuttingLevel(level);
            StorageSnapshot storage = new StorageSnapshot(Map.of(), Map.of(),
                    Map.of(ItemID.TRAIL_GILDED_AXE, 1L, ItemID.DRAGON_AXE_2H, 1L), true, Instant.now());
            TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.FARMING_GUILD_CELASTRUS),
                    TreeRunCrop.CELASTRUS, true, Map.of(), storage, Instant.now());
            for (TreeRunStep step : List.of(TreeRunStep.PREPARE, TreeRunStep.HARVEST_BARK, TreeRunStep.REMOVE))
            {
                GuidedRunTargets targets = GuidedRunTargets.fromTree(snapshot(step, plan), plan, null);
                assertEquals(level >= 41, targets.getInventoryItems().contains(ItemID.TRAIL_GILDED_AXE));
                assertEquals(level >= 61, targets.getInventoryItems().contains(ItemID.DRAGON_AXE_2H));
                assertTrue(targets.getInventoryItems().contains(ItemID.BRONZE_AXE_2H));
                assertFalse(targets.getInventoryItems().contains(ItemID.Cert.TRAIL_GILDED_AXE));
                assertFalse(targets.getInventoryItems().contains(ItemID.CRYSTAL_AXE_2H));
                if (step == TreeRunStep.PREPARE)
                {
                    assertEquals(level >= 41, targets.getBankItems().contains(ItemID.TRAIL_GILDED_AXE));
                    assertEquals(level >= 61, targets.getBankItems().contains(ItemID.DRAGON_AXE_2H));
                }
            }
        }
    }
    @Test public void sceneTargetsSeparateFarmerStepsFromPatchSteps()
    {
        GuidedRunTargets protection = targets(TreeRunStep.PROTECT, TreeRunCrop.OAK);
        assertEquals(TreePatchDefinition.VARROCK_TREE, protection.getTreeScenePatch());
        assertTrue(protection.isTreeFarmer());
        GuidedRunTargets planting = targets(TreeRunStep.PLANT, TreeRunCrop.OAK);
        assertEquals(TreePatchDefinition.VARROCK_TREE, planting.getTreeScenePatch());
        assertFalse(planting.isTreeFarmer());
        assertNull(targets(TreeRunStep.PREPARE, TreeRunCrop.OAK).getTreeScenePatch());
        assertNull(targets(TreeRunStep.WAIT, TreeRunCrop.OAK).getTreeScenePatch());
    }
    @Test public void worldMapMarkerStaysStableAndDisablingKeepsForeignMarkers()
    {
        RecordingManager manager = new RecordingManager();
        WorldMapPoint foreign = new WorldMapPoint(TreePatchDestination.CATHERBY_FRUIT.getLocation(), null); manager.add(foreign);
        GuidedRunOverlayController controller = new GuidedRunOverlayController(manager);
        TreeRunPlan plan = plan(TreeRunCrop.OAK);
        controller.updateTree(snapshot(TreeRunStep.TRAVEL, plan), plan, null, true);
        assertEquals(2, manager.points.size()); WorldMapPoint marker = manager.points.get(1);
        assertTrue(marker.getTooltip().contains("tree farming area"));
        controller.updateTree(snapshot(TreeRunStep.TRAVEL, plan), plan, null, true);
        assertEquals(marker, manager.points.get(1));
        controller.updateTree(snapshot(TreeRunStep.TRAVEL, plan), plan, null, false);
        assertEquals(List.of(foreign), manager.points);
        assertTrue(controller.getTargets().getInventoryItems().isEmpty());
    }
    private static final class RecordingManager extends WorldMapPointManager
    {
        private final List<WorldMapPoint> points = new ArrayList<>();
        @Override public void add(WorldMapPoint point) { points.add(point); }
        @Override public void remove(WorldMapPoint point) { points.remove(point); }
    }
}
