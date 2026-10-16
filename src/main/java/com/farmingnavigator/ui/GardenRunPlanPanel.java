package com.farmingnavigator.ui;

import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.routing.run.GardenRunPlan;
import com.farmingnavigator.routing.run.GardenRunStop;
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

/** Bush/cactus preparation stays usable before opening a bank; unknown bank availability is explicit. */
public final class GardenRunPlanPanel extends JPanel
{
    private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
    private static final Color WARNING = new Color(255, 190, 80);
    public GardenRunPlanPanel(GardenRunPlan plan, boolean expanded, Consumer<Boolean> expansionChanged)
    {
        this(plan, expanded, expansionChanged, null);
    }
    public GardenRunPlanPanel(GardenRunPlan plan, boolean expanded, Consumer<Boolean> expansionChanged,
            com.farmingnavigator.economics.FarmingMaterialEstimate materials)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Bush/cactus run preparation", Color.WHITE));
        add(text(plan.getCrop() + " - " + plan.getStops().size() + " stops", Color.WHITE));
        add(text(plan.getMode().toString(), MUTED));
        add(text("Compost: " + plan.getCompost(), MUTED));
        add(text("Choose bush/cactus crop, patches, keep/replace mode, compost and protection in plugin settings.", MUTED));
        if (!plan.getBlocker().isEmpty()) { add(text(plan.getBlocker(), WARNING)); return; }
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setVisible(expanded);
        JButton toggle = new JButton(expanded ? "Hide bush/cactus preparation" : "Show bush/cactus preparation");
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> {
            boolean show = !details.isVisible();
            details.setVisible(show);
            toggle.setText(show ? "Hide bush/cactus preparation" : "Show bush/cactus preparation");
            expansionChanged.accept(show);
            revalidate(); repaint();
        });
        add(Box.createVerticalStrut(6)); add(toggle); add(details);
        for (String warning : plan.getWarnings()) { details.add(text(warning, WARNING)); }
        if (plan.getStops().isEmpty()) { details.add(text("No selected patches need a stop right now.", MUTED)); }
        int index = 1;
        for (GardenRunStop stop : plan.getStops())
        {
            details.add(Box.createVerticalStrut(7));
            details.add(text(index++ + ". " + stop.getPatch().getLabel(), Color.WHITE));
            if (stop.getRecommended() == null)
            {
                details.add(text("No verified transport selected. Travel manually and check your route.", WARNING));
            }
            else
            {
                details.add(text("Travel: " + stop.getRecommended().getOption().getDisplayName() + " - "
                        + stop.getRecommended().getState().getDisplayName(), MUTED));
                details.add(text(stop.getRecommended().getOption().getInstructions(), MUTED));
            }
            if (stop.getFallback() != null)
            {
                details.add(text("Fallback: " + stop.getFallback().getOption().getDisplayName()
                        + " - " + stop.getFallback().getState().getDisplayName(), MUTED));
            }
            details.add(text(stop.getAction(), MUTED));
        }
        if (plan.getTransportResources() != null)
        {
            details.add(Box.createVerticalStrut(7)); details.add(text("Shared bush/cactus travel supplies", Color.WHITE));
            plan.getTransportResources().getChecks().forEach(check -> {
                details.add(text(check.getRequirement() + ": " + (check.isReadyNow() ? "Ready now"
                        : check.requiresBankWithdrawal() ? "Available from bank"
                        : check.getState().toString().replace('_', ' ').toLowerCase(java.util.Locale.ROOT)),
                        check.isReadyNow() ? MUTED : WARNING));
                details.add(text(check.getDetail(), MUTED));
            });
            plan.getTransportResources().getConsumables().forEach(check -> {
                details.add(text(check.getRequirement().getQuantity() + " x " + check.getRequirement().getName(), Color.WHITE));
                details.add(text(supplyState(check), MUTED));
            });
            plan.getTransportResources().getNotes().forEach(note -> details.add(text(note, MUTED)));
            details.add(text("Herb, tree and bush/cactus plans are separate runs; combining them requires additional shared supplies.", MUTED));
        }
        if (materials != null) { details.add(new FarmingMaterialsPanel(materials)); }
        if (!plan.getSupplies().isEmpty())
        {
            details.add(Box.createVerticalStrut(7)); details.add(text("Farming supplies", Color.WHITE));
            for (SupplyCheck check : plan.getSupplies())
            {
                details.add(text(check.getRequirement().getQuantity() + " x " + check.getRequirement().getName()
                        + (check.getRequirement().isOptional() ? " (optional)" : ""), Color.WHITE));
                details.add(text(supplyState(check), check.getState() == SupplyState.MISSING
                        || check.getState() == SupplyState.DATA_REQUIRED ? WARNING : MUTED));
                details.add(text(check.getRequirement().getNote(), MUTED));
            }
        }
        if (!plan.getExcluded().isEmpty())
        {
            details.add(Box.createVerticalStrut(7)); details.add(text("Excluded patches", Color.WHITE));
            plan.getExcluded().forEach((patch, reason) -> details.add(text(patch.getLabel() + ": " + reason, MUTED)));
        }
    }
    static String supplyState(SupplyCheck check)
    {
        switch (check.getState())
        {
            case ALREADY_CARRIED: return "Already carried";
            case ALREADY_EQUIPPED: return "Already equipped";
            case AVAILABLE_FROM_BANK: return "Withdraw " + check.getWithdrawQuantity() + " from verified bank";
            case MISSING: return "Withdraw " + check.getWithdrawQuantity() + "; missing " + check.getMissingQuantity();
            default: return "Data required: open the bank to check the remaining quantity";
        }
    }
    private JLabel text(String value, Color color)
    {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
