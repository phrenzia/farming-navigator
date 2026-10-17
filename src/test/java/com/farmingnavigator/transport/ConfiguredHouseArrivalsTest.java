package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.routing.capability.*;
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

public class ConfiguredHouseArrivalsTest
{
    private final Instant now = Instant.parse("2026-10-17T18:00:00Z");
    private final Map<TransportVerificationKind, PohHouseLocation> locations = Map.of(
            TransportVerificationKind.POH_RIMMINGTON_HOUSE_LOCATION, PohHouseLocation.RIMMINGTON,
            TransportVerificationKind.POH_TAVERLEY_HOUSE_LOCATION, PohHouseLocation.TAVERLEY,
            TransportVerificationKind.POH_BRIMHAVEN_HOUSE_LOCATION, PohHouseLocation.BRIMHAVEN,
            TransportVerificationKind.POH_YANILLE_HOUSE_LOCATION, PohHouseLocation.YANILLE,
            TransportVerificationKind.POH_PRIFDDINAS_HOUSE_LOCATION, PohHouseLocation.PRIFDDINAS);
    private RequirementContext context(PohHouseLocation location, HerbHouseEntryMode mode, QuestState elves, int magic,
            SpellbookType book, Map<Integer, Integer> inventory, Map<Integer, Long> bank, boolean known)
    {
        FarmingNavigatorConfig config = new FarmingNavigatorConfig() {
            @Override public PohHouseLocation pohHouseLocation() { return location; }
            @Override public HerbHouseEntryMode herbHouseEntryMode() { return mode; }
        };
        AccountState account = new AccountState(true, 99, 99, 99, magic, 99, 99, 99, 99,
                elves == null ? Map.of() : Map.of(Quest.SONG_OF_THE_ELVES, elves), Map.of(), Map.of()).withRealAttackLevel(99);
        return new RequirementContext(account, config, new TravelCapabilitySnapshot(true, book, null, inventory, Map.of(), Map.of()),
                new StorageSnapshot(inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())), Map.of(), bank, known, now));
    }
    private RequirementContext tablets(PohHouseLocation location, Map<Integer, Integer> inventory, Map<Integer, Long> bank)
    { return context(location, HerbHouseEntryMode.TABLETS, QuestState.FINISHED, 1, SpellbookType.LUNAR, inventory, bank, true); }
    private TransportOption option(TransportVerificationKind kind)
    {
        return Stream.of(new TreePatchTransportService().getAllOptions().values(), new GardenPatchTransportService().getAllOptions().values(),
                new FieldPatchTransportService().getAllOptions().values()).flatMap(java.util.Collection::stream).flatMap(List::stream)
                .filter(option -> option.getVerificationKind() == kind).findFirst().get();
    }
    private VerifiedTransportResult verify(RequirementContext context, TransportOption option)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                context.getStorageSnapshot(), Map.of(1, List.of(option))).get(1).get(0);
    }
    private TreeRunPlan tree(RequirementContext context, TreePatchDefinition patch, TreeRunCrop crop)
    {
        return new TreeRunPlanner().plan(context.getAccountState(), Set.of(patch), crop, false, Map.of(), context.getStorageSnapshot(), now)
                .withTransports(new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                        context.getStorageSnapshot(), new TreePatchTransportService().getAllOptions()), HerbTransportPreference.SHARED_BUDGET, context);
    }
    private FieldRunPlan field(RequirementContext context)
    {
        return new FieldRunPlanner().plan(context.getAccountState(), Set.of(FieldPatchDefinition.PRIFDDINAS_NORTH, FieldPatchDefinition.PRIFDDINAS_SOUTH),
                FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, false, Map.of(), context.getStorageSnapshot(), now)
                .withTransports(new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                        context.getStorageSnapshot(), new FieldPatchTransportService().getAllOptions()), HerbTransportPreference.SHARED_BUDGET, context);
    }

    @Test public void everyConfiguredExitAcceptsCarriedOrBankedHouseTabletsWithoutMagicOrSpellbook()
    {
        for (Map.Entry<TransportVerificationKind, PohHouseLocation> location : locations.entrySet())
        {
            TransportOption option = option(location.getKey());
            assertEquals(95, option.getBaselineRank()); assertEquals(TransportCategory.PLAYER_OWNED_HOUSE, option.getCategory());
            assertTrue(option.getInstructions().contains("outside arrival or leave through its portal"));
            assertEquals(VerifiedTransportState.READY_NOW, verify(tablets(location.getValue(), Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of()), option).getState());
            assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, verify(tablets(location.getValue(), Map.of(), Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1L)), option).getState());
            assertFalse(verify(tablets(PohHouseLocation.NONE, Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of()), option).isUsableForPlannedRun());
            assertFalse(verify(tablets(PohHouseLocation.POLLNIVNEACH, Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of()), option).isUsableForPlannedRun());
            assertFalse(verify(tablets(location.getValue(), Map.of(), Map.of()), option).isUsableForPlannedRun());
        }
    }

    @Test public void configuredSpellArrivalsCheckMagicBookAndRuneStock()
    {
        Map<Integer, Integer> runes = Map.of(ItemID.AIRRUNE, 1, ItemID.EARTHRUNE, 1, ItemID.LAWRUNE, 1);
        for (Map.Entry<TransportVerificationKind, PohHouseLocation> location : locations.entrySet())
        {
            TransportOption option = option(location.getKey());
            assertEquals(VerifiedTransportState.READY_NOW, verify(context(location.getValue(), HerbHouseEntryMode.SPELL, QuestState.FINISHED,
                    40, SpellbookType.STANDARD, runes, Map.of(), true), option).getState());
            assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, verify(context(location.getValue(), HerbHouseEntryMode.SPELL, QuestState.FINISHED,
                    40, SpellbookType.STANDARD, Map.of(), Map.of(ItemID.AIRRUNE, 1L, ItemID.EARTHRUNE, 1L, ItemID.LAWRUNE, 1L), true), option).getState());
            assertFalse(verify(context(location.getValue(), HerbHouseEntryMode.SPELL, QuestState.FINISHED, 39, SpellbookType.STANDARD, runes, Map.of(), true), option).isUsableForPlannedRun());
            assertFalse(verify(context(location.getValue(), HerbHouseEntryMode.SPELL, QuestState.FINISHED, 99, SpellbookType.ARCEUUS, runes, Map.of(), true), option).isUsableForPlannedRun());
        }
    }

    @Test public void missingConfigurationUnknownBankAndIncompletePrifddinasQuestRemainExplicit()
    {
        TransportOption prif = option(TransportVerificationKind.POH_PRIFDDINAS_HOUSE_LOCATION);
        for (QuestState quest : new QuestState[]{null, QuestState.NOT_STARTED, QuestState.IN_PROGRESS})
        {
            assertEquals(VerifiedTransportState.LOCKED, verify(context(PohHouseLocation.PRIFDDINAS, HerbHouseEntryMode.TABLETS,
                    quest, 99, SpellbookType.STANDARD, Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of(), true), prif).getState());
        }
        RequirementContext base = tablets(PohHouseLocation.RIMMINGTON, Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of());
        assertEquals(VerifiedTransportState.CONFIGURATION_REQUIRED, new TransportVerificationService().evaluateAll(base.getAccountState(), null,
                base.getTravelCapabilitySnapshot(), base.getStorageSnapshot(), Map.of(1, List.of(option(TransportVerificationKind.POH_RIMMINGTON_HOUSE_LOCATION)))).get(1).get(0).getState());
        RequirementContext unknown = context(PohHouseLocation.RIMMINGTON, HerbHouseEntryMode.TABLETS, null, 1, SpellbookType.LUNAR,
                Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of(), false);
        assertTrue(new TransportVerificationService().evaluateAll(unknown.getAccountState(), unknown.getConfig(), unknown.getTravelCapabilitySnapshot(),
                unknown.getStorageSnapshot(), Map.of(1, List.of(option(TransportVerificationKind.POH_RIMMINGTON_HOUSE_LOCATION)))).isEmpty());
    }

    @Test public void prifddinasPlotsShareOneArrivalButASeparateTreeComponentStillNeedsAnotherEntry()
    {
        RequirementContext one = tablets(PohHouseLocation.PRIFDDINAS, Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of());
        FieldRunPlan field = field(one); assertEquals(2, field.getStops().size());
        assertEquals(TransportVerificationKind.POH_PRIFDDINAS_HOUSE_LOCATION, field.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(field.getStops().get(1).isLocalWalk());
        assertTrue(field.getTransportResources().getConsumables().stream().allMatch(check -> check.getState() == com.farmingnavigator.loadout.SupplyState.ALREADY_CARRIED));
        assertEquals(1, field.getTransportResources().getConsumables().get(0).getRequirement().getQuantity());
        TreeRunPlan tree = tree(one, TreePatchDefinition.PRIFDDINAS_CRYSTAL, TreeRunCrop.CRYSTAL);
        CombinedRunPlan combined = new CombinedRunPlanner().plan(Set.of(CombinedRunKind.TREE, CombinedRunKind.FIELD), null, tree, null, field, one);
        assertTrue(combined.getBlockers().isEmpty());
        assertTrue(combined.getResources().getConsumables().stream().anyMatch(check -> check.getRequirement().getItemId() == ItemID.POH_TABLET_TELEPORTTOHOUSE
                && check.getRequirement().getQuantity() == 2 && check.getMissingQuantity() == 1));
        RequirementContext extra = tablets(PohHouseLocation.PRIFDDINAS, Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1L));
        CombinedRunPlan banked = new CombinedRunPlanner().plan(Set.of(CombinedRunKind.TREE, CombinedRunKind.FIELD), null,
                tree(extra, TreePatchDefinition.PRIFDDINAS_CRYSTAL, TreeRunCrop.CRYSTAL), null, field(extra), extra);
        assertTrue(banked.getResources().getConsumables().stream().anyMatch(check -> check.getWithdrawQuantity() == 1 && check.getMissingQuantity() == 0));
    }

    @Test public void spellModeBudgetsEveryHouseEntryAndDoesNotSpendAvailableTabletsInstead()
    {
        RequirementContext spell = context(PohHouseLocation.PRIFDDINAS, HerbHouseEntryMode.SPELL, QuestState.FINISHED, 40, SpellbookType.STANDARD,
                Map.of(ItemID.AIRRUNE, 2, ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 2), Map.of(), true);
        CombinedRunPlan covered = new CombinedRunPlanner().plan(Set.of(CombinedRunKind.TREE, CombinedRunKind.FIELD), null,
                tree(spell, TreePatchDefinition.PRIFDDINAS_CRYSTAL, TreeRunCrop.CRYSTAL), null, field(spell), spell);
        assertTrue(covered.getResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        assertTrue(covered.getResources().getConsumables().isEmpty());
        RequirementContext shortRun = context(PohHouseLocation.PRIFDDINAS, HerbHouseEntryMode.SPELL, QuestState.FINISHED, 40, SpellbookType.STANDARD,
                Map.of(ItemID.AIRRUNE, 1, ItemID.EARTHRUNE, 1, ItemID.LAWRUNE, 1, ItemID.POH_TABLET_TELEPORTTOHOUSE, 2), Map.of(), true);
        CombinedRunPlan shortPlan = new CombinedRunPlanner().plan(Set.of(CombinedRunKind.TREE, CombinedRunKind.FIELD), null,
                tree(shortRun, TreePatchDefinition.PRIFDDINAS_CRYSTAL, TreeRunCrop.CRYSTAL), null, field(shortRun), shortRun);
        assertTrue(shortPlan.getResources().getChecks().stream().anyMatch(check -> !check.isSatisfied()));
    }

    @Test public void taverleyBrimhavenAndRimmingtonPlansKeepManualApproachesAndHouseEntryHighlights()
    {
        RequirementContext brim = context(PohHouseLocation.BRIMHAVEN, HerbHouseEntryMode.TABLETS, null, 1, SpellbookType.LUNAR,
                Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Map.of(), true);
        TreeRunPlan calquat = tree(brim, TreePatchDefinition.TAI_BWO_WANNAI_CALQUAT, TreeRunCrop.CALQUAT);
        assertEquals(TransportVerificationKind.POH_BRIMHAVEN_HOUSE_LOCATION, calquat.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(calquat.getStops().get(0).getRecommended().getOption().getInstructions().contains("south-east"));
        RequirementContext banked = tablets(PohHouseLocation.RIMMINGTON, Map.of(), Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1L));
        GardenRunPlan garden = new GardenRunPlanner().plan(banked.getAccountState(), Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),
                GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE, false, Map.of(), banked.getStorageSnapshot(), now)
                .withTransports(new TransportVerificationService().evaluateAll(banked.getAccountState(), banked.getConfig(), banked.getTravelCapabilitySnapshot(),
                        banked.getStorageSnapshot(), new GardenPatchTransportService().getAllOptions()), HerbTransportPreference.SHARED_BUDGET, banked);
        GardenRunSession session = new GardenRunSession(); assertTrue(session.start(garden, false, now));
        assertTrue(GuidedRunTargets.fromGarden(session.snapshot(now), garden, banked.getTravelCapabilitySnapshot()).getBankItems().contains(ItemID.POH_TABLET_TELEPORTTOHOUSE));
        session.command(GardenRunCommand.CONFIRM, now.plusSeconds(1));
        assertTrue(GuidedRunTargets.fromGarden(session.snapshot(now), garden, banked.getTravelCapabilitySnapshot()).getInventoryItems().contains(ItemID.POH_TABLET_TELEPORTTOHOUSE));
        TransportOption rewritten = new FieldPatchTransportService().getAllOptions().get(FieldPatchDefinition.PRIFDDINAS_SOUTH).stream()
                .filter(value -> value.getVerificationKind() == TransportVerificationKind.POH_PRIFDDINAS_HOUSE_LOCATION).findFirst().get();
        assertTrue(rewritten.getInstructions().contains(FieldPatchDefinition.PRIFDDINAS_SOUTH.getLabel()));
        assertEquals(TransportCategory.PLAYER_OWNED_HOUSE, rewritten.getCategory());
    }

    @Test public void prifddinasHouseExitInstructionsAndOneSharedEntryRenderInTheSidebar() throws Exception
    {
        FieldRunPlan plan = field(tablets(PohHouseLocation.PRIFDDINAS, Map.of(), Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1L)));
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            com.farmingnavigator.ui.FieldRunPlanPanel panel = new com.farmingnavigator.ui.FieldRunPlanPanel(plan, true, open -> {});
            String text = labels(panel);
            assertTrue(text.contains("Configured Prifddinas house")); assertTrue(text.contains("outside arrival or leave through its portal"));
            assertTrue(text.contains("POH entry budget: 1")); assertTrue(text.contains("AVAILABLE FROM BANK"));
            panel.setSize(225, panel.getPreferredSize().height); layout(panel);
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(225, panel.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            java.io.File directory = new java.io.File("build/verification"); directory.mkdirs();
            try { javax.imageio.ImageIO.write(image, "png", new java.io.File(directory, "configured-house-arrival.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
        });
    }
    private String labels(java.awt.Component component)
    {
        StringBuilder result = new StringBuilder(component instanceof javax.swing.JLabel ? ((javax.swing.JLabel) component).getText() : "");
        if (component instanceof java.awt.Container) { for (java.awt.Component child : ((java.awt.Container) component).getComponents()) { result.append(labels(child)); } }
        return result.toString();
    }
    private void layout(java.awt.Container parent)
    { parent.doLayout(); for (java.awt.Component child : parent.getComponents()) { if (child instanceof java.awt.Container) { layout((java.awt.Container) child); } } }
}
