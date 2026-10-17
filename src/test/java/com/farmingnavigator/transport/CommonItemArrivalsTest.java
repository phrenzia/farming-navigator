package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.guided.tree.*;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.guided.field.*;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Uses the pinned shortest-path item table and community patch-location routes. No game interaction. */
public class CommonItemArrivalsTest
{
    private final Instant now = Instant.parse("2026-10-17T16:00:00Z");
    private RequirementContext context(Map<Integer, Integer> inventory, Map<Integer, Integer> equipment, Map<Integer, Long> bank, boolean known)
    {
        AccountState account = new AccountState(true, 99, 99, 1, 1, 1, 1, 1, 1, Map.of(), Map.of(), Map.of());
        return new RequirementContext(account, new FarmingNavigatorConfig(){},
                new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null, inventory, equipment, Map.of()),
                new StorageSnapshot(inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())),
                        equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())), bank, known, now));
    }
    private List<TransportOption> options()
    {
        return List.of(new TreePatchTransportService().getAllOptions().get(TreePatchDefinition.TAVERLEY_TREE).get(1),
                new GardenPatchTransportService().getAllOptions().get(GardenPatchDefinition.RIMMINGTON_BUSH).get(1),
                new FieldPatchTransportService().getAllOptions().get(FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA).get(1));
    }
    private VerifiedTransportResult verify(RequirementContext context, TransportOption option)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                context.getStorageSnapshot(), Map.of(1, List.of(option))).get(1).get(0);
    }
    private TreeRunPlan tree(RequirementContext context, TreePatchDefinition patch)
    {
        return new TreeRunPlanner().plan(context.getAccountState(), Set.of(patch), TreeRunCrop.OAK, false, Map.of(), context.getStorageSnapshot(), now)
                .withTransports(new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                        context.getStorageSnapshot(), new TreePatchTransportService().getAllOptions()), HerbTransportPreference.SHARED_BUDGET, context);
    }
    private GardenRunPlan garden(RequirementContext context)
    {
        return new GardenRunPlanner().plan(context.getAccountState(), Set.of(GardenPatchDefinition.RIMMINGTON_BUSH), GardenRunCrop.REDBERRIES,
                GardenRunMode.REPLACE_MATURE, false, Map.of(), context.getStorageSnapshot(), now)
                .withTransports(new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                        context.getStorageSnapshot(), new GardenPatchTransportService().getAllOptions()), HerbTransportPreference.SHARED_BUDGET, context);
    }
    private FieldRunPlan field(RequirementContext context)
    {
        return new FieldRunPlanner().plan(context.getAccountState(), Set.of(FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA), FieldRunCrop.BELLADONNA,
                FieldRunMode.HARVEST_ALL, false, Map.of(), context.getStorageSnapshot(), now)
                .withTransports(new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                        context.getStorageSnapshot(), new FieldPatchTransportService().getAllOptions()), HerbTransportPreference.SHARED_BUDGET, context);
    }

    @Test public void everyAcceptedVariantWorksAcrossCarriedEquippedAndBankLocationsWithoutMagic()
    {
        List<ChargedTeleportItemDefinition> definitions = List.of(ChargedTeleportItemDefinition.GAMES_NECKLACE,
                ChargedTeleportItemDefinition.SKILLS_NECKLACE, ChargedTeleportItemDefinition.AMULET_OF_GLORY);
        for (int index = 0; index < definitions.size(); index++)
        {
            for (ChargedTeleportItemDefinition.Variant variant : definitions.get(index).getVariants())
            {
                assertEquals(VerifiedTransportState.READY_NOW, verify(context(Map.of(variant.getItemId(), 1), Map.of(), Map.of(), true), options().get(index)).getState());
                assertEquals(VerifiedTransportState.READY_NOW, verify(context(Map.of(), Map.of(variant.getItemId(), 1), Map.of(), true), options().get(index)).getState());
                assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, verify(context(Map.of(), Map.of(), Map.of(variant.getItemId(), 1L), true), options().get(index)).getState());
            }
        }
    }

    @Test public void unchargedNotedAndUnknownStockCannotBecomeUsableArrivals()
    {
        RequirementContext notes = context(Map.of(ItemID.Cert.NECKLACE_OF_MINIGAMES_8, 1, ItemID.Cert.JEWL_NECKLACE_OF_SKILLS_6, 1,
                ItemID.Cert.AMULET_OF_GLORY_6, 1, ItemID.JEWL_NECKLACE_OF_SKILLS, 1, ItemID.AMULET_OF_GLORY, 1), Map.of(), Map.of(), true);
        for (TransportOption option : options()) { assertEquals(VerifiedTransportState.MISSING, verify(notes, option).getState()); }
        RequirementContext unknown = context(Map.of(ItemID.NECKLACE_OF_MINIGAMES_8, 1), Map.of(), Map.of(), false);
        assertTrue(new TransportVerificationService().evaluateAll(unknown.getAccountState(), unknown.getConfig(), unknown.getTravelCapabilitySnapshot(),
                unknown.getStorageSnapshot(), Map.of(1, options())).isEmpty());
    }

    @Test public void newOptionsAreLowerRankedAndOnlyDescribeTheCorrectDestinationApproach()
    {
        assertEquals(TransportVerificationKind.GAMES_NECKLACE, options().get(0).getVerificationKind());
        assertEquals(TransportVerificationKind.SKILLS_NECKLACE, options().get(1).getVerificationKind());
        assertEquals(TransportVerificationKind.AMULET_OF_GLORY, options().get(2).getVerificationKind());
        for (TransportOption option : options()) { assertEquals(90, option.getBaselineRank()); }
        assertTrue(options().get(0).getInstructions().contains("south through Taverley"));
        assertTrue(options().get(1).getInstructions().contains("Stay outside the guild"));
        assertTrue(options().get(2).getInstructions().contains("Equip suitable gloves"));
        assertEquals(TransportVerificationKind.TELEPORT_SPELL, new TreePatchTransportService().getAllOptions().get(TreePatchDefinition.TAVERLEY_TREE).get(0).getVerificationKind());
        assertEquals(TransportVerificationKind.TELEPORT_SPELL, new GardenPatchTransportService().getAllOptions().get(GardenPatchDefinition.RIMMINGTON_BUSH).get(0).getVerificationKind());
    }

    @Test public void separatePoolsCoverACombinedPlanButOneSkillsChargeCannotCoverTwoComponents()
    {
        RequirementContext context = context(Map.of(ItemID.NECKLACE_OF_MINIGAMES_1, 1, ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1,
                ItemID.AMULET_OF_GLORY_1, 1), Map.of(), Map.of(), true);
        TreeRunPlan tree = tree(context, TreePatchDefinition.TAVERLEY_TREE);
        GardenRunPlan garden = garden(context); FieldRunPlan field = field(context);
        assertEquals(TransportVerificationKind.GAMES_NECKLACE, tree.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertEquals(TransportVerificationKind.SKILLS_NECKLACE, garden.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertEquals(TransportVerificationKind.AMULET_OF_GLORY, field.getStops().get(0).getRecommended().getOption().getVerificationKind());
        CombinedRunPlan combined = new CombinedRunPlanner().plan(Set.of(CombinedRunKind.TREE, CombinedRunKind.GARDEN, CombinedRunKind.FIELD), null, tree, garden, field, context);
        assertEquals(3, combined.getStops().size()); assertTrue(combined.getResources().getChecks().stream().allMatch(RequirementResult::isSatisfied));
        TreeRunPlan guild = tree(context, TreePatchDefinition.FARMING_GUILD_TREE);
        CombinedRunPlan shared = new CombinedRunPlanner().plan(Set.of(CombinedRunKind.TREE, CombinedRunKind.GARDEN), null, guild, garden, null, context);
        assertTrue(shared.getResources().getChecks().stream().anyMatch(check -> check.getState() == RequirementState.BLOCKED));
    }

    @Test public void newBankedArrivalsReachTheExistingPreparationAndTravelHighlights()
    {
        RequirementContext banked = context(Map.of(), Map.of(), Map.of(ItemID.NECKLACE_OF_MINIGAMES_8, 1L,
                ItemID.JEWL_NECKLACE_OF_SKILLS_6, 1L, ItemID.AMULET_OF_GLORY_6, 1L), true);
        TreeRunPlan tree = tree(banked, TreePatchDefinition.TAVERLEY_TREE); TreeRunSession treeSession = new TreeRunSession(); treeSession.start(tree, false, now);
        assertTrue(GuidedRunTargets.fromTree(treeSession.snapshot(now), tree, banked.getTravelCapabilitySnapshot()).getBankItems().contains(ItemID.NECKLACE_OF_MINIGAMES_8));
        treeSession.command(TreeRunCommand.CONFIRM, now.plusSeconds(1));
        assertTrue(GuidedRunTargets.fromTree(treeSession.snapshot(now), tree, banked.getTravelCapabilitySnapshot()).getInventoryItems().contains(ItemID.NECKLACE_OF_MINIGAMES_1));
        GardenRunPlan garden = garden(banked); GardenRunSession gardenSession = new GardenRunSession(); gardenSession.start(garden, false, now);
        assertTrue(GuidedRunTargets.fromGarden(gardenSession.snapshot(now), garden, banked.getTravelCapabilitySnapshot()).getBankItems().contains(ItemID.JEWL_NECKLACE_OF_SKILLS_6));
        gardenSession.command(GardenRunCommand.CONFIRM, now.plusSeconds(1));
        assertTrue(GuidedRunTargets.fromGarden(gardenSession.snapshot(now), garden, banked.getTravelCapabilitySnapshot()).getInventoryItems().contains(ItemID.JEWL_NECKLACE_OF_SKILLS_1));
        FieldRunPlan field = field(banked); FieldRunSession fieldSession = new FieldRunSession(); fieldSession.start(field, false, now);
        assertTrue(GuidedRunTargets.fromField(fieldSession.snapshot(now), field, banked.getTravelCapabilitySnapshot()).getBankItems().contains(ItemID.AMULET_OF_GLORY_6));
        fieldSession.command(FieldRunCommand.CONFIRM, now.plusSeconds(1));
        assertTrue(GuidedRunTargets.fromField(fieldSession.snapshot(now), field, banked.getTravelCapabilitySnapshot()).getInventoryItems().contains(ItemID.AMULET_OF_GLORY_INF));
    }
}
