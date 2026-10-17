package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.state.*;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.guided.*;
import com.farmingnavigator.loadout.SupplyCheck;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.farmingnavigator.routing.run.RunVisitSelection.*;

public class RunVisitSelectionTest
{
    private final Instant now = Instant.parse("2026-10-17T17:00:00Z");
    private final Instant historical = now.minus(Duration.ofDays(7));
    private AccountState account() { return new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false); }
    private HerbPatchObservation herb(PatchCondition condition, Instant at)
    {
        HerbPatchState state = IntStream.range(0, 256).mapToObj(raw -> new HerbPatchStateDecoder().decode(raw))
                .filter(value -> value.getCondition() == condition).findFirst().get();
        return new HerbPatchObservation(state, at, FarmingGrowthRate.NORMAL);
    }
    private TreePatchObservation tree(TreeFamily family, TreePatchCondition condition, Instant at)
    {
        TreePatchState state = IntStream.range(0, 256).mapToObj(raw -> new TreePatchStateDecoder().decode(family, raw))
                .filter(value -> value.getCondition() == condition).findFirst().get();
        return new TreePatchObservation(state, at, FarmingGrowthRate.NORMAL);
    }
    private GardenPatchObservation garden(GardenFamily family, GardenPatchCondition condition, Instant at)
    {
        GardenPatchState state = IntStream.range(0, 256).mapToObj(raw -> new GardenPatchStateDecoder().decode(family, raw))
                .filter(value -> value.getCondition() == condition).findFirst().get();
        return new GardenPatchObservation(state, at, FarmingGrowthRate.NORMAL);
    }
    private FieldPatchObservation field(FieldFamily family, FieldPatchCondition condition, Instant at)
    {
        FieldPatchState state = IntStream.range(0, 256).mapToObj(raw -> new FieldPatchStateDecoder().decode(family, raw))
                .filter(value -> value.getCondition() == condition).findFirst().get();
        return new FieldPatchObservation(state, at, FarmingGrowthRate.NORMAL);
    }
    private long quantity(List<SupplyCheck> supplies, int item)
    { return supplies.stream().filter(check -> check.getRequirement().getItemId() == item).mapToLong(check -> check.getRequirement().getQuantity()).sum(); }
    private HerbRunPlan herbs(Map<HerbPatchDefinition, HerbPatchObservation> observations, RunVisitSelection selection)
    {
        Set<HerbPatchDefinition> selected = Set.of(HerbPatchDefinition.FALADOR, HerbPatchDefinition.CATHERBY, HerbPatchDefinition.ARDOUGNE,
                HerbPatchDefinition.HOSIDIUS, HerbPatchDefinition.MORYTANIA, HerbPatchDefinition.WEISS);
        List<HerbPatchStatus> statuses = selected.stream().map(patch -> new HerbPatchStatus(patch, List.of())).collect(Collectors.toList());
        return new HerbRunPlanner().plan(account(), statuses, selected, HerbRunCrop.RANARR, HerbRunCompost.ULTRACOMPOST,
                observations, Map.of(), null, now, HerbTransportPreference.CATALOGUE, selection);
    }

    @Test public void herbVisitModesReserveOnlyTheChosenWorkAndDoNotPromoteExpiredTimers()
    {
        Map<HerbPatchDefinition, HerbPatchObservation> observations = Map.of(
                HerbPatchDefinition.FALADOR, herb(PatchCondition.HARVESTABLE, historical),
                HerbPatchDefinition.CATHERBY, herb(PatchCondition.DISEASED, historical),
                HerbPatchDefinition.ARDOUGNE, herb(PatchCondition.DEAD, historical),
                HerbPatchDefinition.HOSIDIUS, herb(PatchCondition.EMPTY, historical),
                HerbPatchDefinition.MORYTANIA, herb(PatchCondition.GROWING, historical));
        HerbRunPlan mature = herbs(observations, OBSERVED_MATURE);
        assertEquals(List.of(HerbPatchDefinition.FALADOR), mature.getStops().stream().map(HerbRunStop::getPatch).collect(Collectors.toList()));
        assertEquals(1, quantity(mature.getSupplies(), ItemID.RANARR_SEED));
        assertEquals(0, quantity(mature.getSupplies(), ItemID.PLANT_CURE));
        assertTrue(mature.getExcluded().get(HerbPatchDefinition.MORYTANIA).contains("estimates do not qualify"));
        HerbRunPlan problems = herbs(observations, OBSERVED_PROBLEMS);
        assertEquals(2, problems.getStops().size()); assertEquals(1, quantity(problems.getSupplies(), ItemID.RANARR_SEED));
        assertEquals(1, quantity(problems.getSupplies(), ItemID.PLANT_CURE));
        assertEquals(3, herbs(observations, OBSERVED_MATURE_OR_PROBLEMS).getStops().size());
        assertEquals(6, herbs(observations, ALL_SELECTED).getStops().size());
        assertEquals(6, herbs(observations, null).getStops().size());
    }

    @Test public void treeModesSeparateMatureDiseaseDeathAndUnobservedStops()
    {
        Set<TreePatchDefinition> selected = Set.of(TreePatchDefinition.VARROCK_TREE, TreePatchDefinition.LUMBRIDGE_TREE,
                TreePatchDefinition.TAVERLEY_TREE, TreePatchDefinition.FALADOR_TREE, TreePatchDefinition.FARMING_GUILD_TREE);
        Map<TreePatchDefinition, TreePatchObservation> observations = Map.of(
                TreePatchDefinition.VARROCK_TREE, tree(TreeFamily.TREE, TreePatchCondition.CHECK_HEALTH, historical),
                TreePatchDefinition.LUMBRIDGE_TREE, tree(TreeFamily.TREE, TreePatchCondition.HEALTH_CHECKED, historical),
                TreePatchDefinition.TAVERLEY_TREE, tree(TreeFamily.TREE, TreePatchCondition.DISEASED, historical),
                TreePatchDefinition.FALADOR_TREE, tree(TreeFamily.TREE, TreePatchCondition.DEAD, historical));
        for (RunVisitSelection selection : values())
        {
            TreeRunPlan result = new TreeRunPlanner().plan(account(), selected, TreeRunCrop.OAK, false, TreeRunCompost.NONE,
                    TreeRunCure.PRUNE, observations, null, now, selection);
            assertEquals(selection == ALL_SELECTED ? 5 : selection == OBSERVED_MATURE_OR_PROBLEMS ? 4 : 2, result.getStops().size());
            assertEquals(selection == OBSERVED_PROBLEMS ? 1 : selection == OBSERVED_MATURE ? 2 : selection == ALL_SELECTED ? 4 : 3,
                    quantity(result.getSupplies(), ItemID.PLANTPOT_OAK_SAPLING));
        }
        TreeRunPlan legacy = new TreeRunPlanner().plan(account(), selected, TreeRunCrop.OAK, false, observations, null, now);
        assertEquals(5, legacy.getStops().size());
    }

    @Test public void gardenMaturityNeverInventsProduceAndExistingKeepPolicyStillReservesNoSeeds()
    {
        Set<GardenPatchDefinition> selected = Set.of(GardenPatchDefinition.RIMMINGTON_BUSH, GardenPatchDefinition.CHAMPIONS_GUILD_BUSH,
                GardenPatchDefinition.ARDOUGNE_BUSH, GardenPatchDefinition.FARMING_GUILD_BUSH, GardenPatchDefinition.ETCETERIA_BUSH);
        Map<GardenPatchDefinition, GardenPatchObservation> observations = Map.of(
                GardenPatchDefinition.RIMMINGTON_BUSH, garden(GardenFamily.BUSH, GardenPatchCondition.CHECK_HEALTH, historical),
                GardenPatchDefinition.CHAMPIONS_GUILD_BUSH, garden(GardenFamily.BUSH, GardenPatchCondition.HEALTH_CHECKED, historical),
                GardenPatchDefinition.ARDOUGNE_BUSH, garden(GardenFamily.BUSH, GardenPatchCondition.DISEASED, historical),
                GardenPatchDefinition.FARMING_GUILD_BUSH, garden(GardenFamily.BUSH, GardenPatchCondition.DEAD, historical),
                GardenPatchDefinition.ETCETERIA_BUSH, garden(GardenFamily.BUSH, GardenPatchCondition.CHECK_HEALTH, historical));
        GardenRunPlan keep = new GardenRunPlanner().plan(account(), selected, GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE,
                false, GardenRunCompost.ULTRACOMPOST, HerbRunCure.PLANT_CURE, observations, null, now, OBSERVED_MATURE);
        assertEquals(2, keep.getStops().size()); assertEquals(0, quantity(keep.getSupplies(), GardenRunCrop.REDBERRIES.getSeedId()));
        assertTrue(keep.getExcluded().get(GardenPatchDefinition.ETCETERIA_BUSH).contains("entry requirements"));
        GardenRunPlan problems = new GardenRunPlanner().plan(account(), selected, GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE,
                false, GardenRunCompost.ULTRACOMPOST, HerbRunCure.PLANT_CURE, observations, null, now, OBSERVED_PROBLEMS);
        assertEquals(2, problems.getStops().size()); assertEquals(1, quantity(problems.getSupplies(), GardenRunCrop.REDBERRIES.getSeedId()));
        assertEquals(1, quantity(problems.getSupplies(), ItemID.SECATEURS));
    }

    @Test public void fieldFilteringRetainsPlotOrderAndOnlyCountsIncludedPlantingsAndPayments()
    {
        Set<FieldPatchDefinition> selected = Set.of(FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_SOUTH,
                FieldPatchDefinition.CATHERBY_NORTH, FieldPatchDefinition.CATHERBY_SOUTH);
        Map<FieldPatchDefinition, FieldPatchObservation> observations = Map.of(
                FieldPatchDefinition.FALADOR_NORTH, field(FieldFamily.ALLOTMENT, FieldPatchCondition.HARVESTABLE, historical),
                FieldPatchDefinition.FALADOR_SOUTH, field(FieldFamily.ALLOTMENT, FieldPatchCondition.DISEASED, historical),
                FieldPatchDefinition.CATHERBY_NORTH, field(FieldFamily.ALLOTMENT, FieldPatchCondition.DEAD, historical));
        FieldRunPlan result = new FieldRunPlanner().plan(account(), selected, FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL,
                true, FieldRunCompost.COMPOST, HerbRunCure.PLANT_CURE, FieldRunWatering.NONE, observations, null, now, OBSERVED_MATURE_OR_PROBLEMS);
        assertEquals(3, result.getStops().size());
        assertEquals(6, quantity(result.getSupplies(), ItemID.POTATO_SEED));
        assertEquals(6, quantity(result.getSupplies(), ItemID.BUCKET_COMPOST)); // two applications + four payment buckets
        assertEquals(1, quantity(result.getSupplies(), ItemID.PLANT_CURE));
        assertEquals(List.of(FieldPatchDefinition.CATHERBY_NORTH, FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_SOUTH),
                result.getStops().stream().map(FieldRunStop::getPatch).collect(Collectors.toList()));
        assertTrue(result.getExcluded().get(FieldPatchDefinition.CATHERBY_SOUTH).contains("Visit filter"));
    }

    @Test public void futureAndWrongFamilyEvidenceCannotPassFilteredPlanning()
    {
        assertTrue(herbs(Map.of(HerbPatchDefinition.FALADOR, herb(PatchCondition.HARVESTABLE, now.plusSeconds(1))), OBSERVED_MATURE).getStops().isEmpty());
        TreeRunPlan wrongTree = new TreeRunPlanner().plan(account(), Set.of(TreePatchDefinition.VARROCK_TREE), TreeRunCrop.OAK,
                false, TreeRunCompost.NONE, TreeRunCure.PRUNE, Map.of(TreePatchDefinition.VARROCK_TREE,
                        tree(TreeFamily.FRUIT_TREE, TreePatchCondition.CHECK_HEALTH, now)), null, now, OBSERVED_MATURE);
        assertTrue(wrongTree.getStops().isEmpty());
        GardenRunPlan wrongGarden = new GardenRunPlanner().plan(account(), Set.of(GardenPatchDefinition.RIMMINGTON_BUSH), GardenRunCrop.REDBERRIES,
                GardenRunMode.KEEP_MATURE, false, GardenRunCompost.NONE, HerbRunCure.PLANT_CURE, Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,
                        garden(GardenFamily.CACTUS, GardenPatchCondition.CHECK_HEALTH, now)), null, now, OBSERVED_MATURE);
        assertTrue(wrongGarden.getStops().isEmpty());
        FieldRunPlan wrongField = new FieldRunPlanner().plan(account(), Set.of(FieldPatchDefinition.FALADOR_NORTH), FieldRunCrop.POTATO,
                FieldRunMode.HARVEST_ALL, false, FieldRunCompost.NONE, HerbRunCure.PLANT_CURE, FieldRunWatering.NONE,
                Map.of(FieldPatchDefinition.FALADOR_NORTH, field(FieldFamily.FLOWER, FieldPatchCondition.HARVESTABLE, now)), null, now, OBSERVED_MATURE);
        assertTrue(wrongField.getStops().isEmpty());
    }

    @Test public void historicalSelectionDoesNotWeakenFreshGuidanceEvidence()
    {
        Map<HerbPatchDefinition, HerbPatchObservation> observations = Map.of(HerbPatchDefinition.FALADOR, herb(PatchCondition.HARVESTABLE, historical));
        HerbRunPlan plan = herbs(observations, OBSERVED_MATURE);
        GuidedRunSession session = new GuidedRunSession(); assertTrue(session.start(plan, HerbRunCompost.ULTRACOMPOST, now));
        session.confirm(now.plusSeconds(1));
        assertFalse(session.observe(HerbPatchDestination.FALADOR.getLocation(), observations, now.plusSeconds(2)));
        assertEquals(GuidedRunStep.TRAVEL, session.snapshot().getStep());
        assertTrue(session.observe(HerbPatchDestination.FALADOR.getLocation(),
                Map.of(HerbPatchDefinition.FALADOR, herb(PatchCondition.HARVESTABLE, now.plusSeconds(3))), now.plusSeconds(3)));
        assertEquals(GuidedRunStep.HARVEST, session.snapshot().getStep());
    }

    @Test public void excludingTheFirstPlotCannotTurnTheRemainingArrivalIntoAFreeLocalWalk()
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of());
        Map<Integer, Integer> runes = Map.of(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1);
        com.farmingnavigator.routing.capability.TravelCapabilitySnapshot travel = new com.farmingnavigator.routing.capability.TravelCapabilitySnapshot(
                true, com.farmingnavigator.routing.capability.SpellbookType.STANDARD, null, runes, Map.of(), Map.of());
        com.farmingnavigator.storage.StorageSnapshot storage = new com.farmingnavigator.storage.StorageSnapshot(
                runes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())), Map.of(), Map.of(), true, now);
        com.farmingnavigator.routing.requirement.RequirementContext context = new com.farmingnavigator.routing.requirement.RequirementContext(
                account, new com.farmingnavigator.FarmingNavigatorConfig(){}, travel, storage);
        FieldRunPlan filtered = new FieldRunPlanner().plan(account, Set.of(FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_SOUTH),
                FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, false, FieldRunCompost.NONE, HerbRunCure.PLANT_CURE, FieldRunWatering.NONE,
                Map.of(FieldPatchDefinition.FALADOR_SOUTH, field(FieldFamily.ALLOTMENT, FieldPatchCondition.HARVESTABLE, historical)), storage, now, OBSERVED_MATURE)
                .withTransports(new com.farmingnavigator.transport.TransportVerificationService().evaluateAll(account, context.getConfig(), travel, storage,
                        new com.farmingnavigator.transport.FieldPatchTransportService().getAllOptions()), HerbTransportPreference.SHARED_BUDGET, context);
        assertEquals(1, filtered.getStops().size());
        assertFalse(filtered.getStops().get(0).isLocalWalk()); assertNotNull(filtered.getStops().get(0).getRecommended());
        assertEquals(com.farmingnavigator.routing.spell.TeleportSpellDefinition.FALADOR_TELEPORT,
                filtered.getStops().get(0).getRecommended().getOption().getTeleportSpellDefinition());
        assertTrue(filtered.getTransportResources().getChecks().stream().allMatch(com.farmingnavigator.routing.requirement.RequirementResult::isReadyNow));
    }

    @Test public void specialLifecycleStatesAndEmptyPatchesNeverQualifyAsMatureOrOrdinaryProblems()
    {
        for (FieldPatchCondition condition : List.of(FieldPatchCondition.LIVING_ANIMA, FieldPatchCondition.WITHERING_ANIMA,
                FieldPatchCondition.MATURE_HESPORI, FieldPatchCondition.COLLECTABLE, FieldPatchCondition.SCARECROW,
                FieldPatchCondition.EMPTY, FieldPatchCondition.WEEDS, FieldPatchCondition.UNKNOWN, FieldPatchCondition.GROWING))
        { assertFalse(OBSERVED_MATURE_OR_PROBLEMS.exclusion(condition).isEmpty()); }
        assertFalse(OBSERVED_MATURE.exclusion(TreePatchCondition.HARVESTED).isEmpty());
        assertFalse(OBSERVED_MATURE.exclusion(TreePatchCondition.STUMP).isEmpty());
        assertTrue(OBSERVED_MATURE.exclusion(TreePatchCondition.HARVESTABLE).isEmpty());
        assertTrue(OBSERVED_PROBLEMS.exclusion(FieldPatchCondition.DEAD).isEmpty());
    }
}
