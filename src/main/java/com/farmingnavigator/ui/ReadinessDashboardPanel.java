package com.farmingnavigator.ui;

import com.farmingnavigator.farming.state.ReadinessDashboard;
import com.farmingnavigator.routing.run.CombinedRunKind;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public final class ReadinessDashboardPanel extends JPanel
{
    public ReadinessDashboardPanel(ReadinessDashboard overview, Instant now, boolean expanded, Consumer<Boolean> expansionChanged)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Farming readiness", Color.WHITE));
        add(text("Selected accessible patches across crop families. Historical states may have changed; visit to confirm.", ColorScheme.LIGHT_GRAY_COLOR));
        for (ReadinessDashboard.Group group : ReadinessDashboard.Group.values())
        {
            if (overview.getCount(group) > 0)
            {
                add(text(group.getLabel() + ": " + overview.getCount(group),
                        group == ReadinessDashboard.Group.DISEASED || group == ReadinessDashboard.Group.DEAD
                                ? new Color(255, 190, 80) : ColorScheme.LIGHT_GRAY_COLOR));
            }
        }
        if (overview.getExcludedCount() > 0)
        {
            add(text("Excluded for access or missing data: " + overview.getExcludedCount(), ColorScheme.LIGHT_GRAY_COLOR));
        }
        if (overview.getEntries().isEmpty())
        {
            add(text("No selected accessible patches.", ColorScheme.LIGHT_GRAY_COLOR));
            return;
        }
        add(Box.createVerticalStrut(6));
        add(text("Additional possible readiness if healthy (earliest window; cumulative):", ColorScheme.LIGHT_GRAY_COLOR));
        for (int minutes : new int[]{5, 10, 20, 30, 60})
        {
            add(text("Within " + minutes + " min: " + overview.getUpcomingCount(Duration.ofMinutes(minutes)), ColorScheme.LIGHT_GRAY_COLOR));
        }
        overview.getNext().ifPresent(entry -> add(text("Next estimate: " + entry.getLabel() + " - " + window(entry, now), Color.WHITE)));
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setVisible(expanded);
        JButton toggle = new JButton(expanded ? "Hide readiness details" : "Show readiness details");
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> {
            boolean show = !details.isVisible();
            details.setVisible(show);
            toggle.setText(show ? "Hide readiness details" : "Show readiness details");
            expansionChanged.accept(show);
            revalidate();
            repaint();
        });
        add(Box.createVerticalStrut(6));
        add(toggle);
        add(details);
        details.add(Box.createVerticalStrut(6));
        details.add(text("Visit opportunities by run type", Color.WHITE));
        details.add(text("Across selected accessible crop families; check the individual preparation plan for crop, supplies and travel. Counts are not XP, profit or route rankings.", ColorScheme.LIGHT_GRAY_COLOR));
        for (CombinedRunKind kind : CombinedRunKind.values())
        {
            if (overview.getEntries(kind).isEmpty()) { continue; }
            details.add(Box.createVerticalStrut(6));
            details.add(text(kind + " patches: " + overview.getEntries(kind).size(), Color.WHITE));
            for (ReadinessDashboard.Group group : ReadinessDashboard.Group.values())
            {
                long count = overview.getCount(kind,group);
                if (count > 0) { details.add(text(group.getLabel() + ": " + count, ColorScheme.LIGHT_GRAY_COLOR)); }
            }
            details.add(text("Additional possible in 10 / 30 / 60 min: "
                    + overview.getUpcomingCount(kind,Duration.ofMinutes(10)) + " / "
                    + overview.getUpcomingCount(kind,Duration.ofMinutes(30)) + " / "
                    + overview.getUpcomingCount(kind,Duration.ofMinutes(60))
                    + " (earliest window, cumulative, if healthy).", ColorScheme.LIGHT_GRAY_COLOR));
            overview.getNext(kind).ifPresent(entry -> details.add(text("Next: " + entry.getLabel() + " - " + window(entry,now), ColorScheme.LIGHT_GRAY_COLOR)));
        }
        details.add(Box.createVerticalStrut(8));
        details.add(text("Individual patch evidence", Color.WHITE));
        for (ReadinessDashboard.Entry entry : overview.getEntries())
        {
            details.add(Box.createVerticalStrut(6));
            details.add(text(entry.getLabel() + ": " + entry.getGroup().getLabel(), Color.WHITE));
            if (!entry.getCrop().isEmpty()) { details.add(text(entry.getCrop(), ColorScheme.LIGHT_GRAY_COLOR)); }
            if (entry.getEarliest() != null) { details.add(text(window(entry, now), ColorScheme.LIGHT_GRAY_COLOR)); }
            if (entry.getObservedAt() != null)
            {
                long minutes = Math.max(0, Duration.between(entry.getObservedAt(), now).toMinutes());
                details.add(text("Observed " + minutes + " min ago.",
                        ColorScheme.LIGHT_GRAY_COLOR));
            }
        }
    }

    private String window(ReadinessDashboard.Entry entry, Instant now)
    {
        if (!now.isBefore(entry.getLatest())) { return "Expected ready if healthy; visit to confirm (estimate)."; }
        long earliest = Math.max(0, (Duration.between(now, entry.getEarliest()).getSeconds() + 59) / 60);
        long latest = Math.max(0, (Duration.between(now, entry.getLatest()).getSeconds() + 59) / 60);
        return "Possible readiness in " + earliest + "-" + latest + " min if healthy (estimate).";
    }
    private JLabel text(String value, Color color)
    {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(color);
        return label;
    }
}
