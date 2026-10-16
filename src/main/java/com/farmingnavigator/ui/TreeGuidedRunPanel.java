package com.farmingnavigator.ui;

import com.farmingnavigator.guided.tree.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public final class TreeGuidedRunPanel extends JPanel
{
    public TreeGuidedRunPanel(TreeRunSnapshot run, boolean canStart, Consumer<TreeRunCommand> commands)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Guided tree run", Color.WHITE));
        add(text(run == null ? "Prepare a tree run below, then start guidance." : run.getInstruction(), ColorScheme.LIGHT_GRAY_COLOR));
        if (run != null && run.getStep() != TreeRunStep.INACTIVE)
        {
            add(text("Elapsed: " + run.getElapsed().toMinutes() + " min, including preparation and waiting.", ColorScheme.LIGHT_GRAY_COLOR));
        }
        if (run == null || !run.isActive())
        {
            JButton start = button("Start tree guidance", TreeRunCommand.START, commands);
            start.setEnabled(canStart); add(start); return;
        }
        add(text(run.getCrop() + " - " + run.getStep().toString().replace('_', ' ').toLowerCase(java.util.Locale.ROOT), Color.WHITE));
        add(text("Completed: " + run.getCompleted() + " / " + run.getTotal() + "; skipped: " + run.getSkipped()
                + "; manual completions: " + run.getManual(), ColorScheme.LIGHT_GRAY_COLOR));
        if (run.getStop() != null)
        {
            add(text(run.getStop().getPatch().getLabel(), Color.WHITE));
            if (run.getStep() == TreeRunStep.TRAVEL)
            {
                if (run.getStop().isLocalWalk()) { add(text("Walk to the next nearby hardwood patch.", ColorScheme.LIGHT_GRAY_COLOR)); }
                else if (run.getStop().getRecommended() != null)
                {
                    add(text(run.getStop().getRecommended().getOption().getDisplayName(), ColorScheme.LIGHT_GRAY_COLOR));
                    add(text(run.getStop().getRecommended().getOption().getInstructions(), ColorScheme.LIGHT_GRAY_COLOR));
                }
                else { add(text("No verified transport is usable. Review travel supplies or travel manually.", new Color(255, 190, 80))); }
            }
        }
        if (run.getStep() == TreeRunStep.PREPARE) { add(button("Ready for tree travel", TreeRunCommand.CONFIRM, commands)); }
        else
        {
            if (run.getStep() == TreeRunStep.COMPOST) { add(button("Compost applied (manual)", TreeRunCommand.CONFIRM, commands)); }
            if (run.getStep() == TreeRunStep.PROTECT) { add(button("Protection paid (manual)", TreeRunCommand.CONFIRM, commands)); }
            if (run.getStep() == TreeRunStep.KEEP_OR_REMOVE)
            {
                add(button("Keep tree / finish stop", TreeRunCommand.KEEP_TREE, commands));
                add(button(run.getStop().isReplant() ? "Guide removal and replant" : "Guide removal only",
                        TreeRunCommand.REMOVE_TREE, commands));
            }
            add(button("Complete tree stop manually", TreeRunCommand.MARK_COMPLETE, commands));
            add(button("Skip tree stop", TreeRunCommand.SKIP, commands));
        }
        add(button("Stop tree guidance", TreeRunCommand.CANCEL, commands));
    }
    private JButton button(String title, TreeRunCommand command, Consumer<TreeRunCommand> commands)
    {
        JButton button = new JButton(title); button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> { button.setEnabled(false); commands.accept(command); }); return button;
    }
    private JLabel text(String value, Color color)
    {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
