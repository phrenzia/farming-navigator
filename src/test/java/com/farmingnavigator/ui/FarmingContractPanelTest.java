package com.farmingnavigator.ui;

import com.farmingnavigator.contracts.FarmingContractSnapshot;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.TreeRunCrop;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import com.farmingnavigator.contracts.ContractCrop;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingContractPanelTest
{
    @Test public void activeGuildCropConflictWarnsBeforeReplacementWithoutChangingRun() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmingContractPanel panel = new FarmingContractPanel(FarmingContractSnapshot.decode(true, 11, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.OAK, true, true);
            assertTrue(labels(panel).contains("Magic - Regular tree"));
            assertTrue(labels(panel).contains("differs from this contract"));
            assertTrue(labels(panel).contains("Check before clearing"));
            panel.setSize(225, 650); panel.doLayout();
            BufferedImage image = new BufferedImage(225, 650, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "farming-contract-conflict.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void matchingOtherFamilyOrUnselectedGuildCropDoesNotWarn() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(labels(new FarmingContractPanel(FarmingContractSnapshot.decode(true, 11, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.MAGIC, true, true)).contains("differs from this contract"));
            assertFalse(labels(new FarmingContractPanel(FarmingContractSnapshot.decode(true, 11, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.APPLE, true, true)).contains("differs from this contract"));
            assertFalse(labels(new FarmingContractPanel(FarmingContractSnapshot.decode(true, 11, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.OAK, true, false)).contains("differs from this contract"));
        });
    }
    @Test public void completedUnknownAndUnsupportedFamilyStayExplicit() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(labels(new FarmingContractPanel(FarmingContractSnapshot.decode(true, 11, 1),
                    HerbRunCrop.RANARR, TreeRunCrop.OAK, true, true)).contains("Seed-pack ownership is not checked"));
            assertTrue(labels(new FarmingContractPanel(FarmingContractSnapshot.decode(true, 1000, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.OAK, true, true)).contains("assignment not recognized"));
            assertTrue(labels(new FarmingContractPanel(FarmingContractSnapshot.decode(true, 1, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.OAK, true, true)).contains("Dedicated run planning"));
        });
    }
    @Test public void prepareButtonDispatchesRequestedSpecialContractAndDisablesDuringGuidance() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<ContractCrop> requested = new AtomicReference<>();
            FarmingContractPanel panel = new FarmingContractPanel(FarmingContractSnapshot.decode(true, 51, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.CELASTRUS, false, true, false, requested::set);
            assertFalse(labels(panel).contains("Dedicated run planning"));
            assertTrue(labels(panel).contains("only its Farming Guild patch"));
            JButton button = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            button.doClick(); assertEquals(ContractCrop.CELASTRUS, requested.get()); assertFalse(button.isEnabled());
            FarmingContractPanel busy = new FarmingContractPanel(FarmingContractSnapshot.decode(true, 50, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.REDWOOD, false, true, true, crop -> fail("Busy callback"));
            JButton disabled = (JButton) Arrays.stream(busy.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            assertFalse(disabled.isEnabled()); disabled.doClick();
            FarmingContractPanel unsupported = new FarmingContractPanel(FarmingContractSnapshot.decode(true, 1, 0),
                    HerbRunCrop.RANARR, TreeRunCrop.OAK, false, true, false, crop -> fail("Unsupported callback"));
            assertTrue(Arrays.stream(unsupported.getComponents()).noneMatch(c -> c instanceof JButton));
        });
    }
    private String labels(FarmingContractPanel panel)
    {
        StringBuilder text = new StringBuilder();
        for (Component component : panel.getComponents()) { if (component instanceof JLabel) { text.append(((JLabel) component).getText()); } }
        return text.toString();
    }
}
