package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunStopOutcomesPanelTest
{
    private final Instant now = Instant.parse("2026-10-17T18:00:00Z");
    private RunMetricsTracker tracker(int ago)
    {
        RunMetricsTracker tracker = new RunMetricsTracker();
        tracker.start("Field run", 100, now.minusSeconds(ago), RunHistoryContext.of(RunHistoryStore.Kind.FIELD, "POTATO", 3));
        return tracker;
    }
    @Test public void liveAndSavedViewsDistinguishPartialWorkManualCountsAndLegacyUnknown() throws Exception
    {
        RunMetricsTracker tracker = tracker(180); tracker.sampleStops(1, 1, 1);
        RunMetricsSnapshot live = tracker.snapshot(now.minusSeconds(120));
        tracker.finish(250, now.minusSeconds(120), false); RunMetricsSnapshot stopped = tracker.snapshot(now);
        RunMetricsTracker old = tracker(360); old.finish(250, now.minusSeconds(300), true);
        RunMetricsSnapshot legacy = old.snapshot(now);
        RunMetricsTracker skippedTracker = tracker(90); skippedTracker.sampleStops(0, 0, 3); skippedTracker.finish(100, now.minusSeconds(30), true);
        RunMetricsSnapshot skipped = skippedTracker.snapshot(now);
        SwingUtilities.invokeAndWait(() -> {
            RunMetricsPanel panel = new RunMetricsPanel(live); String labels = labels(panel);
            assertTrue(labels.contains("1 completed (1 completed manually), 1 skipped, 1 unfinished"));
            assertTrue(labels.contains("not harvest quantity"));
            RunHistoryPanel history = new RunHistoryPanel(List.of(stopped, legacy, skipped, live), true, open -> {}, () -> {}, now,
                    RunHistoryPeriod.ALL, null, (period, kind) -> {});
            String saved = labels(history);
            assertTrue(saved.contains("3 saved checklists for this character"));
            assertTrue(saved.contains("Showing 3 saved checklists")); // active snapshot is excluded
            assertTrue(saved.contains("1 completed (1 completed manually), 1 skipped, 1 unfinished"));
            assertTrue(saved.contains("0 completed (0 completed manually), 3 skipped, 0 unfinished"));
            assertTrue(saved.contains("Checklist stop outcomes: unavailable"));
            assertTrue(saved.contains("3 planned stops"));
            assertTrue(saved.contains("Checklist stop subtotals: 1 completed (1 completed manually), 4 skipped, 1 unfinished"));
            assertTrue(saved.contains("Stop counts known for 2 / 3 saved checklists"));
            render(history, "history-stop-outcomes.png"); render(panel, "live-stop-outcomes.png");
        });
    }
    @Test public void knownZeroWorkDoesNotBecomeMissingDataWhenXpIsUnavailable() throws Exception
    {
        RunMetricsTracker tracker = new RunMetricsTracker();
        tracker.start("Field run", -1, now.minusSeconds(60), RunHistoryContext.of(RunHistoryStore.Kind.FIELD, "POTATO", 3));
        tracker.sampleStops(0, 0, 0); tracker.finish(-1, now, false);
        SwingUtilities.invokeAndWait(() -> {
            String labels = labels(new RunMetricsPanel(tracker.snapshot(now)));
            assertTrue(labels.contains("0 completed (0 completed manually), 0 skipped, 3 unfinished"));
            assertTrue(labels.contains("XP: data required")); assertFalse(labels.contains("stop outcomes: unavailable"));
        });
    }
    private String labels(Component component)
    {
        StringBuilder result = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() : "");
        if (component instanceof Container) { for (Component child : ((Container) component).getComponents()) { result.append(labels(child)); } }
        return result.toString();
    }
    private void render(JPanel panel, String name)
    {
        panel.setSize(225, panel.getPreferredSize().height); layout(panel);
        BufferedImage image = new BufferedImage(225, panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
        File directory = new File("build/verification"); directory.mkdirs();
        try { ImageIO.write(image, "png", new File(directory, name)); } catch (Exception ex) { throw new AssertionError(ex); }
    }
    private void layout(Container parent)
    { parent.doLayout(); for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } } }
}
