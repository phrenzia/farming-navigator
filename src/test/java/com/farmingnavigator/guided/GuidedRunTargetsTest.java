package com.farmingnavigator.guided;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.overlay.GuidedRunOverlayController;
import com.farmingnavigator.overlay.GuidedInventoryOverlay;
import com.farmingnavigator.overlay.GuidedBankOverlay;
import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.run.HerbRunCompost;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbRunPlanner;
import com.farmingnavigator.routing.run.HerbRunStop;
import com.farmingnavigator.routing.run.HerbHouseEntryMode;
import com.farmingnavigator.routing.run.RunTransportResourcePlanner;
import com.farmingnavigator.routing.run.HerbRunCareResources;
import com.farmingnavigator.routing.run.HerbRunCure;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchStateDecoder;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.TransportCategory;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationKind;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.time.Instant;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class GuidedRunTargetsTest
{
    @Test
    public void eachPatchActionHighlightsOnlyItsRelevantItems()
    {
        assertEquals(Collections.singleton(ItemID.RAKE), targets(GuidedRunStep.RAKE).getInventoryItems());
        assertEquals(Collections.singleton(ItemID.SPADE), targets(GuidedRunStep.HARVEST).getInventoryItems());
        assertEquals(Collections.singleton(ItemID.SPADE), targets(GuidedRunStep.CLEAR).getInventoryItems());
        assertEquals(Set.of(ItemID.RANARR_SEED, ItemID.DIBBER), targets(GuidedRunStep.PLANT).getInventoryItems());
        assertEquals(Collections.singleton(ItemID.BUCKET_ULTRACOMPOST), targets(GuidedRunStep.COMPOST).getInventoryItems());
        assertEquals(Collections.singleton(ItemID.PLANT_CURE), targets(GuidedRunStep.CURE).getInventoryItems());
        assertTrue(targets(GuidedRunStep.INSPECT).getInventoryItems().isEmpty());
        assertTrue(targets(GuidedRunStep.WAIT).getInventoryItems().isEmpty());
    }

    @Test
    public void preparationHighlightsKnownBankWithdrawalWithoutUnneededSeedWithdrawal()
    {
        GuidedRunTargets targets = targets(GuidedRunStep.PREPARE);
        assertTrue(targets.getInventoryItems().contains(ItemID.RANARR_SEED));
        assertTrue(targets.getBankItems().contains(ItemID.SPADE));
        assertFalse(targets.getBankItems().contains(ItemID.RANARR_SEED));
        assertNull(targets.getDestination());
    }

    @Test
    public void notingTargetsOnlyUnnotedHerbsAndNearbyLeprechaun()
    {
        GuidedRunTargets targets = targets(GuidedRunStep.NOTE_PRODUCE);
        assertTrue(targets.isLeprechaun());
        assertEquals(HerbPatchDefinition.FALADOR, targets.getScenePatch());
        assertTrue(targets.getInventoryItems().contains(ItemID.UNIDENTIFIED_RANARR));
        assertTrue(targets.getInventoryItems().contains(ItemID.RANARR_WEED));
        assertFalse(targets.getInventoryItems().contains(ItemID.Cert.UNIDENTIFIED_RANARR));
        assertFalse(targets(GuidedRunStep.HARVEST).isLeprechaun());
        assertNull(targets(GuidedRunStep.WAIT).getScenePatch());
    }

    @Test
    public void chargedNecklaceVariantsExcludeUnchargedItem()
    {
        GuidedRunTargets targets = travel(TransportVerificationKind.SKILLS_NECKLACE, VerifiedTransportState.READY_NOW, null);
        assertTrue(targets.getInventoryItems().contains(ItemID.JEWL_NECKLACE_OF_SKILLS_1));
        assertTrue(targets.getInventoryItems().contains(ItemID.JEWL_NECKLACE_OF_SKILLS_6));
        assertFalse(targets.getInventoryItems().contains(ItemID.JEWL_NECKLACE_OF_SKILLS));
        assertTrue(targets.getBankItems().isEmpty());
        assertEquals(HerbPatchDestination.FALADOR.getLocation(), targets.getDestination());
    }

    @Test
    public void bankedTravelItemIsAlsoHighlightedInBank()
    {
        assertTrue(travel(TransportVerificationKind.XERICS_TALISMAN_GLADE, VerifiedTransportState.AVAILABLE_FROM_BANK, null)
                .getBankItems().contains(ItemID.XERIC_TALISMAN));
    }

    @Test
    public void spellHighlightsRelevantCombinationRunesAndUsefulPouch()
    {
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                Collections.singletonMap(ItemID.BH_RUNE_POUCH, 1), Collections.emptyMap(), Collections.singletonMap(ItemID.LAWRUNE, 1));
        GuidedRunTargets targets = travel(TransportVerificationKind.TELEPORT_SPELL, VerifiedTransportState.READY_NOW, travel);
        assertTrue(targets.getInventoryItems().contains(ItemID.MISTRUNE));
        assertTrue(targets.getInventoryItems().contains(ItemID.LAWRUNE));
        assertTrue(targets.getInventoryItems().contains(ItemID.BH_RUNE_POUCH));
        assertFalse(targets.getInventoryItems().contains(ItemID.EARTHRUNE));
        assertFalse(travel(TransportVerificationKind.TELEPORT_SPELL, VerifiedTransportState.READY_NOW, null)
                .getInventoryItems().contains(ItemID.BH_RUNE_POUCH));
    }

    @Test
    public void inactiveAndCompleteSessionsHaveNoTargets()
    {
        for (GuidedRunStep step : new GuidedRunStep[] {GuidedRunStep.INACTIVE, GuidedRunStep.COMPLETE})
        {
            assertTrue(targets(step).getInventoryItems().isEmpty());
            assertTrue(targets(step).getBankItems().isEmpty());
            assertNull(targets(step).getDestination());
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void targetSetsAreImmutable()
    {
        targets(GuidedRunStep.PLANT).getInventoryItems().clear();
    }

    @Test
    public void renderersRespectItemTargetsPlaceholdersAndOverlayToggle()
    {
        GuidedRunOverlayController controller = new GuidedRunOverlayController(new RecordingManager());
        controller.update(snapshot(GuidedRunStep.PLANT, null), plan(), null, true);
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.MAGENTA);
        GuidedInventoryOverlay overlay = new GuidedInventoryOverlay(new FarmingNavigatorConfig() {}, controller);
        WidgetItem seed = new WidgetItem(ItemID.RANARR_SEED, 1, new Rectangle(2, 2, 32, 32), null, null);
        overlay.renderItemOverlay(graphics, ItemID.RANARR_SEED, seed);
        assertNotEquals(0, image.getRGB(2, 2));
        assertEquals(Color.MAGENTA, graphics.getColor());
        graphics.dispose();

        BufferedImage untouched = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D emptyGraphics = untouched.createGraphics();
        overlay.renderItemOverlay(emptyGraphics, ItemID.RANARR_SEED,
                new WidgetItem(ItemID.RANARR_SEED, 0, new Rectangle(2, 2, 32, 32), null, null));
        assertEquals(0, untouched.getRGB(2, 2));
        GuidedInventoryOverlay disabled = new GuidedInventoryOverlay(new FarmingNavigatorConfig() {
            @Override public boolean guidedRunOverlays() { return false; }
        }, controller);
        disabled.renderItemOverlay(emptyGraphics, ItemID.RANARR_SEED, seed);
        assertEquals(0, untouched.getRGB(2, 2));
        controller.update(snapshot(GuidedRunStep.PREPARE, null), plan(), null, true);
        GuidedBankOverlay bank = new GuidedBankOverlay(new FarmingNavigatorConfig() {}, controller);
        bank.renderItemOverlay(emptyGraphics, ItemID.RANARR_SEED, seed);
        assertEquals(0, untouched.getRGB(2, 2));
        bank.renderItemOverlay(emptyGraphics, ItemID.SPADE,
                new WidgetItem(ItemID.SPADE, 1, new Rectangle(2, 2, 32, 32), null, null));
        assertNotEquals(0, untouched.getRGB(2, 2));
        emptyGraphics.dispose();
    }

    @Test
    public void worldMapMarkerIsStableAndCleanupRemovesOnlyThisPluginsMarker()
    {
        RecordingManager manager = new RecordingManager();
        WorldMapPoint foreign = new WorldMapPoint(HerbPatchDestination.CATHERBY.getLocation(), null);
        manager.add(foreign);
        GuidedRunOverlayController controller = new GuidedRunOverlayController(manager);
        GuidedRunSnapshot session = snapshot(GuidedRunStep.TRAVEL, null);
        controller.update(session, plan(), null, true);
        assertEquals(2, manager.points.size());
        controller.update(session, plan(), null, true);
        assertEquals(2, manager.points.size());
        assertFalse(manager.points.get(1).isJumpOnClick());
        assertTrue(manager.points.get(1).getTooltip().contains("farming area"));
        controller.update(session, plan(), null, false);
        assertEquals(Collections.singletonList(foreign), manager.points);
        assertTrue(controller.getTargets().getInventoryItems().isEmpty());
        controller.update(session, plan(), null, true);
        controller.clear();
        assertEquals(Collections.singletonList(foreign), manager.points);
    }

    @Test
    public void houseTravelHighlightsOnlyTheConfiguredEntrySupplies()
    {
        VerifiedTransportResult route = new VerifiedTransportResult(new TransportOption(1, "House", "Enter manually",
                TransportCategory.PLAYER_OWNED_HOUSE, TransportVerificationKind.POH_MOUNTED_XERICS_TALISMAN),
                VerifiedTransportState.READY_NOW, "");
        for (HerbHouseEntryMode mode : HerbHouseEntryMode.values())
        {
            FarmingNavigatorConfig config = new FarmingNavigatorConfig()
            {
                @Override public HerbHouseEntryMode herbHouseEntryMode() { return mode; }
            };
            HerbRunPlan plan = plan().withTransportResources(new RunTransportResourcePlanner().evaluate(
                    Collections.emptyList(), new RequirementContext(null, config)));
            GuidedRunTargets targets = GuidedRunTargets.from(snapshot(GuidedRunStep.TRAVEL, route), plan, null);
            assertEquals(mode == HerbHouseEntryMode.TABLETS, targets.getInventoryItems().contains(ItemID.POH_TABLET_TELEPORTTOHOUSE));
            assertEquals(mode == HerbHouseEntryMode.SPELL, targets.getInventoryItems().contains(ItemID.LAWRUNE));
        }
    }

    @Test
    public void preparationIncludesSharedRuneWithdrawalEvenWhenEachLegIsReady()
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
        java.util.Map<Integer, Integer> carried = java.util.Map.of(ItemID.AIRRUNE, 6, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 1);
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                carried, Collections.emptyMap(), Collections.emptyMap());
        StorageSnapshot storage = new StorageSnapshot(java.util.Map.of(ItemID.AIRRUNE, 6L, ItemID.WATERRUNE, 2L, ItemID.LAWRUNE, 1L),
                Collections.emptyMap(), java.util.Map.of(ItemID.LAWRUNE, 1L), true, Instant.now());
        VerifiedTransportResult route = new VerifiedTransportResult(new TransportOption(1, "Spell", "Cast manually",
                TransportCategory.SPELL, TransportVerificationKind.TELEPORT_SPELL, TeleportSpellDefinition.FALADOR_TELEPORT),
                VerifiedTransportState.READY_NOW, "");
        List<HerbRunStop> stops = java.util.Arrays.asList(
                plan().getStops().get(0).withTransports(route, null), plan().getStops().get(0).withTransports(route, null));
        HerbRunPlan plan = plan().withTransportResources(new RunTransportResourcePlanner().evaluate(stops,
                new RequirementContext(account, null, travel, storage)));
        GuidedRunTargets targets = GuidedRunTargets.from(snapshot(GuidedRunStep.PREPARE, null), plan, travel);
        assertTrue(targets.getBankItems().contains(ItemID.LAWRUNE));
        assertFalse(targets.getBankItems().contains(ItemID.AIRRUNE));
    }

    @Test
    public void selectedCureSpellSurvivesSessionAndHighlightsRunesUntilHealthyObservation()
    {
        Instant now = Instant.parse("2026-10-15T12:00:00Z");
        HerbRunPlan base = plan();
        HerbRunPlan selected = base.withCareResources(HerbRunCareResources.evaluate(base, HerbRunCure.CURE_PLANT, null));
        GuidedRunSession session = new GuidedRunSession();
        assertTrue(session.start(selected, HerbRunCompost.ULTRACOMPOST, now));
        session.confirm(now.plusSeconds(1));
        session.observe(HerbPatchDestination.FALADOR.getLocation(), Collections.singletonMap(HerbPatchDefinition.FALADOR,
                new HerbPatchObservation(new HerbPatchStateDecoder().decode(140), now.plusSeconds(2))), now.plusSeconds(2));
        assertEquals(GuidedRunStep.CURE, session.snapshot().getStep());
        assertEquals(HerbRunCure.CURE_PLANT, session.snapshot().getCure());
        assertTrue(session.snapshot().getInstruction().contains("Cast Cure Plant"));
        GuidedRunTargets targets = GuidedRunTargets.from(session.snapshot(), selected, null);
        assertTrue(targets.getInventoryItems().contains(ItemID.ASTRALRUNE));
        assertTrue(targets.getInventoryItems().contains(ItemID.EARTHRUNE));
        assertTrue(targets.getInventoryItems().contains(ItemID.MUDRUNE));
        assertFalse(targets.getInventoryItems().contains(ItemID.LAWRUNE));
        assertFalse(targets.getInventoryItems().contains(ItemID.PLANT_CURE));
        assertEquals(0, session.snapshot().getCompleted());
        session.observe(HerbPatchDestination.FALADOR.getLocation(), Collections.singletonMap(HerbPatchDefinition.FALADOR,
                new HerbPatchObservation(new HerbPatchStateDecoder().decode(33), now.plusSeconds(3))), now.plusSeconds(3));
        assertEquals(GuidedRunStep.COMPLETE, session.snapshot().getStep());
        assertEquals(1, session.snapshot().getCompleted());
        assertEquals(0, session.snapshot().getManuallyCompleted());
    }

    @Test
    public void bottomlessCompostStepTargetsFilledBucketOnly()
    {
        GuidedRunSnapshot snapshot = new GuidedRunSnapshot(GuidedRunStep.COMPOST, HerbRunCrop.RANARR,
                plan().getStops().get(0), 1, 0, 0, 0, HerbRunCompost.BOTTOMLESS_ULTRACOMPOST);
        GuidedRunTargets targets = GuidedRunTargets.from(snapshot, plan(), null);
        assertEquals(Collections.singleton(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED), targets.getInventoryItems());
        assertFalse(targets.getInventoryItems().contains(ItemID.BOTTOMLESS_COMPOST_BUCKET));
    }

    @Test
    public void fertileSoilStepHighlightsRunesAndAshOnlyInUltraMode()
    {
        for (HerbRunCompost compost : java.util.Arrays.asList(HerbRunCompost.FERTILE_SOIL_SUPER, HerbRunCompost.FERTILE_SOIL_ULTRA))
        {
            GuidedRunSnapshot snapshot = new GuidedRunSnapshot(GuidedRunStep.COMPOST, HerbRunCrop.RANARR,
                    plan().getStops().get(0), 1, 0, 0, 0, compost);
            GuidedRunTargets targets = GuidedRunTargets.from(snapshot, plan(), null);
            assertTrue(snapshot.getInstruction().contains("Cast Fertile Soil"));
            assertTrue(snapshot.getInstruction().contains("not automatically verified"));
            assertTrue(targets.getInventoryItems().contains(ItemID.ASTRALRUNE));
            assertTrue(targets.getInventoryItems().contains(ItemID.NATURERUNE));
            assertTrue(targets.getInventoryItems().contains(ItemID.EARTHRUNE));
            assertFalse(targets.getInventoryItems().contains(ItemID.BUCKET_ULTRACOMPOST));
            assertEquals(compost == HerbRunCompost.FERTILE_SOIL_ULTRA, targets.getInventoryItems().contains(ItemID.FOSSIL_VOLCANIC_ASH));
        }
    }

    @Test
    public void selectedRuneStaffIsHighlightedForPreparationAndRelevantFarmingStepOnly()
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, SpellbookType.LUNAR, null,
                java.util.Map.of(ItemID.ASTRALRUNE, 1), java.util.Map.of(ItemID.STAFF_OF_EARTH, 1), Collections.emptyMap());
        StorageSnapshot storage = new StorageSnapshot(java.util.Map.of(ItemID.ASTRALRUNE, 1L),
                java.util.Map.of(ItemID.STAFF_OF_EARTH, 1L), Collections.emptyMap(), true, Instant.now());
        HerbRunPlan selected = plan().withTransportResources(new RunTransportResourcePlanner().evaluate(Collections.emptyList(),
                new RequirementContext(account, null, travel, storage), HerbRunCareResources.CURE_PLANT_COST));
        GuidedRunSnapshot cure = new GuidedRunSnapshot(GuidedRunStep.CURE, HerbRunCrop.RANARR, selected.getStops().get(0),
                1, 0, 0, 0, HerbRunCompost.ULTRACOMPOST, java.time.Duration.ZERO, HerbRunCure.CURE_PLANT);
        assertTrue(GuidedRunTargets.from(cure, selected, travel).getInventoryItems().contains(ItemID.STAFF_OF_EARTH));
        assertTrue(GuidedRunTargets.from(snapshot(GuidedRunStep.PREPARE, null), selected, travel)
                .getInventoryItems().contains(ItemID.STAFF_OF_EARTH));
        assertFalse(GuidedRunTargets.from(snapshot(GuidedRunStep.PLANT, null), selected, travel)
                .getInventoryItems().contains(ItemID.STAFF_OF_EARTH));
    }

    private GuidedRunTargets targets(GuidedRunStep step)
    {
        return GuidedRunTargets.from(snapshot(step, null), plan(), null);
    }

    private GuidedRunTargets travel(TransportVerificationKind kind, VerifiedTransportState state, TravelCapabilitySnapshot travel)
    {
        TransportOption option = new TransportOption(1, "Travel", "Manual travel", TransportCategory.ITEM, kind,
                kind == TransportVerificationKind.TELEPORT_SPELL ? TeleportSpellDefinition.FALADOR_TELEPORT : null);
        return GuidedRunTargets.from(snapshot(GuidedRunStep.TRAVEL, new VerifiedTransportResult(option, state, "")), plan(), travel);
    }

    private GuidedRunSnapshot snapshot(GuidedRunStep step, VerifiedTransportResult route)
    {
        HerbRunStop stop = plan().getStops().get(0).withTransports(route, null);
        return new GuidedRunSnapshot(step, HerbRunCrop.RANARR, stop, 1, 0, 0, 0, HerbRunCompost.ULTRACOMPOST);
    }

    private HerbRunPlan plan()
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
        StorageSnapshot storage = new StorageSnapshot(Collections.singletonMap(ItemID.RANARR_SEED, 1L),
                Collections.emptyMap(), Collections.singletonMap(ItemID.SPADE, 1L), true, Instant.now());
        return new HerbRunPlanner().plan(account, Collections.singletonList(
                new HerbPatchStatus(HerbPatchDefinition.FALADOR, Collections.emptyList())),
                EnumSet.of(HerbPatchDefinition.FALADOR), HerbRunCrop.RANARR, HerbRunCompost.ULTRACOMPOST,
                Collections.emptyMap(), Collections.emptyMap(), storage, Instant.now());
    }

    private static class RecordingManager extends WorldMapPointManager
    {
        private final List<WorldMapPoint> points = new ArrayList<>();
        @Override public void add(WorldMapPoint point) { points.add(point); }
        @Override public void remove(WorldMapPoint point) { points.remove(point); }
    }
}
