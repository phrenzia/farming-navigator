package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.field.*;
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

public class FieldGuidedRunPanelTest
{
    @Test public void keepFlowerConfirmationOnlyDispatchesAChecklistCommand() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.parse("2026-10-15T12:00:00Z"); FieldPatchDefinition patch = FieldPatchDefinition.FALADOR_FLOWER;
            FieldRunPlan plan = new FieldRunPlanner().plan(new AccountState(true, 99, 99, EnumSet.noneOf(net.runelite.api.Quest.class), false),
                    EnumSet.of(patch), FieldRunCrop.LIMPWURT, FieldRunMode.KEEP_MATURE_FLOWERS, true, Map.of(), null, now);
            FieldRunSession session = new FieldRunSession(); session.start(plan, true, now); session.command(FieldRunCommand.CONFIRM, now.plusSeconds(1));
            session.observe(patch.getLocation(), Map.of(patch, new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.FLOWER, 41),
                    now.plusSeconds(2), FarmingGrowthRate.NORMAL)), now.plusSeconds(2));
            AtomicReference<FieldRunCommand> command = new AtomicReference<>();
            FieldGuidedRunPanel panel = new FieldGuidedRunPanel(session.snapshot(now.plusSeconds(3)), false, command::set);
            assertTrue(labels(panel).contains("Harvesting it may remove nearby allotment protection"));
            JButton button = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton
                    && ((JButton) c).getText().equals("Flower kept / finish stop")).findFirst().get();
            button.doClick(); assertEquals(FieldRunCommand.CONFIRM, command.get()); assertFalse(button.isEnabled());
            assertEquals(FieldRunStep.KEEP_FLOWER, session.snapshot(now).getStep());
            panel.setSize(225, 800); layout(panel);
            BufferedImage image = new BufferedImage(225, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "guided-field-keep-flower.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void unavailablePlanDisablesStartButton() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FieldGuidedRunPanel panel = new FieldGuidedRunPanel(null, false, command -> fail("Unexpected command"));
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
