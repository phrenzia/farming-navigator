package com.farmingnavigator.ui;

import com.farmingnavigator.routing.run.RunTravelStop;
import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** Read-only preview of the checklist order; no extra travel or completion action. */
public final class GuidedRunPreviewPanel extends JPanel
{
    public GuidedRunPreviewPanel(RunTravelStop current, RunTravelStop next, boolean preparing, boolean finalStop)
    {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        if (preparing && current != null) { add(text("First stop: " + current.getTravelLabel())); }
        if (next != null)
        {
            add(text("Next planned stop: " + next.getTravelLabel() + (next.isLocalWalk() ? " (nearby walk in plan)." : ".")));
        }
        else if (current != null && finalStop) { add(text("Final planned stop.")); }
    }
    private JLabel text(String value)
    {
        String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        JLabel label=new JLabel("<html><div style='width:135px'>"+escaped+"</div></html>");
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
