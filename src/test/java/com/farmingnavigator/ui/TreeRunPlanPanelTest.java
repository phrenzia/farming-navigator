package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.routing.run.TreeRunCrop;
import com.farmingnavigator.routing.run.TreeRunPlan;
import com.farmingnavigator.routing.run.TreeRunPlanner;
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
public class TreeRunPlanPanelTest
{
    @Test public void expandsAndExplainsUnverifiedBankAndManualTravel() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of());
            TreeRunPlan plan = new TreeRunPlanner().plan(account, EnumSet.of(TreePatchDefinition.VARROCK_TREE,
                    TreePatchDefinition.FARMING_GUILD_TREE, TreePatchDefinition.AUBURNVALE_TREE), TreeRunCrop.OAK, true,
                    Map.of(), new StorageSnapshot(Map.of(), Map.of(), Map.of(), false, null), Instant.now());
            AtomicBoolean expanded = new AtomicBoolean();
            TreeRunPlanPanel panel = new TreeRunPlanPanel(plan, false, expanded::set);
            JButton toggle = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            toggle.doClick(); assertTrue(expanded.get());
            String labels = labels(panel);
            assertTrue(labels.contains("Oak - 2 stops"));
            assertTrue(labels.contains("Travel manually"));
            assertTrue(labels.contains("2 x Oak sapling"));
            assertTrue(labels.contains("2 x Tomatoes (full basket of 5)"));
            assertTrue(labels.contains("open the bank"));
            assertTrue(labels.contains("entry requirements have not been verified"));
            panel.setSize(225, 1750); layout(panel);
            BufferedImage image = new BufferedImage(225, 1750, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "tree-run-preparation.png")); }
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
