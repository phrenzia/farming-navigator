package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.economics.FarmingMaterialEstimate;
import com.farmingnavigator.economics.FarmingMaterialEstimator;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.routing.run.TreeRunCrop;
import com.farmingnavigator.routing.run.TreeRunPlanner;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingMaterialsPanelTest
{
    @Test public void missingPriceIsVisibleAndSubtotalIsNotCalledProfit() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmingMaterialEstimate estimate = new FarmingMaterialEstimator().trees(new TreeRunPlanner().plan(
                    new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false), EnumSet.of(TreePatchDefinition.VARROCK_TREE),
                    TreeRunCrop.MAGIC, true, Map.of(), null, Instant.now()), true,
                    id -> id == ItemID.COCONUT ? 0 : 100000);
            FarmingMaterialsPanel panel = new FarmingMaterialsPanel(estimate); StringBuilder labels = new StringBuilder();
            for (Component component : panel.getComponents()) { if (component instanceof JLabel) { labels.append(((JLabel) component).getText()); } }
            assertTrue(labels.toString().contains("price unavailable")); assertTrue(labels.toString().contains("Known subtotal"));
            assertTrue(labels.toString().contains("Unpriced material lines: 1")); assertTrue(labels.toString().contains("not profit"));
            panel.setBackground(new java.awt.Color(30, 30, 30)); panel.setOpaque(true); panel.setSize(225, 750); panel.doLayout();
            BufferedImage image = new BufferedImage(225, 750, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "farming-materials-value.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
        });
    }
}
