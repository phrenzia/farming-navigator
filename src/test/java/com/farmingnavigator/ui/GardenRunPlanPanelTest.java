package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.routing.run.GardenRunCrop;
import com.farmingnavigator.routing.run.GardenRunPlan;
import com.farmingnavigator.routing.run.GardenRunPlanner;
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
public class GardenRunPlanPanelTest
{
    @Test public void expandsAndExplainsUnverifiedBankAndManualTravel() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of());
            GardenRunPlan plan = new GardenRunPlanner().plan(account, EnumSet.of(GardenPatchDefinition.RIMMINGTON_BUSH,
                    GardenPatchDefinition.FARMING_GUILD_BUSH, GardenPatchDefinition.ETCETERIA_BUSH), GardenRunCrop.REDBERRIES, com.farmingnavigator.routing.run.GardenRunMode.KEEP_MATURE, true,
                    Map.of(), new StorageSnapshot(Map.of(), Map.of(), Map.of(), false, null), Instant.now());
            AtomicBoolean expanded = new AtomicBoolean();
            GardenRunPlanPanel panel = new GardenRunPlanPanel(plan, false, expanded::set);
            JButton toggle = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            toggle.doClick(); assertTrue(expanded.get());
            String labels = labels(panel);
            assertTrue(labels.contains("Redberry - 2 stops"));
            assertTrue(labels.contains("Keep mature plants"));
            assertTrue(labels.contains("Travel manually"));
            assertTrue(labels.contains("2 x Redberry seed"));
            assertTrue(labels.contains("8 x Cabbages (full sack of 10)"));
            assertTrue(labels.contains("open the bank"));
            assertTrue(labels.contains("entry requirements have not been verified"));
            panel.setSize(225, 2100); layout(panel);
            BufferedImage image = new BufferedImage(225, 2100, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "garden-run-preparation.png")); }
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
