package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementState;
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

public class FieldRunTransportTest
{
    private RequirementContext context(Map<Integer, Long> inventory, Map<Integer, Long> bank)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Map.of(Quest.SONG_OF_THE_ELVES, QuestState.FINISHED, Quest.PLAGUE_CITY, QuestState.FINISHED), Map.of(), Map.of());
        return new RequirementContext(account, new FarmingNavigatorConfig() {}, new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())), Map.of(), Map.of()),
                new StorageSnapshot(inventory, Map.of(), bank, true, Instant.now()));
    }
    private Map<FieldPatchDefinition, List<VerifiedTransportResult>> routes(RequirementContext context)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                context.getStorageSnapshot(), new FieldPatchTransportService().getAllOptions());
    }
    private FieldRunPlan plan(RequirementContext context, FieldPatchDefinition... selected)
    {
        return new FieldRunPlanner().plan(context.getAccountState(), EnumSet.copyOf(List.of(selected)), FieldRunCrop.POTATO, FieldRunMode.KEEP_MATURE_FLOWERS,
                true, Map.of(), context.getStorageSnapshot(), Instant.now()).withTransports(routes(context), HerbTransportPreference.CATALOGUE, context);
    }
    @Test public void twoGuildPlotsShareOneBankedNecklaceCharge()
    {
        RequirementContext context = context(Map.of(), Map.of(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1L));
        FieldRunPlan plan = plan(context, FieldPatchDefinition.FARMING_GUILD_NORTH, FieldPatchDefinition.FARMING_GUILD_SOUTH);
        assertEquals(2, plan.getStops().size()); assertFalse(plan.getStops().get(0).isLocalWalk()); assertTrue(plan.getStops().get(1).isLocalWalk());
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, plan.getStops().get(0).getRecommended().getState());
        assertNull(plan.getStops().get(1).getRecommended()); assertEquals(1, plan.getTransportResources().getChecks().size());
        assertTrue(plan.getTransportResources().getChecks().get(0).requiresBankWithdrawal());
        assertTrue(plan.getTransportResources().getBankItems().contains(ItemID.JEWL_NECKLACE_OF_SKILLS_1));
    }
    @Test public void singleSelectedSouthPlotStillNeedsItsOwnArrival()
    {
        FieldRunPlan plan = plan(context(Map.of(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1L), Map.of()), FieldPatchDefinition.FARMING_GUILD_SOUTH);
        assertFalse(plan.getStops().get(0).isLocalWalk()); assertNotNull(plan.getStops().get(0).getRecommended());
        assertTrue(plan.getTransportResources().getChecks().stream().allMatch(c -> c.isReadyNow()));
    }
    @Test public void adjacentPlotsUseOneSpellButSeparateAreasNeedSeparateRunes()
    {
        RequirementContext context = context(Map.of(ItemID.AIRRUNE, 8L, ItemID.WATERRUNE, 1L, ItemID.LAWRUNE, 1L), Map.of());
        FieldRunPlan adjacent = plan(context, FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_SOUTH);
        assertTrue(adjacent.getStops().get(1).isLocalWalk());
        assertTrue(adjacent.getTransportResources().getChecks().stream().allMatch(c -> c.isReadyNow()));
        FieldRunPlan separate = plan(context, FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.CATHERBY_SOUTH);
        assertTrue(separate.getStops().stream().allMatch(s -> s.getRecommended() != null));
        assertTrue(separate.getStops().stream().noneMatch(FieldRunStop::isLocalWalk));
        assertEquals(RequirementState.BLOCKED, separate.getTransportResources().getChecks().get(0).getState());
        assertTrue(separate.getTransportResources().getChecks().get(0).getDetail().contains("Law rune x2"));
    }
    @Test public void prifddinasPlotsShareOneCrystalAndRequireFinishedQuest()
    {
        FieldRunPlan plan = plan(context(Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_1, 1L), Map.of()), FieldPatchDefinition.PRIFDDINAS_NORTH, FieldPatchDefinition.PRIFDDINAS_SOUTH);
        assertEquals(TransportVerificationKind.PRIFDDINAS_CRYSTAL, plan.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(plan.getStops().get(1).isLocalWalk()); assertTrue(plan.getTransportResources().getChecks().stream().allMatch(c -> c.isReadyNow()));
        assertTrue(plan.getStops().get(0).getRecommended().getOption().getInstructions().contains("Prifddinas - North"));
    }
    @Test public void cataloguePreservesPohAndSpellMetadataAndExcludesUnverifiedKastori()
    {
        Map<FieldPatchDefinition, List<TransportOption>> options = new FieldPatchTransportService().getAllOptions();
        assertEquals(30, options.size()); assertFalse(options.containsKey(FieldPatchDefinition.KASTORI_FLOWER));
        boolean sawPortal = false, sawSpell = false;
        for (Map.Entry<FieldPatchDefinition, List<TransportOption>> entry : options.entrySet())
        {
            for (TransportOption option : entry.getValue())
            {
                assertTrue(option.getInstructions().contains(entry.getKey().getLabel()));
                assertFalse(option.getInstructions().contains("herb patch"));
                if (option.getVerificationKind() == TransportVerificationKind.POH_PORTAL) { assertNotNull(option.getPohPortalDestination()); sawPortal = true; }
                if (option.getVerificationKind() == TransportVerificationKind.TELEPORT_SPELL) { assertNotNull(option.getTeleportSpellDefinition()); sawSpell = true; }
            }
        }
        assertTrue(sawPortal); assertTrue(sawSpell);
    }
    @Test public void travelRefreshPreservesSelectedCompost()
    {
        RequirementContext context = context(Map.of(), Map.of());
        FieldRunPlan plan = new FieldRunPlanner().plan(context.getAccountState(), EnumSet.of(FieldPatchDefinition.FALADOR_NORTH), FieldRunCrop.POTATO,
                FieldRunMode.HARVEST_ALL, true, FieldRunCompost.SUPERCOMPOST, Map.of(), null, Instant.now()).withTransports(routes(context), HerbTransportPreference.CATALOGUE, context);
        assertEquals(FieldRunCompost.SUPERCOMPOST, plan.getCompost());
    }

}
