package com.farmingnavigator.ui;

import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeObservationsPanelTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private TreePatchObservation observation(TreeFamily family, int raw, Instant at)
    {
        return new TreePatchObservation(new TreePatchStateDecoder().decode(family, raw), at, FarmingGrowthRate.NORMAL);
    }

    @Test public void textSeparatesObservedHealthChecksFromConditionalWindows()
    {
        TreePatchObservation oak = observation(TreeFamily.TREE, 8, now);
        assertTrue(TreeObservationText.describe(oak, ZoneId.of("UTC")).contains("2026-10-15 12:00:00: Oak - Growing"));
        assertEquals("Estimated health check in 120-160 min if healthy.", TreeObservationText.timer(oak, now));
        assertTrue(TreeObservationText.timer(oak, now.plusSeconds(7200)).startsWith("May be ready"));
        assertTrue(TreeObservationText.timer(oak, now.plusSeconds(9600)).contains("if healthy - visit to confirm"));
        TreePatchObservation ready = observation(TreeFamily.TREE, 12, now);
        assertTrue(TreeObservationText.describe(ready, ZoneId.of("UTC")).contains("Ready to check health"));
        assertEquals("", TreeObservationText.timer(ready, now));
        assertTrue(TreeObservationText.describe(null, ZoneId.of("UTC")).contains("Data required"));
    }

    @Test public void rendersThreeFamiliesAndExpansionWithoutStartingClient() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean expanded = new AtomicBoolean();
            TreeObservationsPanel panel = new TreeObservationsPanel(Map.of(
                    TreePatchDefinition.VARROCK_TREE, observation(TreeFamily.TREE, 12, now),
                    TreePatchDefinition.CATHERBY_FRUIT, observation(TreeFamily.FRUIT_TREE, 8, now),
                    TreePatchDefinition.FOSSIL_ISLAND_EAST, observation(TreeFamily.HARDWOOD_TREE, 18, now)), now, false, expanded::set);
            JButton toggle = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            toggle.doClick();
            assertTrue(expanded.get());
            assertTrue(labels(panel).contains("Observed patches: 3 / 25"));
            assertTrue(labels(panel).contains("health-check ready: 1; diseased or dead: 1"));
            assertTrue(labels(panel).contains("Regular trees"));
            assertTrue(labels(panel).contains("Fruit trees"));
            assertTrue(labels(panel).contains("Hardwood trees"));
            assertTrue(labels(panel).contains("Calquat trees"));
            assertTrue(labels(panel).contains("Celastrus trees"));
            assertTrue(labels(panel).contains("Redwood trees"));
            assertTrue(labels(panel).contains("Crystal trees"));
            panel.setSize(225, 2600);
            layout(panel);
            BufferedImage image = new BufferedImage(225, 2600, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "tree-observations.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
            toggle.doClick();
            assertFalse(expanded.get());
        });
    }

    @Test public void futureObservationIsNotCountedAsHealthCheckReady() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            TreeObservationsPanel panel = new TreeObservationsPanel(Map.of(TreePatchDefinition.VARROCK_TREE,
                    observation(TreeFamily.TREE, 12, now.plusSeconds(1))), now, true, value -> {});
            assertTrue(labels(panel).contains("Observed patches: 0 / 25"));
            assertFalse(labels(panel).contains("health-check ready: 1"));
        });
    }

    private String labels(Component component)
    {
        StringBuilder result = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() : "");
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents()) { result.append(labels(child)); }
        }
        return result.toString();
    }
    private void layout(Container parent)
    {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
