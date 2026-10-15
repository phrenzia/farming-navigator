package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.guided.tree.TreePatchDestination;
import com.farmingnavigator.guided.tree.TreeRunCommand;
import com.farmingnavigator.guided.tree.TreeRunSession;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalquatTravelTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private RequirementContext context(QuestState trio, boolean rings, Map<Integer, Long> inventory, Map<Integer, Long> bank)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Map.of(Quest.TAI_BWO_WANNAI_TRIO, trio), Map.of(), Map.of(), false, rings, false);
        return new RequirementContext(account, new FarmingNavigatorConfig() {}, new TravelCapabilitySnapshot(true, SpellbookType.STANDARD,
                null, inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().intValue())), Map.of(), Map.of()),
                new StorageSnapshot(inventory, Map.of(), bank, true, now));
    }
    private List<VerifiedTransportResult> routes(RequirementContext context)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                context.getStorageSnapshot(), new TreePatchTransportService().getAllOptions()).get(TreePatchDefinition.TAI_BWO_WANNAI_CALQUAT);
    }
    private VerifiedTransportResult scroll(RequirementContext context)
    {
        return routes(context).stream().filter(route -> route.getOption().getVerificationKind() == TransportVerificationKind.TAI_BWO_WANNAI_SCROLL).findFirst().get();
    }
    private TreeRunPlan plan(RequirementContext context)
    {
        return new TreeRunPlanner().plan(context.getAccountState(), EnumSet.of(TreePatchDefinition.TAI_BWO_WANNAI_CALQUAT), TreeRunCrop.CALQUAT,
                true, Map.of(), context.getStorageSnapshot(), now).withTransports(Map.of(TreePatchDefinition.TAI_BWO_WANNAI_CALQUAT, routes(context)),
                        HerbTransportPreference.CATALOGUE, context);
    }
    @Test public void scrollNeedsFinishedTrioButThePatchDoesNot()
    {
        for (QuestState state : List.of(QuestState.NOT_STARTED, QuestState.IN_PROGRESS))
        {
            RequirementContext context = context(state, false, Map.of(ItemID.TELEPORTSCROLL_TAIBWO, 1L), Map.of());
            assertEquals(VerifiedTransportState.LOCKED, scroll(context).getState());
            assertEquals(1, plan(context).getStops().size());
            assertNull(plan(context).getStops().get(0).getRecommended());
        }
    }
    @Test public void looseScrollOwnershipDistinguishesCarriedBankedMissingAndUnsupportedBook()
    {
        assertEquals(VerifiedTransportState.READY_NOW, scroll(context(QuestState.FINISHED, false,
                Map.of(ItemID.TELEPORTSCROLL_TAIBWO, 1L), Map.of())).getState());
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, scroll(context(QuestState.FINISHED, false,
                Map.of(), Map.of(ItemID.TELEPORTSCROLL_TAIBWO, 1L))).getState());
        assertEquals(VerifiedTransportState.MISSING, scroll(context(QuestState.FINISHED, false, Map.of(), Map.of())).getState());
        assertEquals(VerifiedTransportState.MISSING, scroll(context(QuestState.FINISHED, false,
                Map.of(ItemID.BOOKOFSCROLLS_CHARGED, 1L), Map.of())).getState());
    }
    @Test public void ckrUsesFairyRingUnlockAndStaffWithoutTrioOrVarlamoreVisit()
    {
        RequirementContext context = context(QuestState.NOT_STARTED, true, Map.of(ItemID.DRAMEN_STAFF, 1L), Map.of());
        TreeRunPlan plan = plan(context);
        assertEquals(TransportVerificationKind.FAIRY_RING_ACCESS, plan.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(plan.getStops().get(0).getRecommended().getOption().getDisplayName().contains("CKR"));
        assertTrue(plan.getTransportResources().getConsumables().isEmpty());
        assertNull(plan(context(QuestState.NOT_STARTED, true, Map.of(), Map.of())).getStops().get(0).getRecommended());
    }
    @Test public void repeatedScrollLegsCannotReuseOneConsumable()
    {
        RequirementContext context = context(QuestState.FINISHED, false, Map.of(ItemID.TELEPORTSCROLL_TAIBWO, 1L), Map.of());
        TreeRunStop stop = plan(context).getStops().get(0);
        RunTransportResources resources = new RunTransportResourcePlanner().evaluate(List.of(stop, stop), context);
        assertEquals(2, resources.getConsumables().get(0).getRequirement().getQuantity());
        assertEquals(SupplyState.MISSING, resources.getConsumables().get(0).getState());
        assertEquals(1, resources.getConsumables().get(0).getMissingQuantity());
    }
    @Test public void bankedScrollAppearsInPreparationAndTravelTargets()
    {
        RequirementContext context = context(QuestState.FINISHED, false, Map.of(), Map.of(ItemID.TELEPORTSCROLL_TAIBWO, 1L));
        TreeRunPlan plan = plan(context);
        assertEquals(SupplyState.AVAILABLE_FROM_BANK, plan.getTransportResources().getConsumables().get(0).getState());
        TreeRunSession session = new TreeRunSession(); assertTrue(session.start(plan, true, now));
        GuidedRunTargets prep = GuidedRunTargets.fromTree(session.snapshot(now), plan, context.getTravelCapabilitySnapshot());
        assertTrue(prep.getBankItems().contains(ItemID.TELEPORTSCROLL_TAIBWO));
        session.command(TreeRunCommand.CONFIRM, now.plusSeconds(1));
        GuidedRunTargets travel = GuidedRunTargets.fromTree(session.snapshot(now.plusSeconds(1)), plan, context.getTravelCapabilitySnapshot());
        assertTrue(travel.getInventoryItems().contains(ItemID.TELEPORTSCROLL_TAIBWO));
        assertEquals(TreePatchDestination.TAI_BWO_WANNAI_CALQUAT.getLocation(), travel.getDestination());
    }
}
