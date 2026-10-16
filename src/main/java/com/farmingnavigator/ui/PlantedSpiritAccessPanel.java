package com.farmingnavigator.ui;

import com.farmingnavigator.routing.spirit.PlantedSpiritTree;
import com.farmingnavigator.routing.spirit.SpiritTreeDeclarations;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;

/** These controls update a local checklist declaration, never a game action or a persistent unlock. */
public final class PlantedSpiritAccessPanel extends JPanel
{
    public PlantedSpiritAccessPanel(SpiritTreeDeclarations.Snapshot snapshot,boolean expanded,Consumer<Boolean> expansionChanged,
            Consumer<Set<PlantedSpiritTree>> apply)
    {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9,8,12,8)); setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        add(text("Planted spirit-tree access",Color.WHITE));
        add(text(snapshot.getTrees().size()+(snapshot.getTrees().size()==1?" tree declared":" trees declared")+" currently usable for this login session.",ColorScheme.LIGHT_GRAY_COLOR));
        JPanel details=new JPanel(); details.setOpaque(false); details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS)); details.setVisible(expanded);
        JButton toggle=new JButton(expanded?"Hide tree access":"Declare tree access"); toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> { boolean show=!details.isVisible(); details.setVisible(show);
            toggle.setText(show?"Hide tree access":"Declare tree access"); expansionChanged.accept(show); revalidate(); repaint(); }); add(toggle); add(details);
        details.add(text("Select only planted trees you can use now. Apply these declarations to show arrival alternatives via the Grand Exchange. Quest, ring charges, bank stock and patch access are checked separately.",ColorScheme.LIGHT_GRAY_COLOR));
        details.add(text("Declarations clear on logout, world hop, character/mode change or disabling this feature. Historical growth never enables access. Applying changed declarations stops current guidance so you can review the new plan.",ColorScheme.LIGHT_GRAY_COLOR));
        boolean available=snapshot.getProfile()!=null; Map<PlantedSpiritTree,JCheckBox> choices=new EnumMap<>(PlantedSpiritTree.class);
        for(PlantedSpiritTree tree:PlantedSpiritTree.values())
        {
            JCheckBox box=new JCheckBox(tree.toString(),snapshot.getTrees().contains(tree)); box.setOpaque(false); box.setForeground(Color.WHITE);
            box.setEnabled(available); box.setAlignmentX(Component.LEFT_ALIGNMENT); choices.put(tree,box); details.add(box);
        }
        if(!available) { details.add(text("Character profile data required before declaring access.",ColorScheme.LIGHT_GRAY_COLOR)); }
        JButton button=new JButton("Apply access declarations"); button.setAlignmentX(Component.LEFT_ALIGNMENT); button.setEnabled(available);
        button.addActionListener(event -> { button.setEnabled(false); Set<PlantedSpiritTree> selected=EnumSet.noneOf(PlantedSpiritTree.class);
            choices.forEach((tree,box) -> { if(box.isSelected()) { selected.add(tree); } }); apply.accept(Set.copyOf(selected)); }); details.add(button);
    }
    private JLabel text(String value,Color color)
    {
        JLabel label=new JLabel("<html><div style='width:135px'>"+value+"</div></html>"); label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
