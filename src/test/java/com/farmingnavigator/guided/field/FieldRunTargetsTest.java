package com.farmingnavigator.guided.field;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
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

public class FieldRunTargetsTest
{
    private final AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of()).withRealAttackLevel(99);
    private FieldRunPlan plan(FieldRunCrop crop)
    {
        FieldPatchDefinition patch = crop == FieldRunCrop.MARIGOLD ? FieldPatchDefinition.FALADOR_FLOWER : FieldPatchDefinition.FALADOR_NORTH;
        return new FieldRunPlanner().plan(account, EnumSet.of(patch), crop, FieldRunMode.KEEP_MATURE_FLOWERS, true, Map.of(),
                new StorageSnapshot(Map.of(ItemID.SPADE, 1L), Map.of(), Map.of(crop.getSeedId(), 1L, ItemID.Cert.SACK_CABBAGE_10, 4L), true, Instant.now()), Instant.now());
    }
    private FieldRunSnapshot snapshot(FieldRunStep step, FieldRunPlan plan)
    {
        return new FieldRunSnapshot(step, plan.getCrop(), plan.getMode(), plan.getStops().get(0), 1, 0, 0, 0, 0, Duration.ZERO);
    }
    private GuidedRunTargets targets(FieldRunStep step, FieldRunCrop crop)
    {
        FieldRunPlan plan = plan(crop); return GuidedRunTargets.fromField(snapshot(step, plan), plan, null);
    }
    @Test public void preparationHighlightsSeedsAndPaymentFormsWithoutWithdrawingCarriedSpade()
    {
        GuidedRunTargets targets = targets(FieldRunStep.PREPARE, FieldRunCrop.TOMATO);
        assertTrue(targets.getInventoryItems().containsAll(Set.of(ItemID.TOMATO_SEED, ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10)));
        assertTrue(targets.getBankItems().containsAll(Set.of(ItemID.TOMATO_SEED, ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10)));
        assertFalse(targets.getBankItems().contains(ItemID.SPADE));
    }
    @Test public void plantingHarvestCureAndPaymentHighlightSeparateItems()
    {
        assertEquals(Set.of(ItemID.DIBBER, ItemID.TOMATO_SEED), targets(FieldRunStep.PLANT, FieldRunCrop.TOMATO).getInventoryItems());
        assertEquals(Set.of(ItemID.SPADE), targets(FieldRunStep.HARVEST, FieldRunCrop.TOMATO).getInventoryItems());
        assertEquals(Set.of(ItemID.PLANT_CURE), targets(FieldRunStep.CURE, FieldRunCrop.MARIGOLD).getInventoryItems());
        assertEquals(Set.of(ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10), targets(FieldRunStep.PROTECT, FieldRunCrop.TOMATO).getInventoryItems());
        assertTrue(targets(FieldRunStep.HARVEST, FieldRunCrop.MARIGOLD).getInventoryItems().isEmpty());
        assertTrue(targets(FieldRunStep.WAIT, FieldRunCrop.TOMATO).getInventoryItems().isEmpty());
    }
    @Test public void travelUsesSharedBankedRuneSourceAndFarmingAreaAnchor()
    {
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null, Map.of(ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1), Map.of(), Map.of());
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.WATERRUNE, 1L, ItemID.LAWRUNE, 1L), Map.of(), Map.of(ItemID.STAFF_OF_AIR, 1L), true, Instant.now());
        RequirementContext context = new RequirementContext(account, new FarmingNavigatorConfig() {}, travel, storage);
        FieldRunPlan plan = new FieldRunPlanner().plan(account, EnumSet.of(FieldPatchDefinition.FALADOR_NORTH), FieldRunCrop.TOMATO,
                FieldRunMode.KEEP_MATURE_FLOWERS, true, Map.of(), storage, Instant.now()).withTransports(new TransportVerificationService().evaluateAll(account,
                        context.getConfig(), travel, storage, new FieldPatchTransportService().getAllOptions()), HerbTransportPreference.CATALOGUE, context);
        GuidedRunTargets targets = GuidedRunTargets.fromField(snapshot(FieldRunStep.TRAVEL, plan), plan, travel);
        assertTrue(targets.getInventoryItems().contains(ItemID.STAFF_OF_AIR));
        assertEquals(FieldPatchDefinition.FALADOR_NORTH.getLocation(), targets.getDestination());
        assertTrue(GuidedRunTargets.fromField(snapshot(FieldRunStep.PREPARE, plan), plan, travel).getBankItems().contains(ItemID.STAFF_OF_AIR));
        assertFalse(GuidedRunTargets.fromField(snapshot(FieldRunStep.PLANT, plan), plan, travel).getInventoryItems().contains(ItemID.STAFF_OF_AIR));
    }
    @Test public void disabledCompletedAndCancelledRunsClearTheirOwnMapAndItemTargets()
    {
        RecordingManager manager = new RecordingManager(); FieldRunPlan plan = plan(FieldRunCrop.TOMATO);
        WorldMapPoint foreign = new WorldMapPoint(FieldPatchDefinition.FALADOR_FLOWER.getLocation(), null); manager.add(foreign);
        GuidedRunOverlayController controller = new GuidedRunOverlayController(manager);
        controller.updateField(snapshot(FieldRunStep.TRAVEL, plan), plan, null, true); assertEquals(2, manager.points.size());
        WorldMapPoint marker = manager.points.get(1);
        controller.updateField(snapshot(FieldRunStep.TRAVEL, plan), plan, null, true); assertSame(marker, manager.points.get(1));
        controller.updateField(snapshot(FieldRunStep.TRAVEL, plan), plan, null, false); assertEquals(List.of(foreign), manager.points);
        for (FieldRunStep step : List.of(FieldRunStep.COMPLETE, FieldRunStep.INACTIVE))
        {
            GuidedRunTargets targets = GuidedRunTargets.fromField(snapshot(step, plan), plan, null);
            assertTrue(targets.getInventoryItems().isEmpty()); assertTrue(targets.getBankItems().isEmpty()); assertNull(targets.getDestination());
            assertNull(targets.getGardenScenePatch()); assertNull(targets.getTreeScenePatch());
        }
    }
    @Test public void keptFlowersAndScarecrowsHaveNoActionItemHighlights()
    {
        assertTrue(targets(FieldRunStep.KEEP_FLOWER, FieldRunCrop.MARIGOLD).getInventoryItems().isEmpty());
        assertTrue(targets(FieldRunStep.SCARECROW, FieldRunCrop.MARIGOLD).getInventoryItems().isEmpty());
    }
    @Test public void sameAreaMarkerKeepsItsIdentityButUpdatesPlotLabel()
    {
        RecordingManager manager = new RecordingManager(); GuidedRunOverlayController controller = new GuidedRunOverlayController(manager);
        FieldRunPlan plan = new FieldRunPlanner().plan(account, EnumSet.of(FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_SOUTH),
                FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, true, Map.of(), null, Instant.now());
        controller.updateField(snapshot(FieldRunStep.TRAVEL, plan), plan, null, true);
        WorldMapPoint marker = manager.points.get(0); assertTrue(marker.getTooltip().contains("North west"));
        FieldRunSnapshot south = new FieldRunSnapshot(FieldRunStep.TRAVEL, plan.getCrop(), plan.getMode(), plan.getStops().get(1), 2, 1, 0, 0, 0, Duration.ZERO);
        controller.updateField(south, plan, null, true);
        assertSame(marker, manager.points.get(0)); assertEquals(1, manager.points.size()); assertTrue(marker.getTooltip().contains("South east"));
    }
    @Test public void houseTravelUsesConfiguredTabletOrSpellEntrySupplies()
    {
        VerifiedTransportResult route = new VerifiedTransportResult(new TransportOption(1, "House", "Enter manually",
                TransportCategory.PLAYER_OWNED_HOUSE, TransportVerificationKind.POH_MOUNTED_XERICS_TALISMAN), VerifiedTransportState.READY_NOW, "");
        FieldPatchDefinition patch = FieldPatchDefinition.FALADOR_NORTH;
        for (HerbHouseEntryMode mode : HerbHouseEntryMode.values())
        {
            FarmingNavigatorConfig config = new FarmingNavigatorConfig() { @Override public HerbHouseEntryMode herbHouseEntryMode() { return mode; } };
            FieldRunPlan plan = plan(FieldRunCrop.TOMATO).withTransports(Map.of(patch, List.of(route)), HerbTransportPreference.CATALOGUE, new RequirementContext(account, config));
            GuidedRunTargets targets = GuidedRunTargets.fromField(snapshot(FieldRunStep.TRAVEL, plan), plan, null);
            assertEquals(mode == HerbHouseEntryMode.TABLETS, targets.getInventoryItems().contains(ItemID.POH_TABLET_TELEPORTTOHOUSE));
            assertEquals(mode == HerbHouseEntryMode.SPELL, targets.getInventoryItems().contains(ItemID.LAWRUNE));
        }
    }
    @Test public void localWalkShowsDestinationWithoutAdditionalTravelItem()
    {
        FieldRunPlan plan = plan(FieldRunCrop.TOMATO);
        FieldRunStop local = plan.getStops().get(0).withLocalWalk();
        FieldRunSnapshot snapshot = new FieldRunSnapshot(FieldRunStep.TRAVEL, plan.getCrop(), plan.getMode(), local, 1, 0, 0, 0, 0, Duration.ZERO);
        GuidedRunTargets targets = GuidedRunTargets.fromField(snapshot, plan, null);
        assertNotNull(targets.getDestination()); assertTrue(targets.getInventoryItems().isEmpty()); assertTrue(targets.getBankItems().isEmpty());
    }
    @Test public void sceneTargetsAreExclusiveAndSuppressWaitingAndScarecrowActions()
    {
        GuidedRunTargets protect = targets(FieldRunStep.PROTECT, FieldRunCrop.TOMATO);
        assertEquals(FieldPatchDefinition.FALADOR_NORTH, protect.getFieldScenePatch()); assertTrue(protect.isFieldFarmer());
        assertNull(protect.getGardenScenePatch()); assertNull(protect.getTreeScenePatch()); assertNull(protect.getScenePatch());
        GuidedRunTargets harvest = targets(FieldRunStep.HARVEST, FieldRunCrop.TOMATO);
        assertEquals(FieldPatchDefinition.FALADOR_NORTH, harvest.getFieldScenePatch()); assertFalse(harvest.isFieldFarmer());
        for (FieldRunStep step : List.of(FieldRunStep.PREPARE, FieldRunStep.WAIT, FieldRunStep.SCARECROW))
        {
            assertNull(targets(step, FieldRunCrop.MARIGOLD).getFieldScenePatch());
        }
    }
    private static final class RecordingManager extends WorldMapPointManager
    {
        private final List<WorldMapPoint> points = new ArrayList<>();
        @Override public void add(WorldMapPoint point) { points.add(point); }
        @Override public void remove(WorldMapPoint point) { points.remove(point); }
    }
}
