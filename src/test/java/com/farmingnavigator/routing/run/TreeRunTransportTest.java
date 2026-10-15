package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementState;
import com.farmingnavigator.routing.requirement.RuneKind;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeRunTransportTest
{
    private final AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of()).withRealAttackLevel(99);
    private RequirementContext context(Map<Integer, Integer> inventory, Map<Integer, Long> bank)
    {
        Map<Integer, Long> carried = inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue()));
        return new RequirementContext(account, new FarmingNavigatorConfig() {}, new TravelCapabilitySnapshot(true,
                SpellbookType.STANDARD, null, inventory, Map.of(), Map.of()), new StorageSnapshot(carried, Map.of(), bank, true, Instant.now()));
    }
    private Map<TreePatchDefinition, List<VerifiedTransportResult>> verified(RequirementContext context)
    {
        return new TransportVerificationService().evaluateAll(account, context.getConfig(), context.getTravelCapabilitySnapshot(),
                context.getStorageSnapshot(), new TreePatchTransportService().getAllOptions());
    }
    @Test public void genericVerifierChecksTreeDestinationsWithoutHerbIdentityMapping()
    {
        RequirementContext ctx = context(Map.of(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1), Map.of());
        Map<TreePatchDefinition, List<VerifiedTransportResult>> results = verified(ctx);
        assertTrue(results.get(TreePatchDefinition.VARROCK_TREE).get(0).isUsableForPlannedRun());
        assertEquals(TeleportSpellDefinition.VARROCK_TELEPORT, results.get(TreePatchDefinition.VARROCK_TREE).get(0).getOption().getTeleportSpellDefinition());
        assertTrue(results.get(TreePatchDefinition.VARROCK_TREE).get(0).getOption().getInstructions().contains("choose Varrock explicitly"));
        assertFalse(results.containsKey(TreePatchDefinition.FOSSIL_ISLAND_EAST));
    }
    @Test public void individuallyReadyTreeSpellsCannotReuseOneLawRune()
    {
        RequirementContext ctx = context(Map.of(ItemID.AIRRUNE, 6, ItemID.FIRERUNE, 1, ItemID.EARTHRUNE, 1, ItemID.LAWRUNE, 1), Map.of());
        Map<TreePatchDefinition, List<VerifiedTransportResult>> results = verified(ctx);
        assertTrue(results.get(TreePatchDefinition.VARROCK_TREE).get(0).isUsableForPlannedRun());
        assertTrue(results.get(TreePatchDefinition.LUMBRIDGE_TREE).get(0).isUsableForPlannedRun());
        TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.VARROCK_TREE,
                TreePatchDefinition.LUMBRIDGE_TREE), TreeRunCrop.OAK, true, Map.of(), ctx.getStorageSnapshot(), Instant.now())
                .withTransports(results, HerbTransportPreference.CATALOGUE, ctx);
        assertEquals(RequirementState.BLOCKED, plan.getTransportResources().getChecks().get(0).getState());
        assertTrue(plan.getTransportResources().getChecks().get(0).getDetail().contains("Law rune x2"));
        assertTrue(plan.getStops().stream().allMatch(stop -> stop.getRecommended() != null));
    }
    @Test public void treeSpellShortfallCanBePreparedFromVerifiedBank()
    {
        RequirementContext ctx = context(Map.of(ItemID.AIRRUNE, 6, ItemID.FIRERUNE, 1, ItemID.EARTHRUNE, 1, ItemID.LAWRUNE, 1), Map.of(ItemID.LAWRUNE, 1L));
        TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.VARROCK_TREE,
                TreePatchDefinition.LUMBRIDGE_TREE), TreeRunCrop.OAK, true, Map.of(), ctx.getStorageSnapshot(), Instant.now())
                .withTransports(verified(ctx), HerbTransportPreference.CATALOGUE, ctx);
        assertTrue(plan.getTransportResources().getChecks().get(0).requiresBankWithdrawal());
        assertTrue(plan.getTransportResources().getBankItems().contains(ItemID.LAWRUNE));
    }
    @Test public void twoTreeWealthTeleportsReserveTwoCharges()
    {
        RequirementContext ctx = context(Map.of(ItemID.RING_OF_WEALTH_1, 1), Map.of());
        TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.VARROCK_TREE,
                TreePatchDefinition.FALADOR_TREE), TreeRunCrop.OAK, true, Map.of(), ctx.getStorageSnapshot(), Instant.now())
                .withTransports(verified(ctx), HerbTransportPreference.CATALOGUE, ctx);
        assertTrue(plan.getStops().stream().allMatch(stop -> stop.getRecommended() != null));
        assertEquals(RequirementState.BLOCKED, plan.getTransportResources().getChecks().get(0).getState());
    }
    @Test public void unsupportedManualLegDoesNotAddImaginarySupplies()
    {
        RequirementContext ctx = context(Map.of(), Map.of());
        TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.GNOME_STRONGHOLD_TREE),
                TreeRunCrop.OAK, true, Map.of(), ctx.getStorageSnapshot(), Instant.now())
                .withTransports(verified(ctx), HerbTransportPreference.CATALOGUE, ctx);
        assertNull(plan.getStops().get(0).getRecommended());
        assertTrue(plan.getTransportResources().getChecks().isEmpty());
        assertTrue(plan.getTransportResources().getNotes().get(0).contains("Gnome Stronghold"));
    }
    @Test public void newSpellDefinitionsHaveExactCostsAndLevels()
    {
        assertEquals(25, TeleportSpellDefinition.VARROCK_TELEPORT.getRequiredMagicLevel());
        assertEquals(31, TeleportSpellDefinition.LUMBRIDGE_TELEPORT.getRequiredMagicLevel());
        assertEquals(3, TeleportSpellDefinition.VARROCK_TELEPORT.getRuneCost().getQuantity(RuneKind.AIR));
        assertEquals(1, TeleportSpellDefinition.VARROCK_TELEPORT.getRuneCost().getQuantity(RuneKind.FIRE));
        assertEquals(1, TeleportSpellDefinition.LUMBRIDGE_TELEPORT.getRuneCost().getQuantity(RuneKind.EARTH));
    }
    @Test public void unverifiedBankWithholdsTravelRecommendations()
    {
        RequirementContext ctx = context(Map.of(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1), Map.of());
        assertTrue(new TransportVerificationService().evaluateAll(account, ctx.getConfig(), ctx.getTravelCapabilitySnapshot(),
                new StorageSnapshot(Map.of(), Map.of(), Map.of(), false, null), new TreePatchTransportService().getAllOptions()).isEmpty());
    }
}
