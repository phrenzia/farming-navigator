package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.routing.run.FieldRunCrop;
import com.farmingnavigator.routing.run.FieldRunPlan;
import com.farmingnavigator.routing.run.FieldRunPlanner;
import com.farmingnavigator.storage.StorageSnapshot;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/** Offscreen Swing rendering only. No client startup or game interaction. */
public class FieldRunPlanPanelTest
{
    @Test public void expandsAndExplainsUnverifiedBankAndManualTravel() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of());
            FieldRunPlan plan = new FieldRunPlanner().plan(account, EnumSet.of(FieldPatchDefinition.FALADOR_NORTH,
                    FieldPatchDefinition.FARMING_GUILD_NORTH, FieldPatchDefinition.PRIFDDINAS_NORTH), FieldRunCrop.TOMATO, com.farmingnavigator.routing.run.FieldRunMode.KEEP_MATURE_FLOWERS, true,
                    Map.of(), new StorageSnapshot(Map.of(), Map.of(), Map.of(), false, null), Instant.now());
            AtomicBoolean expanded = new AtomicBoolean();
            FieldRunPlanPanel panel = new FieldRunPlanPanel(plan, false, expanded::set);
            JButton toggle = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            toggle.doClick(); assertTrue(expanded.get());
            String labels = labels(panel);
            assertTrue(labels.contains("Tomato - 2 stops"));
            assertTrue(labels.contains("Keep mature flowers"));
            assertTrue(labels.contains("Travel manually"));
            assertTrue(labels.contains("6 x Tomato seed"));
            assertTrue(labels.contains("4 x Cabbages (full sack of 10)"));
            assertTrue(labels.contains("open the bank"));
            assertTrue(labels.contains("Complete Song of the Elves"));
            panel.setSize(225, 2100); layout(panel);
            BufferedImage image = new BufferedImage(225, 2100, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "field-run-preparation.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
            toggle.doClick(); assertFalse(expanded.get());
        });
    }
    private String labels(Component component)
    {
        StringBuilder value = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() : "");
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents()) { value.append(labels(child)); }
        }
        return value.toString();
    }
    private void layout(Container parent)
    {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
