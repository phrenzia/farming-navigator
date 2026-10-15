package com.farmingnavigator.ui;

import com.farmingnavigator.economics.FarmingMaterialEstimate;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public final class FarmingMaterialsPanel extends JPanel
{
    public FarmingMaterialsPanel(FarmingMaterialEstimate estimate)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        NumberFormat numbers = NumberFormat.getIntegerInstance();
        add(Box.createVerticalStrut(7)); add(text("Potential materials value", Color.WHITE));
        if (estimate.getLines().isEmpty()) { add(text("No farming materials reserved for potential replants or treatment.", ColorScheme.LIGHT_GRAY_COLOR)); }
        else
        {
            for (FarmingMaterialEstimate.Line line : estimate.getLines())
            {
                add(text(line.getQuantity() + " x " + line.getName() + ": "
                        + (line.getValue().isPresent() ? numbers.format(line.getValue().getAsLong()) + " gp" : "price unavailable"), ColorScheme.LIGHT_GRAY_COLOR));
            }
            add(text(estimate.getKnownSubtotal().isPresent() ? "Known subtotal: " + numbers.format(estimate.getKnownSubtotal().getAsLong()) + " gp"
                    : "Subtotal unavailable: value exceeds supported range.", Color.WHITE));
            if (estimate.getUnknownPriceCount() > 0) { add(text("Unpriced material lines: " + estimate.getUnknownPriceCount() + ". Their value is excluded.", new Color(255, 190, 80))); }
        }
        estimate.getNotes().forEach(note -> add(text(note, ColorScheme.LIGHT_GRAY_COLOR)));
    }
    private JLabel text(String value, Color color)
    {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
