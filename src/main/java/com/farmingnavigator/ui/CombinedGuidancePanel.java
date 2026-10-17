package com.farmingnavigator.ui;

import com.farmingnavigator.guided.combined.CombinedGuidanceSession;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;

public final class CombinedGuidancePanel extends JPanel
{
    public CombinedGuidancePanel(CombinedGuidanceSession.Snapshot snapshot, boolean canBegin, String blocked,
            Consumer<CombinedGuidanceSession.Command> commands)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9, 8, 12, 8)); setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(text("Combined guided sequence", Color.WHITE));
        if (snapshot.getState() == CombinedGuidanceSession.State.INACTIVE)
        {
            add(text("Start selected components in catalogue order. Each begins at preparation; review supplies and arrivals before continuing.", ColorScheme.LIGHT_GRAY_COLOR));
            if (!blocked.isEmpty()) { add(text(blocked, new Color(255, 190, 80))); }
            button("Begin sequence", CombinedGuidanceSession.Command.BEGIN, commands, canBegin);
            return;
        }
        add(text(snapshot.getSequence().stream().map(Object::toString).collect(Collectors.joining(" > ")), ColorScheme.LIGHT_GRAY_COLOR));
        add(text(snapshot.getFinished() + " / " + snapshot.getSequence().size() + " component checklists finished.", Color.WHITE));
        if (snapshot.getState() == CombinedGuidanceSession.State.RUNNING)
        {
            add(text("Current: " + snapshot.getCurrent() + ". Use its checklist below. The next component waits for your confirmation.", ColorScheme.LIGHT_GRAY_COLOR));
        }
        else if (snapshot.getState() == CombinedGuidanceSession.State.BETWEEN)
        {
            add(text("Next: " + snapshot.getNext() + ". Its plan will refresh from current observations and supplies. Review its preparation before travel.", ColorScheme.LIGHT_GRAY_COLOR));
            if (!blocked.isEmpty()) { add(text(blocked, new Color(255, 190, 80))); }
            button("Prepare next component", CombinedGuidanceSession.Command.NEXT, commands, canBegin);
        }
        else { add(text("Sequence finished. Skipped and manually completed patches remain recorded in the individual checklists.", ColorScheme.LIGHT_GRAY_COLOR)); }
        add(text("Ending the sequence leaves the current individual checklist available. XP and history remain separate for each component.", ColorScheme.LIGHT_GRAY_COLOR));
        button("End sequence", CombinedGuidanceSession.Command.END, commands, true);
    }
    private void button(String label, CombinedGuidanceSession.Command command,
            Consumer<CombinedGuidanceSession.Command> commands, boolean enabled)
    {
        JButton button = new JButton(label); button.setAlignmentX(Component.LEFT_ALIGNMENT); button.setEnabled(enabled);
        button.addActionListener(event -> commands.accept(command)); add(Box.createVerticalStrut(6)); add(button);
    }
    private JLabel text(String value, Color color)
    {
        JLabel label = new JLabel("<html><div style='width:135px'>" + value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;") + "</div></html>");
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
