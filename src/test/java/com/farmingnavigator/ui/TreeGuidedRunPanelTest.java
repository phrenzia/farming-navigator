package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.guided.tree.*;
import com.farmingnavigator.routing.run.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeGuidedRunPanelTest
{
    @Test public void paymentConfirmationIsExplicitAndDispatchesOnlyChecklistCommand() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.parse("2026-10-15T12:00:00Z");
            TreePatchDefinition patch = TreePatchDefinition.VARROCK_TREE;
            TreeRunSession session = new TreeRunSession();
            TreeRunPlan plan = new TreeRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.noneOf(net.runelite.api.Quest.class), false),
                    EnumSet.of(patch), TreeRunCrop.OAK, true, Map.of(), null, now);
            session.start(plan, true, now); session.command(TreeRunCommand.CONFIRM, now.plusSeconds(1));
            session.observe(TreePatchDestination.VARROCK_TREE.getLocation(), Map.of(patch, new TreePatchObservation(
                    new TreePatchStateDecoder().decode(TreeFamily.TREE, 0), now.plusSeconds(2), FarmingGrowthRate.NORMAL)), now.plusSeconds(2));
            session.observe(TreePatchDestination.VARROCK_TREE.getLocation(), Map.of(patch, new TreePatchObservation(
                    new TreePatchStateDecoder().decode(TreeFamily.TREE, 8), now.plusSeconds(3), FarmingGrowthRate.NORMAL)), now.plusSeconds(3));
            AtomicReference<TreeRunCommand> command = new AtomicReference<>();
            TreeGuidedRunPanel panel = new TreeGuidedRunPanel(session.snapshot(now.plusSeconds(4)), false, command::set);
            assertTrue(labels(panel).contains("Payment is not automatically verified"));
            JButton button = (JButton) Arrays.stream(panel.getComponents()).filter(component -> component instanceof JButton
                    && ((JButton) component).getText().equals("Protection paid (manual)")).findFirst().get();
            button.doClick(); assertEquals(TreeRunCommand.CONFIRM, command.get()); assertFalse(button.isEnabled());
            assertEquals(TreeRunStep.PROTECT, session.snapshot(now.plusSeconds(5)).getStep());
            panel.setSize(225, 700); layout(panel);
            BufferedImage image = new BufferedImage(225, 700, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "guided-tree-payment.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void unavailablePlanDisablesStartButton() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            TreeGuidedRunPanel panel = new TreeGuidedRunPanel(null, false, command -> fail("Unexpected command"));
            JButton start = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            assertFalse(start.isEnabled());
        });
    }
    private String labels(Component component)
    {
        StringBuilder text = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() : "");
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents()) { text.append(labels(child)); }
        }
        return text.toString();
    }
    private void layout(Container parent)
    {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
