package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.guided.field.*;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class WatchtowerHopsTransportTest
{
    private final Instant now = Instant.parse("2026-10-17T15:00:00Z");
    private RequirementContext context(QuestState quest, int magic, SpellbookType book,
            Map<Integer, Integer> inventory, Map<Integer, Integer> equipment, Map<Integer, Integer> pouch,
            Map<Integer, Long> bank, boolean known)
    {
        AccountState account = new AccountState(true, 99, 99, 57, magic, 99, 99, 99, 99,
                quest == null ? Map.of() : Map.of(Quest.WATCHTOWER, quest), Map.of(), Map.of()).withRealAttackLevel(1);
        return new RequirementContext(account, new FarmingNavigatorConfig(){},
                new TravelCapabilitySnapshot(true, book, null, inventory, equipment, pouch),
                new StorageSnapshot(inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())),
                        equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())), bank, known, now));
    }
    private RequirementContext ready(Map<Integer, Integer> inventory, Map<Integer, Long> bank)
    { return context(QuestState.FINISHED, 58, SpellbookType.STANDARD, inventory, Map.of(), Map.of(), bank, true); }
    private TransportOption option(boolean tablet)
    {
        return new FieldPatchTransportService().getAllOptions().get(FieldPatchDefinition.YANILLE_HOPS).stream()
                .filter(option -> tablet ? option.getVerificationKind() == TransportVerificationKind.WATCHTOWER_TELEPORT_TABLET
                        : option.getTeleportSpellDefinition() == TeleportSpellDefinition.WATCHTOWER_TELEPORT).findFirst().get();
    }
    private VerifiedTransportResult verify(RequirementContext context, boolean tablet)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                context.getStorageSnapshot(), Map.of(FieldPatchDefinition.YANILLE_HOPS, List.of(option(tablet)))).get(FieldPatchDefinition.YANILLE_HOPS).get(0);
    }
    private FieldRunPlan plan(RequirementContext context, boolean tablet)
    {
        return new FieldRunPlanner().plan(context.getAccountState(), Set.of(FieldPatchDefinition.YANILLE_HOPS), FieldRunCrop.JUTE,
                FieldRunMode.HARVEST_ALL, false, Map.of(), context.getStorageSnapshot(), now)
                .withTransports(Map.of(FieldPatchDefinition.YANILLE_HOPS, List.of(verify(context, tablet))), HerbTransportPreference.SHARED_BUDGET, context);
    }
    private Map<Integer, Integer> runes() { return Map.of(ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 2); }
    private RunTravelStop stop(VerifiedTransportResult arrival)
    {
        return new RunTravelStop() {
            public VerifiedTransportResult getRecommended() { return arrival; }
            public String getTravelLabel() { return "Watchtower test arrival"; }
        };
    }

    @Test public void spellUsesExactRuneCostBoostableMagicAndFinishedQuest()
    {
        TeleportSpellDefinition spell = TeleportSpellDefinition.WATCHTOWER_TELEPORT;
        assertEquals(Map.of(RuneKind.EARTH, 2, RuneKind.LAW, 2), spell.getRuneCost().getQuantities());
        assertEquals(58, spell.getRequiredMagicLevel()); assertEquals(Quest.WATCHTOWER, spell.getRequiredQuest());
        assertEquals(VerifiedTransportState.READY_NOW, verify(ready(runes(), Map.of()), false).getState());
        for (QuestState quest : new QuestState[]{null, QuestState.NOT_STARTED, QuestState.IN_PROGRESS})
        {
            assertFalse(verify(context(quest, 58, SpellbookType.STANDARD, runes(), Map.of(), Map.of(), Map.of(), true), false).isUsableForPlannedRun());
            assertEquals(VerifiedTransportState.LOCKED, verify(context(quest, 99, SpellbookType.LUNAR,
                    Map.of(ItemID.POH_TABLET_WATCHTOWERTELEPORT, 1), Map.of(), Map.of(), Map.of(), true), true).getState());
        }
        assertFalse(verify(context(QuestState.FINISHED, 57, SpellbookType.STANDARD, runes(), Map.of(), Map.of(), Map.of(), true), false).isUsableForPlannedRun());
    }

    @Test public void spellbookPouchAndInfiniteEarthSourceUseExistingRuneChecks()
    {
        for (SpellbookType book : new SpellbookType[]{SpellbookType.LUNAR, SpellbookType.ANCIENT, SpellbookType.ARCEUUS})
        {
            assertFalse(verify(context(QuestState.FINISHED, 99, book, runes(), Map.of(), Map.of(), Map.of(), true), false).isUsableForPlannedRun());
        }
        assertEquals(VerifiedTransportState.READY_NOW, verify(context(QuestState.FINISHED, 58, SpellbookType.STANDARD,
                Map.of(), Map.of(), runes(), Map.of(), true), false).getState());
        assertEquals(VerifiedTransportState.READY_NOW, verify(context(QuestState.FINISHED, 58, SpellbookType.STANDARD,
                Map.of(ItemID.LAWRUNE, 2), Map.of(ItemID.STAFF_OF_EARTH, 1), Map.of(), Map.of(), true), false).getState());
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, verify(ready(Map.of(ItemID.LAWRUNE, 2), Map.of(ItemID.STAFF_OF_EARTH, 1L)), false).getState());
        assertFalse(verify(ready(Map.of(ItemID.LAWRUNE, 1, ItemID.EARTHRUNE, 2), Map.of()), false).isUsableForPlannedRun());
    }

    @Test public void tabletDoesNotNeedSpellbookMagicOrRunesButNeedsItsQuestAndActualStock()
    {
        for (SpellbookType book : new SpellbookType[]{SpellbookType.STANDARD, SpellbookType.LUNAR, SpellbookType.ANCIENT, SpellbookType.ARCEUUS})
        {
            assertEquals(VerifiedTransportState.READY_NOW, verify(context(QuestState.FINISHED, 1, book,
                    Map.of(ItemID.POH_TABLET_WATCHTOWERTELEPORT, 1), Map.of(), Map.of(), Map.of(), true), true).getState());
        }
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, verify(ready(Map.of(), Map.of(ItemID.POH_TABLET_WATCHTOWERTELEPORT, 1L)), true).getState());
        assertEquals(VerifiedTransportState.MISSING, verify(ready(Map.of(), Map.of()), true).getState());
        RequirementContext unknown = context(QuestState.FINISHED, 58, SpellbookType.STANDARD, runes(), Map.of(), Map.of(), Map.of(), false);
        assertTrue(new TransportVerificationService().evaluateAll(unknown.getAccountState(), unknown.getConfig(), unknown.getTravelCapabilitySnapshot(),
                unknown.getStorageSnapshot(), Map.of(FieldPatchDefinition.YANILLE_HOPS, List.of(option(true), option(false)))).isEmpty());
    }

    @Test public void arrivalAndAdditionalCareCannotReuseTheSameLawRunesOrTablets()
    {
        RequirementContext twoLaws = ready(runes(), Map.of());
        RuneCost care = RuneCost.builder().add(RuneKind.LAW, 1).build();
        RunTransportResources shortRun = new RunTransportResourcePlanner().evaluate(List.of(stop(verify(twoLaws, false))), twoLaws, care);
        assertTrue(shortRun.getChecks().stream().anyMatch(check -> !check.isSatisfied()));
        RequirementContext threeLaws = ready(Map.of(ItemID.LAWRUNE, 3, ItemID.EARTHRUNE, 2), Map.of());
        assertTrue(new RunTransportResourcePlanner().evaluate(List.of(stop(verify(threeLaws, false))), threeLaws, care)
                .getChecks().stream().allMatch(RequirementResult::isReadyNow));
        RequirementContext oneTablet = ready(Map.of(ItemID.POH_TABLET_WATCHTOWERTELEPORT, 1), Map.of());
        RunTransportResources twoArrivals = new RunTransportResourcePlanner().evaluate(List.of(stop(verify(oneTablet, true)), stop(verify(oneTablet, true))), oneTablet);
        assertTrue(twoArrivals.getConsumables().stream().anyMatch(check -> check.getState() == SupplyState.MISSING));
        RequirementContext bankExtra = ready(Map.of(ItemID.POH_TABLET_WATCHTOWERTELEPORT, 1), Map.of(ItemID.POH_TABLET_WATCHTOWERTELEPORT, 1L));
        assertTrue(new RunTransportResourcePlanner().evaluate(List.of(stop(verify(bankExtra, true)), stop(verify(bankExtra, true))), bankExtra)
                .getConsumables().stream().anyMatch(check -> check.getState() == SupplyState.AVAILABLE_FROM_BANK && check.getWithdrawQuantity() == 1));
    }

    @Test public void planningAndTravelTargetsPreserveBankWithdrawalsAndEitherManualApproach()
    {
        for (boolean tablet : new boolean[]{false, true})
        {
            RequirementContext context = ready(Map.of(), tablet ? Map.of(ItemID.POH_TABLET_WATCHTOWERTELEPORT, 1L) : Map.of(ItemID.LAWRUNE, 2L, ItemID.EARTHRUNE, 2L));
            FieldRunPlan plan = plan(context, tablet);
            assertTrue(plan.getTransportResources().getChecks().stream().allMatch(RequirementResult::isSatisfied));
            assertTrue(plan.getTransportResources().getNotes().stream().noneMatch(note -> note.contains("unmodelled")));
            FieldRunSession session = new FieldRunSession(); assertTrue(session.start(plan, false, now));
            GuidedRunTargets preparation = GuidedRunTargets.fromField(session.snapshot(now), plan, context.getTravelCapabilitySnapshot());
            assertTrue(preparation.getBankItems().contains(tablet ? ItemID.POH_TABLET_WATCHTOWERTELEPORT : ItemID.LAWRUNE));
            session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1));
            GuidedRunTargets travel = GuidedRunTargets.fromField(session.snapshot(now), plan, context.getTravelCapabilitySnapshot());
            assertTrue(travel.getInventoryItems().contains(tablet ? ItemID.POH_TABLET_WATCHTOWERTELEPORT : ItemID.LAWRUNE));
            assertEquals(FieldPatchDefinition.YANILLE_HOPS.getLocation(), travel.getDestination());
            assertTrue(option(tablet).getInstructions().contains("descend the tower"));
            assertTrue(option(tablet).getInstructions().contains("diary redirect"));
            assertTrue(plan.getStops().get(0).getRecommended().getOption().getInstructions().contains("Use the labelled plot: Yanille"));
        }
    }
}
