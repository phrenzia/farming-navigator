package com.farmingnavigator.ui;

import com.farmingnavigator.farming.tree.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.EnumMap;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public final class TreeObservationsPanel extends JPanel
{
    public TreeObservationsPanel(Map<TreePatchDefinition, TreePatchObservation> observations, Instant now,
            boolean expanded, Consumer<Boolean> expansionChanged)
    {
        Map<TreePatchDefinition, TreePatchObservation> valid = new EnumMap<>(TreePatchDefinition.class);
        observations.forEach((patch, observation) -> {
            if (!observation.getObservedAt().isAfter(now)) { valid.put(patch, observation); }
        });
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Tree observations", Color.WHITE));
        add(text("Observed patches: " + valid.size() + " / " + TreePatchDefinition.values().length, ColorScheme.LIGHT_GRAY_COLOR));
        long ready = valid.values().stream().filter(observation -> observation.getState().getCondition() == TreePatchCondition.CHECK_HEALTH).count();
        long problem = valid.values().stream().filter(observation -> observation.getState().getCondition() == TreePatchCondition.DISEASED
                || observation.getState().getCondition() == TreePatchCondition.DEAD).count();
        add(text("Last observed health-check ready: " + ready + "; diseased or dead: " + problem, ColorScheme.LIGHT_GRAY_COLOR));
        add(text("Observations only. Tree access, routes and protection are not checked here. Visit to confirm old states.", ColorScheme.LIGHT_GRAY_COLOR));
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setOpaque(false);
        details.setVisible(expanded);
        JButton toggle = new JButton(expanded ? "Hide tree observations" : "Show tree observations");
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> {
            boolean show = !details.isVisible();
            details.setVisible(show);
            toggle.setText(show ? "Hide tree observations" : "Show tree observations");
            expansionChanged.accept(show);
            revalidate();
            repaint();
        });
        add(Box.createVerticalStrut(6));
        add(toggle);
        add(details);
        for (TreeFamily family : TreeFamily.values())
        {
            details.add(Box.createVerticalStrut(8));
            details.add(text(family.getLabel(), Color.WHITE));
            for (TreePatchDefinition patch : TreePatchDefinition.values())
            {
                if (patch.getFamily() != family) { continue; }
                TreePatchObservation observation = valid.get(patch);
                details.add(Box.createVerticalStrut(6));
                details.add(text(patch.getLabel(), Color.WHITE));
                boolean problemState = observation != null && (observation.getState().getCondition() == TreePatchCondition.DISEASED
                        || observation.getState().getCondition() == TreePatchCondition.DEAD);
                details.add(text(TreeObservationText.describe(observation, ZoneId.systemDefault()),
                        problemState ? new Color(255, 190, 80) : ColorScheme.LIGHT_GRAY_COLOR));
                String timer = TreeObservationText.timer(observation, now);
                if (!timer.isEmpty()) { details.add(text(timer, ColorScheme.LIGHT_GRAY_COLOR)); }
            }
        }
    }

    private JLabel text(String value, Color color)
    {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
