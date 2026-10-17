package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.RunMetricsSnapshot;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public final class RunMetricsPanel extends JPanel
{
    public RunMetricsPanel(RunMetricsSnapshot metrics)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8)); setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.getDefault());
        add(text("Run statistics", Color.WHITE));
        add(text(metrics.getRunName() + " - " + (metrics.isRecording() ? "recording" : metrics.isCompleted() ? "completed" : "stopped"), Color.WHITE));
        add(text("Elapsed: " + metrics.getElapsed().toMinutes() + " min (includes preparation and waiting).", ColorScheme.LIGHT_GRAY_COLOR));
        if (metrics.getContext().isKnown())
        {
            add(text(metrics.getStopOutcomes().describe(metrics.getContext()), ColorScheme.LIGHT_GRAY_COLOR));
            add(text("Stop counts describe checklist decisions, not harvest quantity or verified farming actions.", ColorScheme.LIGHT_GRAY_COLOR));
        }
        add(text(metrics.isXpKnown() ? "Recorded Farming XP: " + numbers.format(metrics.getXpGained())
                : "Farming XP: data required; the XP baseline was missing or changed unexpectedly.", ColorScheme.LIGHT_GRAY_COLOR));
        add(text(metrics.getXpPerHour().isPresent() ? "Recorded XP/hour: " + numbers.format(Math.round(metrics.getXpPerHour().getAsDouble()))
                : "Recorded XP/hour: unavailable until time and XP data are known.", ColorScheme.LIGHT_GRAY_COLOR));
        add(text("Includes all Farming XP gained while this checklist was active, including other Farming actions. No future harvest XP is included.", ColorScheme.LIGHT_GRAY_COLOR));
    }
    private JLabel text(String value, Color color)
    {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
