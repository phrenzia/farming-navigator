package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.RunMetricsSnapshot;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public final class RunHistoryPanel extends JPanel
{
    public RunHistoryPanel(List<RunMetricsSnapshot> entries, boolean expanded, Consumer<Boolean> expansionChanged, Runnable clear)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(9,8,12,8)); setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        add(text("Recent run history",Color.WHITE));
        add(text(entries.size()+" saved checklists for this character and mode (last 20).",ColorScheme.LIGHT_GRAY_COLOR));
        if(entries.isEmpty()) { add(text("Complete or stop guidance to save its statistics. Logout, hopping and shutdown interruptions are not saved.",ColorScheme.LIGHT_GRAY_COLOR)); return; }
        JPanel details=new JPanel(); details.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS)); details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT); details.setVisible(expanded);
        JButton toggle=new JButton(expanded?"Hide recent runs":"Show recent runs"); toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> { boolean show=!details.isVisible(); details.setVisible(show);
            toggle.setText(show?"Hide recent runs":"Show recent runs"); expansionChanged.accept(show); revalidate(); repaint(); });
        add(toggle); add(details);
        NumberFormat numbers=NumberFormat.getIntegerInstance(Locale.getDefault());
        DateTimeFormatter time=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        for(RunMetricsSnapshot entry:entries)
        {
            details.add(Box.createVerticalStrut(7));
            details.add(text(entry.getRunName()+" - "+(entry.isCompleted()?"completed":"stopped"),Color.WHITE));
            details.add(text("Ended "+time.format(entry.getFinishedAt())+"; "+entry.getElapsed().toMinutes()+" min.",ColorScheme.LIGHT_GRAY_COLOR));
            details.add(text(entry.isXpKnown()?"Recorded Farming XP: "+numbers.format(entry.getXpGained()):"Farming XP: data required.",ColorScheme.LIGHT_GRAY_COLOR));
            if(entry.getXpPerHour().isPresent()) { details.add(text("Recorded XP/hour: "+numbers.format(Math.round(entry.getXpPerHour().getAsDouble())),ColorScheme.LIGHT_GRAY_COLOR)); }
        }
        details.add(text("Includes preparation, waiting and other Farming actions during each checklist. No crop attribution or predicted harvest XP.",ColorScheme.LIGHT_GRAY_COLOR));
        JButton clearButton=new JButton("Clear history"); clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearButton.addActionListener(event -> { clearButton.setEnabled(false); clear.run(); }); details.add(clearButton);
    }
    private JLabel text(String value,Color color)
    {
        String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        JLabel label=new JLabel("<html><div style='width:135px'>"+escaped+"</div></html>");
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
