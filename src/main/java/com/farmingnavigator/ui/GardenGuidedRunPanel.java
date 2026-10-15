package com.farmingnavigator.ui;

import com.farmingnavigator.guided.garden.*;
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

public final class GardenGuidedRunPanel extends JPanel
{
    public GardenGuidedRunPanel(GardenRunSnapshot run, boolean canStart, Consumer<GardenRunCommand> commands)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Guided bush/cactus run", Color.WHITE));
        add(text(run == null ? "Prepare a bush/cactus run below, then start guidance." : run.getInstruction(), ColorScheme.LIGHT_GRAY_COLOR));
        if (run != null && run.getStep() != GardenRunStep.INACTIVE)
        {
            add(text("Elapsed: " + run.getElapsed().toMinutes() + " min, including preparation and waiting.", ColorScheme.LIGHT_GRAY_COLOR));
        }
        if (run == null || !run.isActive())
        {
            JButton start = button("Start bush/cactus guidance", GardenRunCommand.START, commands);
            start.setEnabled(canStart); add(start); return;
        }
        add(text(run.getCrop() + " - " + run.getStep().toString().replace('_', ' ').toLowerCase(java.util.Locale.ROOT), Color.WHITE));
        add(text("Completed: " + run.getCompleted() + " / " + run.getTotal() + "; skipped: " + run.getSkipped()
                + "; manual completions: " + run.getManual(), ColorScheme.LIGHT_GRAY_COLOR));
        if (run.getStop() != null)
        {
            add(text(run.getStop().getPatch().getLabel(), Color.WHITE));
            if (run.getStep() == GardenRunStep.TRAVEL)
            {
                if (run.getStop().getRecommended() != null)
                {
                    add(text(run.getStop().getRecommended().getOption().getDisplayName(), ColorScheme.LIGHT_GRAY_COLOR));
                    add(text(run.getStop().getRecommended().getOption().getInstructions(), ColorScheme.LIGHT_GRAY_COLOR));
                }
                else { add(text("No verified transport is usable. Review travel supplies or travel manually.", new Color(255, 190, 80))); }
            }
        }
        if (run.getStep() == GardenRunStep.PREPARE) { add(button("Ready for travel", GardenRunCommand.CONFIRM, commands)); }
        else
        {
            if (run.getStep() == GardenRunStep.PROTECT) { add(button("Protection paid (manual)", GardenRunCommand.CONFIRM, commands)); }
            if (run.getStep() == GardenRunStep.HARVEST) { add(button("Produce checked (manual)", GardenRunCommand.CONFIRM, commands)); }
            if (run.getStep() == GardenRunStep.KEEP_OR_REPLACE)
            {
                add(button("Keep plant / finish stop", GardenRunCommand.KEEP_PLANT, commands));
                if (run.getStop().isReplant()) { add(button("Guide clearing and replant", GardenRunCommand.REPLACE_PLANT, commands)); }
            }
            add(button("Complete stop manually", GardenRunCommand.MARK_COMPLETE, commands));
            add(button("Skip stop", GardenRunCommand.SKIP, commands));
        }
        add(button("Stop bush/cactus guidance", GardenRunCommand.CANCEL, commands));
    }
    private JButton button(String title, GardenRunCommand command, Consumer<GardenRunCommand> commands)
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
