package com.farmingnavigator.ui;

import com.farmingnavigator.routing.run.CombinedRunPlan;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyState;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;

public final class CombinedRunPlanPanel extends JPanel
{
    public CombinedRunPlanPanel(CombinedRunPlan plan, boolean expanded, Consumer<Boolean> expansionChanged)
    { this(plan,expanded,expansionChanged,null); }
    public CombinedRunPlanPanel(CombinedRunPlan plan,boolean expanded,Consumer<Boolean> expansionChanged,
            com.farmingnavigator.economics.FarmingMaterialEstimate materials)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9,8,12,8)); setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        add(text("Combined preparation", Color.WHITE));
        for(String component:plan.getComponents()) { add(text(component,ColorScheme.LIGHT_GRAY_COLOR)); }
        if(!plan.getBlockers().isEmpty())
        {
            plan.getBlockers().forEach(blocker -> add(text(blocker,new Color(255,190,80)))); return;
        }
        add(text(plan.getStops().size() + " total stops. Follow component checklists one at a time; this is a shared preparation worksheet.",ColorScheme.LIGHT_GRAY_COLOR));
        JPanel details=new JPanel(); details.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS)); details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT); details.setVisible(expanded);
        JButton toggle=new JButton(expanded?"Hide combined supplies":"Show combined supplies"); toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> {
            boolean show=!details.isVisible(); details.setVisible(show); toggle.setText(show?"Hide combined supplies":"Show combined supplies");
            expansionChanged.accept(show); revalidate(); repaint();
        });
        add(Box.createVerticalStrut(6)); add(toggle); add(details);
        details.add(Box.createVerticalStrut(7)); details.add(text("Shared farming supplies",Color.WHITE));
        plan.getSupplies().forEach(check -> {
            details.add(text(check.getRequirement().getQuantity() + " x " + check.getRequirement().getName()
                    + (check.getRequirement().isOptional()?" (optional)":""),Color.WHITE));
            details.add(text(supplyState(check),ColorScheme.LIGHT_GRAY_COLOR));
            details.add(text(check.getRequirement().getNote(),ColorScheme.LIGHT_GRAY_COLOR));
        });
        if (!plan.getCareChecks().isEmpty()) { details.add(Box.createVerticalStrut(7)); details.add(text("Care access and declarations",Color.WHITE)); }
        plan.getCareChecks().forEach(check -> details.add(text(check.getRequirement() + ": " + check.getDetail(),check.isReadyNow()?ColorScheme.LIGHT_GRAY_COLOR:new Color(255,190,80))));
        if(plan.getResources()!=null)
        {
            details.add(Box.createVerticalStrut(7)); details.add(text("Shared travel and spell budget",Color.WHITE));
            plan.getResources().getChecks().forEach(check -> details.add(text(check.getRequirement() + ": " + check.getDetail(),
                    check.isReadyNow()?ColorScheme.LIGHT_GRAY_COLOR:new Color(255,190,80))));
            plan.getResources().getConsumables().forEach(check -> details.add(text(check.getRequirement().getQuantity() + " x " + check.getRequirement().getName()
                    + "; " + supplyState(check),ColorScheme.LIGHT_GRAY_COLOR)));
            plan.getResources().getNotes().forEach(note -> details.add(text(note,ColorScheme.LIGHT_GRAY_COLOR)));
        }
        if(materials!=null) { details.add(new FarmingMaterialsPanel(materials)); }
        details.add(Box.createVerticalStrut(7)); details.add(text("Planning notes",Color.WHITE));
        plan.getWarnings().forEach(warning -> details.add(text(warning,ColorScheme.LIGHT_GRAY_COLOR)));
    }
    private String supplyState(SupplyCheck check)
    {
        if(check.getState()==SupplyState.DATA_REQUIRED) { return "Availability unknown; verify your bank and carried items."; }
        return check.getState().toString().replace('_',' ').toLowerCase(java.util.Locale.ROOT)
                + "; withdraw " + check.getWithdrawQuantity() + "; missing " + check.getMissingQuantity();
    }
    private JLabel text(String value,Color color)
    {
        String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        JLabel label=new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT); label.setForeground(color); return label;
    }
}
