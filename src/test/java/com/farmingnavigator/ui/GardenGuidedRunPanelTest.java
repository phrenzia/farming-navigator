package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.garden.*;
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

public class GardenGuidedRunPanelTest
{
    @Test public void produceConfirmationIsExplicitAndDispatchesOnlyAChecklistCommand() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.parse("2026-10-15T12:00:00Z"); GardenPatchDefinition patch = GardenPatchDefinition.RIMMINGTON_BUSH;
            GardenRunPlan plan = new GardenRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.noneOf(net.runelite.api.Quest.class), false),
                    EnumSet.of(patch), GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE, true, Map.of(), null, now);
            GardenRunSession session = new GardenRunSession(); session.start(plan, true, now); session.command(GardenRunCommand.CONFIRM, now.plusSeconds(1));
            session.observe(patch.getLocation(), Map.of(patch, new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH, 14),
                    now.plusSeconds(2), FarmingGrowthRate.NORMAL)), now.plusSeconds(2));
            AtomicReference<GardenRunCommand> command = new AtomicReference<>();
            GardenGuidedRunPanel panel = new GardenGuidedRunPanel(session.snapshot(now.plusSeconds(3)), false, command::set);
            assertTrue(labels(panel).contains("harvest completion are not automatically verified"));
            JButton button = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton
                    && ((JButton) c).getText().equals("Produce checked (manual)")).findFirst().get();
            button.doClick(); assertEquals(GardenRunCommand.CONFIRM, command.get()); assertFalse(button.isEnabled());
            assertEquals(GardenRunStep.HARVEST, session.snapshot(now).getStep());
            panel.setSize(225, 800); layout(panel);
            BufferedImage image = new BufferedImage(225, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "guided-garden-produce.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void compostConfirmationIsExplicitAndFitsTheSidebar() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.parse("2026-10-15T12:00:00Z"); GardenPatchDefinition patch = GardenPatchDefinition.RIMMINGTON_BUSH;
            GardenRunPlan plan = new GardenRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.noneOf(net.runelite.api.Quest.class), false),
                    EnumSet.of(patch), GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE, true, GardenRunCompost.BOTTOMLESS_ULTRACOMPOST, Map.of(), null, now);
            GardenRunSession session = new GardenRunSession(); session.start(plan, true, now); session.command(GardenRunCommand.CONFIRM, now.plusSeconds(1));
            session.observe(patch.getLocation(), Map.of(patch, new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH, 0),
                    now.plusSeconds(2), FarmingGrowthRate.NORMAL)), now.plusSeconds(2));
            AtomicReference<GardenRunCommand> command = new AtomicReference<>();
            GardenGuidedRunPanel panel = new GardenGuidedRunPanel(session.snapshot(now.plusSeconds(3)), false, command::set);
            assertTrue(labels(panel).contains("application are not observed"));
            JButton button = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton
                    && ((JButton) c).getText().equals("Compost applied (manual)")).findFirst().get();
            button.doClick(); assertEquals(GardenRunCommand.CONFIRM, command.get()); assertFalse(button.isEnabled());
            assertEquals(GardenRunStep.COMPOST, session.snapshot(now).getStep());
            panel.setSize(225, 800); layout(panel);
            BufferedImage image = new BufferedImage(225, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "guided-garden-compost.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void unavailablePlanDisablesStartButton() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            GardenGuidedRunPanel panel = new GardenGuidedRunPanel(null, false, command -> fail("Unexpected command"));
            JButton button = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            assertFalse(button.isEnabled());
        });
    }
    private String labels(Component component)
    {
        StringBuilder result = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() : "");
        if (component instanceof Container) { for (Component child : ((Container) component).getComponents()) { result.append(labels(child)); } }
        return result.toString();
    }
    private void layout(Container parent)
    {
        parent.doLayout(); for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
