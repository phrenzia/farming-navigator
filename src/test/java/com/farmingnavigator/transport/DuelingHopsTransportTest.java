package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.guided.GuidedRunTargets;
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
import java.util.stream.IntStream;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Castle Wars destination/items: shortest-path 99735452c7c257d3fe5ae79970429cdce907d571 teleportation_items.tsv. */
public class DuelingHopsTransportTest
{
    private final Instant now = Instant.parse("2026-10-17T14:00:00Z");
    private RequirementContext context(Map<Integer, Integer> inventory, Map<Integer, Integer> equipment, Map<Integer, Long> bank, boolean known)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of());
        return new RequirementContext(account, new FarmingNavigatorConfig(){},
                new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null, inventory, equipment, Map.of()),
                new StorageSnapshot(inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())),
                        equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())), bank, known, now));
    }
    private TransportOption option()
    {
        return new FieldPatchTransportService().getAllOptions().get(FieldPatchDefinition.YANILLE_HOPS).stream()
                .filter(option -> option.getVerificationKind() == TransportVerificationKind.RING_OF_DUELING).findFirst().get();
    }
    private VerifiedTransportResult verify(RequirementContext context)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(),
                context.getTravelCapabilitySnapshot(), context.getStorageSnapshot(), Map.of(FieldPatchDefinition.YANILLE_HOPS, List.of(option())))
                .get(FieldPatchDefinition.YANILLE_HOPS).get(0);
    }
    private FieldRunPlan plan(RequirementContext context)
    {
        Map<FieldPatchDefinition, List<VerifiedTransportResult>> routes = new TransportVerificationService().evaluateAll(
                context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(), context.getStorageSnapshot(),
                new FieldPatchTransportService().getAllOptions());
        return new FieldRunPlanner().plan(context.getAccountState(), Set.of(FieldPatchDefinition.YANILLE_HOPS), FieldRunCrop.JUTE,
                FieldRunMode.HARVEST_ALL, false, Map.of(), context.getStorageSnapshot(), now)
                .withTransports(routes, HerbTransportPreference.SHARED_BUDGET, context);
    }
    private RunTravelStop stop(VerifiedTransportResult arrival, boolean local)
    {
        return new RunTravelStop() {
            public VerifiedTransportResult getRecommended() { return arrival; }
            public String getTravelLabel() { return "Test arrival"; }
            public boolean isLocalWalk() { return local; }
        };
    }

    @Test public void allEightChargeVariantsWorkCarriedEquippedOrFromVerifiedBank()
    {
        for (ChargedTeleportItemDefinition.Variant variant : ChargedTeleportItemDefinition.RING_OF_DUELING.getVariants())
        {
            assertEquals(VerifiedTransportState.READY_NOW, verify(context(Map.of(variant.getItemId(), 1), Map.of(), Map.of(), true)).getState());
            assertEquals(VerifiedTransportState.READY_NOW, verify(context(Map.of(), Map.of(variant.getItemId(), 1), Map.of(), true)).getState());
            VerifiedTransportResult banked = verify(context(Map.of(), Map.of(), Map.of(variant.getItemId(), 1L), true));
            assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, banked.getState());
            assertTrue(banked.getDetail().contains("Castle Wars"));
        }
    }

    @Test public void missingNotedAndUnknownBankDoNotClaimReadyTravel()
    {
        assertEquals(VerifiedTransportState.MISSING, verify(context(Map.of(), Map.of(), Map.of(), true)).getState());
        assertEquals(VerifiedTransportState.MISSING, verify(context(Map.of(ItemID.Cert.RING_OF_DUELING_8, 1), Map.of(), Map.of(), true)).getState());
        for (boolean carried : new boolean[]{false, true})
        {
            RequirementContext unknown = context(carried ? Map.of(ItemID.RING_OF_DUELING_1, 1) : Map.of(), Map.of(), Map.of(ItemID.RING_OF_DUELING_8, 1L), false);
            assertTrue(new TransportVerificationService().evaluateAll(unknown.getAccountState(), unknown.getConfig(),
                    unknown.getTravelCapabilitySnapshot(), unknown.getStorageSnapshot(), Map.of(FieldPatchDefinition.YANILLE_HOPS, List.of(option()))).isEmpty());
        }
    }

    @Test public void everyChargeTierHasExactSharedCapacityAndLocalWalksSpendNone()
    {
        for (ChargedTeleportItemDefinition.Variant variant : ChargedTeleportItemDefinition.RING_OF_DUELING.getVariants())
        {
            RequirementContext context = context(Map.of(variant.getItemId(), 1), Map.of(), Map.of(), true);
            VerifiedTransportResult arrival = verify(context);
            List<RunTravelStop> stops = IntStream.range(0, variant.getCharges()).mapToObj(i -> stop(arrival, false)).collect(Collectors.toList());
            RunTransportResources exact = new RunTransportResourcePlanner().evaluate(stops, context);
            assertTrue(exact.getChecks().stream().allMatch(RequirementResult::isReadyNow));
            stops.add(stop(arrival, true));
            assertTrue(new RunTransportResourcePlanner().evaluate(stops, context).getChecks().stream().allMatch(RequirementResult::isReadyNow));
            stops.add(stop(arrival, false));
            assertTrue(new RunTransportResourcePlanner().evaluate(stops, context).getChecks().stream().anyMatch(c -> c.getState() == RequirementState.BLOCKED));
        }
        RequirementContext banked = context(Map.of(ItemID.RING_OF_DUELING_1, 1), Map.of(), Map.of(ItemID.RING_OF_DUELING_8, 1L), true);
        RunTransportResources combined = new RunTransportResourcePlanner().evaluate(List.of(stop(verify(banked), false), stop(verify(banked), false)), banked);
        assertTrue(combined.getChecks().stream().anyMatch(RequirementResult::requiresBankWithdrawal));
        assertTrue(combined.getBankItems().contains(ItemID.RING_OF_DUELING_8));
    }

    @Test public void catalogueAndKnownPoolRepairRetainAnExistingFairyRingAlternative()
    {
        List<TransportOption> options = new FieldPatchTransportService().getAllOptions().get(FieldPatchDefinition.YANILLE_HOPS);
        assertEquals(TransportVerificationKind.FAIRY_RING_ACCESS, options.get(0).getVerificationKind());
        assertEquals(90, option().getBaselineRank()); assertEquals(TransportCategory.JEWELLERY, option().getCategory());
        assertTrue(option().getInstructions().contains("longer manual walk"));
        RequirementContext context = context(Map.of(ItemID.RING_OF_DUELING_1, 1), Map.of(), Map.of(), true);
        FieldRunPlan plan = plan(context);
        assertEquals(TransportVerificationKind.RING_OF_DUELING, plan.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(plan.getTransportResources().isKnownPoolsRequired());
        assertTrue(plan.getTransportResources().getChecks().stream().allMatch(RequirementResult::isSatisfied));
    }

    @Test public void activeTravelHighlightsUsableVariantsAndDropsTheArrivalWhenItsLastChargeIsGone()
    {
        RequirementContext banked = context(Map.of(), Map.of(), Map.of(ItemID.RING_OF_DUELING_8, 1L), true);
        FieldRunPlan plan = plan(banked);
        FieldRunSession session = new FieldRunSession(); assertTrue(session.start(plan, false, now));
        GuidedRunTargets preparation = GuidedRunTargets.fromField(session.snapshot(now), plan, banked.getTravelCapabilitySnapshot());
        assertTrue(preparation.getBankItems().contains(ItemID.RING_OF_DUELING_8));
        session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1));
        GuidedRunTargets travel = GuidedRunTargets.fromField(session.snapshot(now), plan, banked.getTravelCapabilitySnapshot());
        for (ChargedTeleportItemDefinition.Variant variant : ChargedTeleportItemDefinition.RING_OF_DUELING.getVariants())
        { assertTrue(travel.getInventoryItems().contains(variant.getItemId())); }
        assertFalse(travel.getInventoryItems().contains(ItemID.Cert.RING_OF_DUELING_8));
        RequirementContext empty = context(Map.of(), Map.of(), Map.of(), true);
        Map<FieldPatchDefinition, List<VerifiedTransportResult>> routes = new TransportVerificationService().evaluateAll(empty.getAccountState(), empty.getConfig(),
                empty.getTravelCapabilitySnapshot(), empty.getStorageSnapshot(), new FieldPatchTransportService().getAllOptions());
        session.refreshTransports(routes, HerbTransportPreference.SHARED_BUDGET, empty);
        assertNull(session.snapshot(now).getStop().getRecommended());
        assertFalse(GuidedRunTargets.fromField(session.snapshot(now), plan, empty.getTravelCapabilitySnapshot()).getInventoryItems().contains(ItemID.RING_OF_DUELING_8));
    }

    @Test public void bankedArrivalShowsCastleWarsAndTheManualWalkInTheSidebar() throws Exception
    {
        FieldRunPlan plan = plan(context(Map.of(), Map.of(), Map.of(ItemID.RING_OF_DUELING_8, 1L), true));
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            com.farmingnavigator.ui.FieldRunPlanPanel panel = new com.farmingnavigator.ui.FieldRunPlanPanel(plan, true, value -> {});
            String labels = labels(panel);
            assertTrue(labels.contains("Ring of dueling to Castle Wars"));
            assertTrue(labels.contains("AVAILABLE FROM BANK"));
            assertTrue(labels.contains("longer manual walk east to Yanille"));
            panel.setSize(225, panel.getPreferredSize().height); layout(panel);
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(225, panel.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            java.io.File directory = new java.io.File("build/verification"); directory.mkdirs();
            try { javax.imageio.ImageIO.write(image, "png", new java.io.File(directory, "dueling-yanille-arrival.png")); }
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
