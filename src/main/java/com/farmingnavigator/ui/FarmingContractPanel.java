package com.farmingnavigator.ui;

import com.farmingnavigator.contracts.*;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.TreeRunCrop;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public final class FarmingContractPanel extends JPanel
{
    public FarmingContractPanel(FarmingContractSnapshot contract, HerbRunCrop herb, TreeRunCrop tree,
            boolean herbGuildSelected, boolean treeGuildSelected)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8)); setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Farming contract", Color.WHITE));
        switch (contract.getState())
        {
            case ACTIVE:
                ContractCrop crop = contract.getCrop();
                add(text(crop.getLabel() + " - " + crop.getFamily().getLabel(), Color.WHITE));
                add(text("Current assignment reported by the game. Grow the assigned crop in the Farming Guild; confirm progress and turn-in with Guildmaster Jane.", ColorScheme.LIGHT_GRAY_COLOR));
                boolean herbConflict = herbGuildSelected && crop.getHerbRunCrop() != null && crop.getHerbRunCrop() != herb;
                boolean treeConflict = treeGuildSelected && crop.getTreeRunCrop() != null
                        && crop.getTreeRunCrop().getCrop().getFamily() == tree.getCrop().getFamily() && crop.getTreeRunCrop() != tree;
                if (herbConflict || treeConflict)
                {
                    add(text("Your selected guild run crop differs from this contract. Check before clearing or replanting the guild patch.", new Color(255, 190, 80)));
                }
                if (crop.getHerbRunCrop() == null && crop.getTreeRunCrop() == null)
                {
                    add(text("Dedicated run planning for this contract family is not available yet.", ColorScheme.LIGHT_GRAY_COLOR));
                }
                break;
            case REPORTED_COMPLETE:
                add(text("Contract completion reported. Talk to Guildmaster Jane to confirm your reward or next assignment. Seed-pack ownership is not checked.", ColorScheme.LIGHT_GRAY_COLOR)); break;
            case NONE:
                add(text("No active assignment reported. Talk to Guildmaster Jane to request a contract.", ColorScheme.LIGHT_GRAY_COLOR)); break;
            case LOGGED_OUT:
                add(text("Log in to read your current contract.", ColorScheme.LIGHT_GRAY_COLOR)); break;
            default:
                add(text("Data required: assignment not recognized. Talk to Guildmaster Jane and refresh.", ColorScheme.LIGHT_GRAY_COLOR)); break;
        }
        add(text("Contract difficulty, rewards and harvest completion are not predicted here.", ColorScheme.LIGHT_GRAY_COLOR));
    }
    private JLabel text(String value, Color color)
    {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
