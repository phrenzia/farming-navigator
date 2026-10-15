package com.farmingnavigator.ui;

import com.farmingnavigator.farming.state.HerbOverview;
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

public final class HerbOverviewPanel extends JPanel
{
    public HerbOverviewPanel(HerbOverview overview, Instant now, boolean expanded, Consumer<Boolean> expansionChanged)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Herb overview", Color.WHITE));
        add(text("Selected unlocked patches; visit to confirm. Last observations may have changed.", ColorScheme.LIGHT_GRAY_COLOR));
        for (HerbOverview.Group group : HerbOverview.Group.values())
        {
            if (overview.getCount(group) > 0)
            {
                add(text(group.getLabel() + ": " + overview.getCount(group),
                        group == HerbOverview.Group.DISEASED || group == HerbOverview.Group.DEAD
                                ? new Color(255, 190, 80) : ColorScheme.LIGHT_GRAY_COLOR));
            }
        }
        if (overview.getLockedCount() > 0)
        {
            add(text("Selected locked patches excluded: " + overview.getLockedCount(), ColorScheme.LIGHT_GRAY_COLOR));
        }
        if (overview.getEntries().isEmpty())
        {
            add(text("No selected unlocked herb patches.", ColorScheme.LIGHT_GRAY_COLOR));
            return;
        }
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setVisible(expanded);
        JButton toggle = new JButton(expanded ? "Hide patch summary" : "Show patch summary");
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> {
            boolean show = !details.isVisible();
            details.setVisible(show);
            toggle.setText(show ? "Hide patch summary" : "Show patch summary");
            expansionChanged.accept(show);
            revalidate();
            repaint();
        });
        add(Box.createVerticalStrut(6));
        add(toggle);
        add(details);
        for (HerbOverview.Entry entry : overview.getEntries())
        {
            details.add(Box.createVerticalStrut(6));
            details.add(text(entry.getPatch().getDisplayName() + ": " + entry.getGroup().getLabel(), Color.WHITE));
            if (entry.getObservation() != null)
            {
                long minutes = Math.max(0, Duration.between(entry.getObservation().getObservedAt(), now).toMinutes());
                details.add(text("Observed " + minutes + " min ago. " + HerbGrowthTimerText.describe(entry.getObservation(), now),
                        ColorScheme.LIGHT_GRAY_COLOR));
            }
        }
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
