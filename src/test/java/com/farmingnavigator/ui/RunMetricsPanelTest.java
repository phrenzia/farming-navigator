package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.RunMetricsTracker;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunMetricsPanelTest
{
    @Test public void rendersRecordedXpWithAttributionAndTimingLimits() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.parse("2026-10-15T12:00:00Z"); RunMetricsTracker tracker = new RunMetricsTracker();
            tracker.start("Tree run", 1000, now); tracker.finish(61000, now.plusSeconds(600), true);
            RunMetricsPanel panel = new RunMetricsPanel(tracker.snapshot(now.plusSeconds(1000)));
            String labels = labels(panel);
            assertTrue(labels.contains("Tree run - completed")); assertTrue(labels.contains("Elapsed: 10 min"));
            assertTrue(labels.contains("Recorded Farming XP:")); assertTrue(labels.contains("Recorded XP/hour:"));
            assertTrue(labels.contains("other Farming actions")); assertTrue(labels.contains("No future harvest XP"));
            panel.setSize(225, 420); panel.doLayout();
            BufferedImage image = new BufferedImage(225, 420, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "run-statistics.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void missingXpBaselineDoesNotRenderMisleadingZeroGain() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.now(); RunMetricsTracker tracker = new RunMetricsTracker(); tracker.start("Herb run", -1, now);
            String labels = labels(new RunMetricsPanel(tracker.snapshot(now.plusSeconds(1))));
            assertTrue(labels.contains("XP: data required")); assertTrue(labels.contains("XP/hour: unavailable"));
            assertFalse(labels.contains("Recorded Farming XP: 0"));
        });
    }
    private String labels(RunMetricsPanel panel)
    {
        StringBuilder result = new StringBuilder();
        for (Component component : panel.getComponents()) { if (component instanceof JLabel) { result.append(((JLabel) component).getText()); } }
        return result.toString();
    }
}
