package com.farmingnavigator.ui;

import com.farmingnavigator.guided.GuidedRunCommand;
import com.farmingnavigator.guided.GuidedRunSnapshot;
import com.farmingnavigator.guided.GuidedRunStep;
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

/** All controls change the checklist only; they never send a game interaction. */
public final class GuidedRunPanel extends JPanel
{
    public GuidedRunPanel(GuidedRunSnapshot session, boolean canStart, Consumer<GuidedRunCommand> commands)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        add(text("Guided herb run", Color.WHITE));
        if (session == null || !session.isActive())
        {
            if (session != null)
            {
                add(text(session.getInstruction(), ColorScheme.LIGHT_GRAY_COLOR));
                if (session.getStep() == GuidedRunStep.COMPLETE)
                {
                    add(text(elapsedText(session), ColorScheme.LIGHT_GRAY_COLOR));
                }
            }
            JButton start = button("Start guided herb run", GuidedRunCommand.START, commands);
            start.setEnabled(canStart);
            add(start);
            return;
        }
        add(text(session.getCrop().getDisplayName() + ": " + session.getCompleted() + " completed, "
                + session.getSkipped() + " skipped / " + session.getTotal(), ColorScheme.LIGHT_GRAY_COLOR));
        add(text(elapsedText(session), ColorScheme.LIGHT_GRAY_COLOR));
        add(Box.createVerticalStrut(5));
        add(text(session.getInstruction(), Color.WHITE));
        if (session.getStop() != null && session.getStep() != GuidedRunStep.PREPARE)
        {
            add(text("Destination: " + session.getStop().getPatch().getDisplayName(), Color.WHITE));
            if (session.getStep() == GuidedRunStep.TRAVEL)
            {
                if (session.getStop().getRecommended() != null)
                {
                    add(text(session.getStop().getRecommended().getOption().getDisplayName() + " - "
                            + session.getStop().getRecommended().getState().getDisplayName(), ColorScheme.LIGHT_GRAY_COLOR));
                    add(text(session.getStop().getRecommended().getOption().getInstructions(), ColorScheme.LIGHT_GRAY_COLOR));
                }
                else
                {
                    add(text("No verified transport is usable. Review alternatives or travel manually.", new Color(255, 190, 80)));
                }
                if (session.getStop().getFallback() != null)
                {
                    add(text("Fallback: " + session.getStop().getFallback().getOption().getDisplayName(),
                            ColorScheme.LIGHT_GRAY_COLOR));
                }
            }
        }
        if (session.getStep() == GuidedRunStep.PREPARE)
        {
            add(button("Ready to travel", GuidedRunCommand.CONFIRM, commands));
        }
        else
        {
            if (session.getStep() == GuidedRunStep.COMPOST)
            {
                add(button("Compost step confirmed", GuidedRunCommand.CONFIRM, commands));
            }
            add(button("Complete stop manually", GuidedRunCommand.MARK_COMPLETE, commands));
            add(button("Skip this stop", GuidedRunCommand.SKIP, commands));
        }
        add(button("Stop guided run", GuidedRunCommand.CANCEL, commands));
    }

    private String elapsedText(GuidedRunSnapshot session)
    {
        return "Elapsed: " + session.getElapsed().toMinutes() + " min (includes preparation and waiting).";
    }

    private JButton button(String label, GuidedRunCommand command, Consumer<GuidedRunCommand> commands)
    {
        JButton button = new JButton(label);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> {
            button.setEnabled(false);
            commands.accept(command);
        });
        return button;
    }

    private JLabel text(String value, Color color)
    {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel("<html><div style='width:135px'>" + escaped + "</div></html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(color);
        return label;
    }
}
