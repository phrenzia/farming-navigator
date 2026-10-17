package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.item.ConstructionCapeItems;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConstructionCapeTransportTest
{
    private final Instant now = Instant.parse("2026-10-17T18:00:00Z");
    private final List<TransportVerificationKind> kinds = List.of(TransportVerificationKind.CONSTRUCTION_CAPE_RIMMINGTON,
            TransportVerificationKind.CONSTRUCTION_CAPE_TAVERLEY, TransportVerificationKind.CONSTRUCTION_CAPE_BRIMHAVEN,
            TransportVerificationKind.CONSTRUCTION_CAPE_YANILLE, TransportVerificationKind.CONSTRUCTION_CAPE_HOSIDIUS,
            TransportVerificationKind.CONSTRUCTION_CAPE_PRIFDDINAS);
    private RequirementContext context(int construction, boolean elves, Map<Integer, Integer> inventory, Map<Integer, Integer> equipment, Map<Integer, Long> bank, boolean known)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 1, 99, 99, 99, 99,
                elves ? Map.of(Quest.SONG_OF_THE_ELVES, QuestState.FINISHED) : Map.of(), Map.of(), Map.of())
                .withRealConstructionLevel(construction);
        return new RequirementContext(account, new FarmingNavigatorConfig(){
            @Override public PohHouseLocation pohHouseLocation() { return PohHouseLocation.NONE; }
            @Override public HerbHouseEntryMode herbHouseEntryMode() { return HerbHouseEntryMode.SPELL; }
        }, new TravelCapabilitySnapshot(true, SpellbookType.LUNAR, null, inventory, equipment, Map.of()),
                new StorageSnapshot(inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())),
                        equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())), bank, known, now));
    }
    private TransportOption option(TransportVerificationKind kind)
    {
        return Stream.of(new HerbPatchTransportService().getAllOptions().values(), new TreePatchTransportService().getAllOptions().values(),
                new GardenPatchTransportService().getAllOptions().values(), new FieldPatchTransportService().getAllOptions().values())
                .flatMap(java.util.Collection::stream).flatMap(List::stream).filter(option -> option.getVerificationKind() == kind).findFirst().get();
    }
    private VerifiedTransportResult verify(RequirementContext context, TransportVerificationKind kind)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                context.getStorageSnapshot(), Map.of(1, List.of(option(kind)))).get(1).get(0);
    }
    private GardenRunPlan garden(RequirementContext context)
    {
        return new GardenRunPlanner().plan(context.getAccountState(), Set.of(GardenPatchDefinition.RIMMINGTON_BUSH), GardenRunCrop.REDBERRIES,
                GardenRunMode.KEEP_MATURE, false, Map.of(), context.getStorageSnapshot(), now)
                .withTransports(new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                        context.getStorageSnapshot(), new GardenPatchTransportService().getAllOptions()), HerbTransportPreference.SHARED_BUDGET, context);
    }

    @Test public void allFourCapeFormsWorkInEveryPortalFromInventoryEquipmentOrVerifiedBank()
    {
        assertEquals(Set.of(ItemID.SKILLCAPE_CONSTRUCTION, ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED, ItemID.SKILLCAPE_MAX, ItemID.SKILLCAPE_MAX_WORN), ConstructionCapeItems.getItemIds());
        for (TransportVerificationKind kind : kinds)
        {
            assertEquals(TransportCategory.ITEM, option(kind).getCategory()); assertEquals(96, option(kind).getBaselineRank());
            for (int id : ConstructionCapeItems.getItemIds())
            {
                assertEquals(VerifiedTransportState.READY_NOW, verify(context(99, true, Map.of(id, 1), Map.of(), Map.of(), true), kind).getState());
                assertEquals(VerifiedTransportState.READY_NOW, verify(context(99, true, Map.of(), Map.of(id, 1), Map.of(), true), kind).getState());
                assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, verify(context(99, true, Map.of(), Map.of(), Map.of(id, 1L), true), kind).getState());
            }
        }
        try { ConstructionCapeItems.getItemIds().clear(); fail("Mutable item set"); } catch (UnsupportedOperationException expected) { }
    }

    @Test public void unknownOrInsufficientBaseConstructionAndPrifddinasQuestCannotBecomeReady()
    {
        for (TransportVerificationKind kind : kinds)
        {
            for (int invalid : new int[]{-1, 0, 100})
            { assertEquals(VerifiedTransportState.DATA_REQUIRED, verify(context(invalid, true, Map.of(ItemID.SKILLCAPE_MAX, 1), Map.of(), Map.of(), true), kind).getState()); }
            assertEquals(VerifiedTransportState.LOCKED, verify(context(98, true, Map.of(ItemID.SKILLCAPE_CONSTRUCTION, 1), Map.of(), Map.of(), true), kind).getState());
        }
        assertEquals(VerifiedTransportState.LOCKED, verify(context(99, false, Map.of(ItemID.SKILLCAPE_CONSTRUCTION, 1), Map.of(), Map.of(), true),
                TransportVerificationKind.CONSTRUCTION_CAPE_PRIFDDINAS).getState());
        assertEquals(VerifiedTransportState.READY_NOW, verify(context(99, false, Map.of(ItemID.SKILLCAPE_CONSTRUCTION, 1), Map.of(), Map.of(), true),
                TransportVerificationKind.CONSTRUCTION_CAPE_HOSIDIUS).getState());
    }

    @Test public void hoodsCosmeticMaxVariantsMissingItemsAndUnknownBankRemainExcluded()
    {
        for (int unsupported : new int[]{ItemID.SKILLCAPE_CONSTRUCTION_HOOD, ItemID.DUMMY_SKILLCAPE_CONSTRUCTION, ItemID.SKILLCAPE_MAX_ARDY, ItemID.SKILLCAPE_FARMING})
        {
            assertEquals(VerifiedTransportState.MISSING, verify(context(99, true, Map.of(unsupported, 1), Map.of(), Map.of(), true), kinds.get(0)).getState());
        }
        assertEquals(VerifiedTransportState.MISSING, verify(context(99, true, Map.of(), Map.of(), Map.of(), true), kinds.get(0)).getState());
        RequirementContext unknown = context(99, true, Map.of(ItemID.SKILLCAPE_CONSTRUCTION, 1), Map.of(), Map.of(), false);
        assertTrue(new TransportVerificationService().evaluateAll(unknown.getAccountState(), unknown.getConfig(), unknown.getTravelCapabilitySnapshot(),
                unknown.getStorageSnapshot(), Map.of(1, List.of(option(kinds.get(0))))).isEmpty());
    }

    @Test public void repeatedDirectPortalTripsNeedNoHouseConfigurationEntryItemsRunesOrCharges()
    {
        RequirementContext context = context(99, true, Map.of(ItemID.SKILLCAPE_CONSTRUCTION, 1), Map.of(), Map.of(), true);
        List<RunTravelStop> stops = java.util.stream.IntStream.range(0, 100).mapToObj(index -> {
            VerifiedTransportResult arrival = verify(context, kinds.get(index % kinds.size()));
            return new RunTravelStop() {
                public VerifiedTransportResult getRecommended() { return arrival; }
                public String getTravelLabel() { return "Direct portal test"; }
            };
        }).collect(Collectors.toList());
        SharedTransportRepair.Result<RunTravelStop> repaired = new SharedTransportRepair().repair(stops,
                stop -> List.of(stop.getRecommended()), (stop, arrival) -> stop, context, RuneCost.builder().build(), true);
        assertEquals(0, repaired.getAttempts()); assertTrue(repaired.getResources().isKnownPoolsRequired());
        assertTrue(repaired.getResources().getConsumables().isEmpty()); assertTrue(repaired.getResources().getChecks().isEmpty());
        assertFalse(repaired.getResources().getNotes().stream().anyMatch(note -> note.contains("POH entry budget")));
    }

    @Test public void oneCapeCoversAllFourComponentArrivalsWithoutCreatingHouseEntryCosts()
    {
        RequirementContext context = context(99, true, Map.of(ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED, 1), Map.of(), Map.of(), true);
        TransportVerificationService verifier = new TransportVerificationService();
        Map<HerbPatchDefinition, List<VerifiedTransportResult>> herbRoutes = verifier.evaluateAll(context.getAccountState(), context.getConfig(),
                context.getTravelCapabilitySnapshot(), context.getStorageSnapshot(), new HerbPatchTransportService().getAllOptions());
        HerbRunPlan herb = new HerbRunPlanner().plan(context.getAccountState(), List.of(new HerbPatchStatus(HerbPatchDefinition.HOSIDIUS, List.of())),
                Set.of(HerbPatchDefinition.HOSIDIUS), HerbRunCrop.RANARR, HerbRunCompost.NONE, Map.of(), herbRoutes, context.getStorageSnapshot(), now);
        herb = herb.withCareResources(HerbRunCareResources.evaluate(herb, HerbRunCure.PLANT_CURE, HerbRunCompost.NONE, context))
                .withTransports(herbRoutes, HerbTransportPreference.REUSABLE_FIRST, context);
        TreeRunPlan tree = new TreeRunPlanner().plan(context.getAccountState(), Set.of(TreePatchDefinition.TAVERLEY_TREE), TreeRunCrop.OAK,
                false, Map.of(), context.getStorageSnapshot(), now).withTransports(verifier.evaluateAll(context.getAccountState(), context.getConfig(),
                        context.getTravelCapabilitySnapshot(), context.getStorageSnapshot(), new TreePatchTransportService().getAllOptions()), HerbTransportPreference.REUSABLE_FIRST, context);
        FieldRunPlan field = new FieldRunPlanner().plan(context.getAccountState(), Set.of(FieldPatchDefinition.YANILLE_HOPS), FieldRunCrop.JUTE,
                FieldRunMode.HARVEST_ALL, false, Map.of(), context.getStorageSnapshot(), now).withTransports(verifier.evaluateAll(context.getAccountState(), context.getConfig(),
                        context.getTravelCapabilitySnapshot(), context.getStorageSnapshot(), new FieldPatchTransportService().getAllOptions()), HerbTransportPreference.REUSABLE_FIRST, context);
        CombinedRunPlan combined = new CombinedRunPlanner().plan(java.util.EnumSet.allOf(CombinedRunKind.class), herb, tree, garden(context), field, context);
        assertTrue(combined.getBlockers().isEmpty()); assertEquals(4, combined.getStops().size());
        assertTrue(combined.getStops().stream().allMatch(stop -> stop.getRecommended() != null && stop.getRecommended().getOption().getCategory() == TransportCategory.ITEM));
        assertTrue(combined.getResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        assertTrue(combined.getResources().getConsumables().isEmpty());
    }

    @Test public void bankedCapeHighlightsAndSharedAreaRewritesRetainDirectPortalSemantics()
    {
        RequirementContext banked = context(99, true, Map.of(), Map.of(), Map.of(ItemID.SKILLCAPE_CONSTRUCTION, 1L), true);
        GardenRunPlan plan = garden(banked); GardenRunSession session = new GardenRunSession(); assertTrue(session.start(plan, false, now));
        assertTrue(GuidedRunTargets.fromGarden(session.snapshot(now), plan, banked.getTravelCapabilitySnapshot()).getBankItems().contains(ItemID.SKILLCAPE_CONSTRUCTION));
        session.command(GardenRunCommand.CONFIRM, now.plusSeconds(1));
        GuidedRunTargets targets = GuidedRunTargets.fromGarden(session.snapshot(now), plan, banked.getTravelCapabilitySnapshot());
        assertTrue(targets.getInventoryItems().containsAll(ConstructionCapeItems.getItemIds()));
        assertFalse(targets.getInventoryItems().contains(ItemID.POH_TABLET_TELEPORTTOHOUSE));
        assertFalse(targets.getInventoryItems().contains(ItemID.LAWRUNE));
        TransportOption inherited = new FieldPatchTransportService().getAllOptions().get(FieldPatchDefinition.HOSIDIUS_NORTH).stream()
                .filter(value -> value.getVerificationKind() == TransportVerificationKind.CONSTRUCTION_CAPE_HOSIDIUS).findFirst().get();
        assertEquals(TransportCategory.ITEM, inherited.getCategory());
        assertTrue(inherited.getInstructions().contains(FieldPatchDefinition.HOSIDIUS_NORTH.getLabel()));
        assertTrue(inherited.getInstructions().contains("POH Portals"));
    }

    @Test public void reusablePreferenceSelectsTheCapeWhileCatalogueKeepsTheEarlierHouseArrival()
    {
        RequirementContext base = context(99, true, Map.of(ItemID.SKILLCAPE_CONSTRUCTION, 1, ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of(), Map.of(), true);
        FarmingNavigatorConfig config = new FarmingNavigatorConfig() {
            @Override public PohHouseLocation pohHouseLocation() { return PohHouseLocation.TAVERLEY; }
        };
        RequirementContext context = new RequirementContext(base.getAccountState(), config, base.getTravelCapabilitySnapshot(), base.getStorageSnapshot());
        Map<TreePatchDefinition, List<VerifiedTransportResult>> routes = new TransportVerificationService().evaluateAll(context.getAccountState(), config,
                context.getTravelCapabilitySnapshot(), context.getStorageSnapshot(), new TreePatchTransportService().getAllOptions());
        TreeRunPlan planned = new TreeRunPlanner().plan(context.getAccountState(), Set.of(TreePatchDefinition.TAVERLEY_TREE), TreeRunCrop.OAK,
                false, Map.of(), context.getStorageSnapshot(), now);
        assertEquals(TransportVerificationKind.POH_TAVERLEY_HOUSE_LOCATION, planned.withTransports(routes, HerbTransportPreference.CATALOGUE, context)
                .getStops().get(0).getRecommended().getOption().getVerificationKind());
        TreeRunPlan reusable = planned.withTransports(routes, HerbTransportPreference.REUSABLE_FIRST, context);
        assertEquals(TransportVerificationKind.CONSTRUCTION_CAPE_TAVERLEY, reusable.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(reusable.getTransportResources().getConsumables().isEmpty());
    }
}
