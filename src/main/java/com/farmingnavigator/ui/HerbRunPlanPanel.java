package com.farmingnavigator.ui;

import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbRunStop;
import com.farmingnavigator.routing.run.RunTransportResources;
import com.farmingnavigator.routing.requirement.RequirementResult;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** Displays a suggested circuit and supply list without owning planning decisions. */
public final class HerbRunPlanPanel extends JPanel
{
    private static final Color MUTED = new Color(180, 180, 180);
    private static final Color WARNING = new Color(255, 190, 80);

    public HerbRunPlanPanel(HerbRunPlan plan, boolean expanded, Consumer<Boolean> expansionChanged)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Suggested herb run", Color.WHITE));
        add(Box.createVerticalStrut(5));
        add(text(plan.getCrop() + " - " + plan.getStops().size() + " stops", Color.WHITE));
        add(text("Choose crop, patches and compost in plugin settings.", MUTED));
        if (!plan.getBlocker().isEmpty())
        {
            add(text(plan.getBlocker(), WARNING));
            return;
        }
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setVisible(expanded);
        JButton toggle = new JButton(expanded ? "Hide route and supplies" : "Show route and supplies");
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> {
            boolean show = !details.isVisible();
            details.setVisible(show);
            toggle.setText(show ? "Hide route and supplies" : "Show route and supplies");
            expansionChanged.accept(show);
            revalidate();
            repaint();
        });
        add(Box.createVerticalStrut(7));
        add(toggle);
        add(details);
        details.add(text("Suggested order; travel times are not optimized. Check travel supplies for the whole run.", MUTED));
        if (plan.getStops().isEmpty())
        {
            details.add(text("No selected patches need a stop right now.", MUTED));
        }
        int index = 1;
        for (HerbRunStop stop : plan.getStops())
        {
            details.add(Box.createVerticalStrut(7));
            details.add(text(index++ + ". " + stop.getPatch().getDisplayName(), Color.WHITE));
            if (stop.getRecommended() == null)
            {
                details.add(text("No verified transport available. Review the patch's transport options or travel manually.", WARNING));
            }
            else
            {
                details.add(text("Travel: " + stop.getRecommended().getOption().getDisplayName()
                        + " - " + stop.getRecommended().getState().getDisplayName(), MUTED));
                details.add(text(stop.getRecommended().getOption().getInstructions(), MUTED));
            }
            if (stop.getFallback() != null)
            {
                details.add(text("Fallback: " + stop.getFallback().getOption().getDisplayName()
                        + " - " + stop.getFallback().getState().getDisplayName(), MUTED));
            }
            details.add(text(stop.getAction(), Color.WHITE));
        }
        if (!plan.getExcluded().isEmpty())
        {
            details.add(Box.createVerticalStrut(7));
            details.add(text("Excluded patches", Color.WHITE));
            plan.getExcluded().forEach((patch, reason) -> details.add(text(patch.getDisplayName() + ": " + reason, MUTED)));
        }
        RunTransportResources resources = plan.getTransportResources();
        if (plan.getCareResources() != null)
        {
            details.add(Box.createVerticalStrut(7));
            details.add(text("Disease treatment: " + plan.getCure(), Color.WHITE));
            details.add(text("Supplies cover observed diseased stops. Unexpected disease may need extra supplies.", MUTED));
            for (RequirementResult check : plan.getCareResources().getChecks())
            {
                details.add(text(check.getRequirement() + ": " + check.getDetail(), check.isSatisfied() ? MUTED : WARNING));
            }
        }
        if (resources != null)
        {
            details.add(Box.createVerticalStrut(7));
            details.add(text("Shared travel supplies", Color.WHITE));
            for (RequirementResult check : resources.getChecks())
            {
                details.add(text(check.getRequirement(), Color.WHITE));
                details.add(text(check.getDetail(), check.isSatisfied() ? MUTED : WARNING));
            }
            for (String note : resources.getNotes())
            {
                details.add(text(note, WARNING));
            }
            for (SupplyCheck check : resources.getConsumables())
            {
                details.add(text(LoadoutText.describe(check),
                        check.getState() == SupplyState.MISSING || check.getState() == SupplyState.DATA_REQUIRED ? WARNING : MUTED));
            }
            if (resources.getChecks().isEmpty() && resources.getNotes().isEmpty() && resources.getConsumables().isEmpty())
            {
                details.add(text("No direct-spell, charged-jewellery or single-use teleport supplies are needed by these legs.", MUTED));
            }
        }
        if (!plan.getSupplies().isEmpty())
        {
            details.add(Box.createVerticalStrut(7));
            details.add(text("Suggested farming supplies", Color.WHITE));
            details.add(text("This list checks carried items and the verified bank. Check transport supplies and leprechaun storage too.", MUTED));
            for (SupplyCheck check : plan.getSupplies())
            {
                details.add(Box.createVerticalStrut(5));
                details.add(text(LoadoutText.describe(check),
                        check.getState() == SupplyState.MISSING && !check.getRequirement().isOptional() ? WARNING : MUTED));
                details.add(text(check.getRequirement().getNote(), MUTED));
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
