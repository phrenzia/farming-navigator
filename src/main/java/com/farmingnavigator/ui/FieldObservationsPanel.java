package com.farmingnavigator.ui;

import com.farmingnavigator.farming.field.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.EnumMap;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;

public final class FieldObservationsPanel extends JPanel
{
    private final Map<FieldPatchDefinition,FieldPatchObservation> valid=new EnumMap<>(FieldPatchDefinition.class);
    private final JPanel summary=new JPanel(),plots=new JPanel();
    private final JComboBox<String> families=new JComboBox<>();
    private final Instant now;
    public FieldObservationsPanel(Map<FieldPatchDefinition,FieldPatchObservation> observations,Instant now,
            boolean expanded,Consumer<Boolean> expansionChanged)
    { this(observations,now,expanded,expansionChanged,null,family -> {}); }
    public FieldObservationsPanel(Map<FieldPatchDefinition,FieldPatchObservation> observations,Instant now,
            boolean expanded,Consumer<Boolean> expansionChanged,FieldFamily initialFamily,Consumer<FieldFamily> familyChanged)
    {
        this.now=now;
        observations.forEach((patch,observation) -> {
            if (!observation.getObservedAt().isAfter(now) && (observation.getState().getCrop().getFamily()==null
                    || observation.getState().getCrop().getFamily()==patch.getFamily())) { valid.put(patch,observation); }
        });
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9,8,12,8)); setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        add(text("Field and special patch observations",Color.WHITE));
        setup(summary); add(summary);
        add(text("Observations only. Patch access, routes and protection are not checked here. Visit to confirm old states.",ColorScheme.LIGHT_GRAY_COLOR));
        JPanel details=new JPanel(); setup(details); details.setVisible(expanded);
        JButton toggle=new JButton(expanded?"Hide field observations":"Show field observations"); toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> { boolean show=!details.isVisible(); details.setVisible(show);
            toggle.setText(show?"Hide field observations":"Show field observations"); expansionChanged.accept(show); revalidate(); repaint(); });
        add(Box.createVerticalStrut(6)); add(toggle); add(details);
        families.addItem("All field families"); for(FieldFamily family:FieldFamily.values()) { families.addItem(family.getLabel()); }
        families.setSelectedIndex(initialFamily==null?0:initialFamily.ordinal()+1);
        families.setAlignmentX(Component.LEFT_ALIGNMENT); families.setMaximumSize(new Dimension(190,25)); details.add(families);
        setup(plots); details.add(plots);
        families.addActionListener(event -> { render(); familyChanged.accept(selectedFamily()); }); render();
    }
    private FieldFamily selectedFamily() { return families.getSelectedIndex()==0?null:FieldFamily.values()[families.getSelectedIndex()-1]; }
    private void render()
    {
        FieldFamily selected=selectedFamily(); summary.removeAll(); plots.removeAll();
        long total=java.util.Arrays.stream(FieldPatchDefinition.values()).filter(patch -> selected==null || patch.getFamily()==selected).count();
        long observed=valid.keySet().stream().filter(patch -> selected==null || patch.getFamily()==selected).count();
        long ready=valid.entrySet().stream().filter(entry -> selected==null || entry.getKey().getFamily()==selected)
                .filter(entry -> entry.getValue().getState().getCondition()==FieldPatchCondition.HARVESTABLE).count();
        long problem=valid.entrySet().stream().filter(entry -> selected==null || entry.getKey().getFamily()==selected)
                .filter(entry -> entry.getValue().getState().getCondition()==FieldPatchCondition.DISEASED || entry.getValue().getState().getCondition()==FieldPatchCondition.DEAD).count();
        summary.add(text("Observed patches: " + observed + " / " + total,ColorScheme.LIGHT_GRAY_COLOR));
        summary.add(text("Last observed harvestable: " + ready + "; diseased or dead: " + problem,ColorScheme.LIGHT_GRAY_COLOR));
        for(FieldFamily family:FieldFamily.values())
        {
            if(selected!=null && selected!=family) { continue; }
            plots.add(Box.createVerticalStrut(8)); plots.add(text(family.getLabel(),Color.WHITE));
            if(family==FieldFamily.GRAPES)
            {
                plots.add(text("Vineyard soil preparation, watering, entry and travel are unverified. No grape planting checklist is offered.",ColorScheme.LIGHT_GRAY_COLOR));
                plots.add(text("Fully grown vines may need a health check before harvesting. Growing codes remain separate from observed harvestability.",ColorScheme.LIGHT_GRAY_COLOR));
            }
            for(FieldPatchDefinition patch:FieldPatchDefinition.values())
            {
                if(patch.getFamily()!=family) { continue; }
                FieldPatchObservation observation=valid.get(patch); plots.add(Box.createVerticalStrut(6)); plots.add(text(patch.getLabel(),Color.WHITE));
                boolean problemState=observation!=null && (observation.getState().getCondition()==FieldPatchCondition.DISEASED || observation.getState().getCondition()==FieldPatchCondition.DEAD);
                plots.add(text(FieldObservationText.describe(observation,ZoneId.systemDefault()),problemState?new Color(255,190,80):ColorScheme.LIGHT_GRAY_COLOR));
                String timer=FieldObservationText.timer(observation,now); if(!timer.isEmpty()) { plots.add(text(timer,ColorScheme.LIGHT_GRAY_COLOR)); }
            }
        }
        revalidate(); repaint();
    }
    private void setup(JPanel panel) { panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS)); panel.setOpaque(false); panel.setAlignmentX(Component.LEFT_ALIGNMENT); }
    private JLabel text(String value,Color color)
    {
        String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        JLabel label=new JLabel("<html><div style='width:135px'>"+escaped+"</div></html>"); label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
