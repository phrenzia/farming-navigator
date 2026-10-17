package com.farmingnavigator.ui;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.FarmingNavigatorPanel;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.guided.combined.CombinedGuidanceSession;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.farmingnavigator.guided.combined.CombinedGuidanceSession.Command.*;
import static com.farmingnavigator.guided.combined.CombinedGuidanceSession.Progress.*;
import static com.farmingnavigator.routing.run.CombinedRunKind.*;

/** Offscreen Swing only; no RuneLite startup or game interaction. */
public class CombinedGuidancePanelTest
{
    @Test public void explicitControlsReflectRunningBetweenAndFinishedStates() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            CombinedGuidanceSession session = new CombinedGuidanceSession();
            List<CombinedGuidanceSession.Command> commands = new ArrayList<>();
            CombinedGuidancePanel initial = new CombinedGuidancePanel(session.snapshot(), true, "", commands::add);
            button(initial, "Begin sequence").doClick(); assertEquals(List.of(BEGIN), commands);
            session.begin("profile", Set.of(HERB, TREE, GARDEN, FIELD), kind -> true);
            CombinedGuidancePanel running = new CombinedGuidancePanel(session.snapshot(), false, "", commands::add);
            assertTrue(labels(running).contains("Current: Herb"));
            assertTrue(labels(running).contains("0 / 4"));
            assertNull(findButton(running, "Prepare next component"));
            session.observe("profile", Map.of(HERB, COMPLETE));
            CombinedGuidancePanel between = new CombinedGuidancePanel(session.snapshot(), true, "", commands::add);
            assertTrue(labels(between).contains("1 / 4"));
            assertTrue(labels(between).contains("Next: Tree"));
            button(between, "Prepare next component").doClick();
            assertEquals(List.of(BEGIN, NEXT), commands);
            render(between, "combined-guidance-next.png");
            for (CombinedRunKind kind : List.of(TREE, GARDEN, FIELD))
            {
                assertTrue(session.next("profile", next -> true));
                session.observe("profile", Map.of(kind, COMPLETE));
            }
            CombinedGuidancePanel finished = new CombinedGuidancePanel(session.snapshot(), false, "", commands::add);
            assertTrue(labels(finished).contains("4 / 4"));
            assertTrue(labels(finished).contains("Skipped and manually completed"));
            assertNull(findButton(finished, "Prepare next component"));
            button(finished, "End sequence").doClick(); assertEquals(List.of(BEGIN, NEXT, END), commands);
        });
    }

    @Test public void unavailableComponentsExplainAndDisableStartOrContinuation() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            CombinedGuidanceSession session = new CombinedGuidanceSession();
            CombinedGuidancePanel blocked = new CombinedGuidancePanel(session.snapshot(), false, "Review current checklist first.", command -> fail());
            assertTrue(labels(blocked).contains("Review current checklist first."));
            assertFalse(button(blocked, "Begin sequence").isEnabled());
            button(blocked, "Begin sequence").doClick();
            session.begin("profile", Set.of(HERB, TREE), kind -> true);
            session.observe("profile", Map.of(HERB, COMPLETE));
            CombinedGuidancePanel between = new CombinedGuidancePanel(session.snapshot(), false, "Tree preparation has no available stops.", command -> {});
            assertFalse(button(between, "Prepare next component").isEnabled());
            assertTrue(button(between, "End sequence").isEnabled());
            assertTrue(labels(between).contains("no available stops"));
        });
    }

    @Test public void rootRefreshPreservesTheBoundCallbackOfAnOlderWidget() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.now();
            AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of());
            RequirementContext context = new RequirementContext(account, new FarmingNavigatorConfig(){}, null, null);
            GardenRunPlan garden = new GardenRunPlanner().plan(account, Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),
                    GardenRunCrop.REDBERRIES, GardenRunMode.REPLACE_MATURE, false, Map.of(), null, now)
                    .withTransports(Map.of(), HerbTransportPreference.CATALOGUE, context);
            FieldRunPlan field = new FieldRunPlanner().plan(account, Set.of(FieldPatchDefinition.FALADOR_NORTH),
                    FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, false, Map.of(), null, now)
                    .withTransports(Map.of(), HerbTransportPreference.CATALOGUE, context);
            CombinedRunPlan combined = new CombinedRunPlanner().plan(Set.of(GARDEN, FIELD), null, null, garden, field, context);
            FarmingNavigatorPanel root = new FarmingNavigatorPanel(() -> {}, null);
            com.farmingnavigator.storage.StorageSnapshot storage = new com.farmingnavigator.storage.StorageSnapshot(Map.of(), Map.of(), Map.of(), false, null);
            root.setCombinedRunPlan(combined);
            CombinedGuidanceSession session = new CombinedGuidanceSession();
            List<String> received = new ArrayList<>();
            root.setCombinedGuidance(session.snapshot(), "", command -> received.add("old:" + command));
            root.setAccountState(account, storage, List.of(), Map.of(), new FarmingNavigatorConfig(){}, Map.of());
            JButton old = button(root, "Begin sequence");
            root.setCombinedGuidance(session.snapshot(), "", command -> received.add("new:" + command));
            root.setAccountState(account, storage, List.of(), Map.of(), new FarmingNavigatorConfig(){}, Map.of());
            old.doClick(); button(root, "Begin sequence").doClick();
            assertEquals(List.of("old:BEGIN", "new:BEGIN"), received);
            root.setAccountState(AccountState.loggedOut(), storage, List.of(), Map.of(), new FarmingNavigatorConfig(){}, Map.of());
            assertNull(findButton(root, "Begin sequence"));
        });
    }

    private static JButton button(Component component, String label)
    { JButton result = findButton(component, label); assertNotNull(label, result); return result; }
    private static JButton findButton(Component component, String label)
    {
        if (component instanceof JButton && ((JButton) component).getText().equals(label)) { return (JButton) component; }
        if (component instanceof Container) { for (Component child : ((Container) component).getComponents())
        { JButton result = findButton(child, label); if (result != null) { return result; } } }
        return null;
    }
    private static String labels(Component component)
    {
        StringBuilder text = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() : "");
        if (component instanceof Container) { for (Component child : ((Container) component).getComponents()) { text.append(labels(child)); } }
        return text.toString();
    }
    private static void render(JPanel panel, String name)
    {
        panel.setSize(225, panel.getPreferredSize().height); layout(panel);
        BufferedImage image = new BufferedImage(225, panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
        File directory = new File("build/verification"); directory.mkdirs();
        try { ImageIO.write(image, "png", new File(directory, name)); } catch (Exception ex) { throw new AssertionError(ex); }
    }
    private static void layout(Container parent)
    { parent.doLayout(); for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } } }
}
