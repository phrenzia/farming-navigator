package com.farmingnavigator.guided.garden;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
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

public class GardenRunTargetsTest
{
    private final AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of()).withRealAttackLevel(99);
    private GardenRunPlan plan(GardenRunCrop crop)
    {
        GardenPatchDefinition patch = crop == GardenRunCrop.CACTUS ? GardenPatchDefinition.FARMING_GUILD_CACTUS : GardenPatchDefinition.RIMMINGTON_BUSH;
        return new GardenRunPlanner().plan(account, EnumSet.of(patch), crop, GardenRunMode.KEEP_MATURE, true, Map.of(),
                new StorageSnapshot(Map.of(ItemID.SPADE, 1L), Map.of(), Map.of(crop.getSeedId(), 1L, ItemID.Cert.SACK_CABBAGE_10, 4L), true, Instant.now()), Instant.now());
    }
    private GardenRunSnapshot snapshot(GardenRunStep step, GardenRunPlan plan)
    {
        return new GardenRunSnapshot(step, plan.getCrop(), plan.getMode(), plan.getStops().get(0), 1, 0, 0, 0, 0, Duration.ZERO);
    }
    private GuidedRunTargets targets(GardenRunStep step, GardenRunCrop crop)
    {
        GardenRunPlan plan = plan(crop); return GuidedRunTargets.fromGarden(snapshot(step, plan), plan, null);
    }
    @Test public void preparationHighlightsSeedsAndPaymentFormsWithoutWithdrawingCarriedSpade()
    {
        GuidedRunTargets targets = targets(GardenRunStep.PREPARE, GardenRunCrop.REDBERRIES);
        assertTrue(targets.getInventoryItems().containsAll(Set.of(ItemID.REDBERRY_BUSH_SEED, ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10)));
        assertTrue(targets.getBankItems().containsAll(Set.of(ItemID.REDBERRY_BUSH_SEED, ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10)));
        assertFalse(targets.getBankItems().contains(ItemID.SPADE));
    }
    @Test public void plantingPruningCureAndPaymentHighlightSeparateItems()
    {
        assertEquals(Set.of(ItemID.DIBBER, ItemID.REDBERRY_BUSH_SEED), targets(GardenRunStep.PLANT, GardenRunCrop.REDBERRIES).getInventoryItems());
        assertEquals(Set.of(ItemID.SECATEURS, ItemID.FAIRY_ENCHANTED_SECATEURS), targets(GardenRunStep.PRUNE, GardenRunCrop.REDBERRIES).getInventoryItems());
        assertEquals(Set.of(ItemID.PLANT_CURE), targets(GardenRunStep.CURE, GardenRunCrop.CACTUS).getInventoryItems());
        assertEquals(Set.of(ItemID.SACK_CABBAGE_10, ItemID.Cert.SACK_CABBAGE_10), targets(GardenRunStep.PROTECT, GardenRunCrop.REDBERRIES).getInventoryItems());
        assertTrue(targets(GardenRunStep.HARVEST, GardenRunCrop.REDBERRIES).getInventoryItems().isEmpty());
        assertTrue(targets(GardenRunStep.WAIT, GardenRunCrop.REDBERRIES).getInventoryItems().isEmpty());
    }
    @Test public void travelUsesSharedBankedRuneSourceAndFarmingAreaAnchor()
    {
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null, Map.of(ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1), Map.of(), Map.of());
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.WATERRUNE, 1L, ItemID.LAWRUNE, 1L), Map.of(), Map.of(ItemID.STAFF_OF_AIR, 1L), true, Instant.now());
        RequirementContext context = new RequirementContext(account, new FarmingNavigatorConfig() {}, travel, storage);
        GardenRunPlan plan = new GardenRunPlanner().plan(account, EnumSet.of(GardenPatchDefinition.RIMMINGTON_BUSH), GardenRunCrop.REDBERRIES,
                GardenRunMode.KEEP_MATURE, true, Map.of(), storage, Instant.now()).withTransports(new TransportVerificationService().evaluateAll(account,
                        context.getConfig(), travel, storage, new GardenPatchTransportService().getAllOptions()), HerbTransportPreference.CATALOGUE, context);
        GuidedRunTargets targets = GuidedRunTargets.fromGarden(snapshot(GardenRunStep.TRAVEL, plan), plan, travel);
        assertTrue(targets.getInventoryItems().contains(ItemID.STAFF_OF_AIR));
        assertEquals(GardenPatchDefinition.RIMMINGTON_BUSH.getLocation(), targets.getDestination());
        assertTrue(GuidedRunTargets.fromGarden(snapshot(GardenRunStep.PREPARE, plan), plan, travel).getBankItems().contains(ItemID.STAFF_OF_AIR));
        assertFalse(GuidedRunTargets.fromGarden(snapshot(GardenRunStep.PLANT, plan), plan, travel).getInventoryItems().contains(ItemID.STAFF_OF_AIR));
    }
    @Test public void disabledCompletedAndCancelledRunsClearTheirOwnMapAndItemTargets()
    {
        RecordingManager manager = new RecordingManager(); GardenRunPlan plan = plan(GardenRunCrop.REDBERRIES);
        WorldMapPoint foreign = new WorldMapPoint(GardenPatchDefinition.AL_KHARID_CACTUS.getLocation(), null); manager.add(foreign);
        GuidedRunOverlayController controller = new GuidedRunOverlayController(manager);
        controller.updateGarden(snapshot(GardenRunStep.TRAVEL, plan), plan, null, true); assertEquals(2, manager.points.size());
        WorldMapPoint marker = manager.points.get(1);
        controller.updateGarden(snapshot(GardenRunStep.TRAVEL, plan), plan, null, true); assertSame(marker, manager.points.get(1));
        controller.updateGarden(snapshot(GardenRunStep.TRAVEL, plan), plan, null, false); assertEquals(List.of(foreign), manager.points);
        for (GardenRunStep step : List.of(GardenRunStep.COMPLETE, GardenRunStep.INACTIVE))
        {
            GuidedRunTargets targets = GuidedRunTargets.fromGarden(snapshot(step, plan), plan, null);
            assertTrue(targets.getInventoryItems().isEmpty()); assertTrue(targets.getBankItems().isEmpty()); assertNull(targets.getDestination());
        }
    }
    private static final class RecordingManager extends WorldMapPointManager
    {
        private final List<WorldMapPoint> points = new ArrayList<>();
        @Override public void add(WorldMapPoint point) { points.add(point); }
        @Override public void remove(WorldMapPoint point) { points.remove(point); }
    }
}
