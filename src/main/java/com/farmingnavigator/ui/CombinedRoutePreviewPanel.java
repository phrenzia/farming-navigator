package com.farmingnavigator.ui;

import com.farmingnavigator.routing.run.RunTravelStop;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.awt.Color;
import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;

/** Read-only arrival sequence; it never advances guidance or changes component routes. */
public final class CombinedRoutePreviewPanel extends JPanel
{
    public CombinedRoutePreviewPanel(List<? extends RunTravelStop> stops,boolean expanded,Consumer<Boolean> expansionChanged)
    {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel details=new JPanel(); details.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS)); details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT); details.setVisible(expanded);
        JButton toggle=new JButton(expanded?"Hide arrival sequence":"Show arrival sequence"); toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> { boolean show=!details.isVisible(); details.setVisible(show);
            toggle.setText(show?"Hide arrival sequence":"Show arrival sequence"); expansionChanged.accept(show); revalidate(); repaint(); });
        add(toggle); add(details);
        details.add(text("Arrival sequence only. Follow component checklists for travel instructions. Stop order and existing local walks are preserved; shared worksheet checks still apply.",ColorScheme.LIGHT_GRAY_COLOR));
        int number=0;
        for(RunTravelStop stop:List.copyOf(stops))
        {
            details.add(Box.createVerticalStrut(6)); details.add(text((++number)+". "+stop.getTravelLabel(),Color.WHITE));
            if(stop.isLocalWalk())
            { details.add(text("Local walk inside this component; no additional teleport budgeted.",ColorScheme.LIGHT_GRAY_COLOR)); continue; }
            VerifiedTransportResult arrival=stop.getRecommended();
            if(arrival==null || arrival.getOption()==null)
            { details.add(text("DATA REQUIRED: choose a verified arrival in the component plan.",new Color(255,190,80))); continue; }
            details.add(text(arrival.getOption().getDisplayName(),ColorScheme.LIGHT_GRAY_COLOR));
            details.add(text(arrival.getState().getDisplayName()+": "+arrival.getDetail(),arrival.isUsableForPlannedRun()?ColorScheme.LIGHT_GRAY_COLOR:new Color(255,190,80)));
        }
        if(stops.isEmpty()) { details.add(text("No planned arrivals.",ColorScheme.LIGHT_GRAY_COLOR)); }
        details.add(text("Arrival checks describe individual legs. The shared budget can still require withdrawals or block a sequence that reuses limited stock. Every component arrival remains included.",ColorScheme.LIGHT_GRAY_COLOR));
    }
    private static JLabel text(String value,Color color)
    {
        String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        JLabel label=new JLabel("<html><div style='width:135px'>"+escaped+"</div></html>"); label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
