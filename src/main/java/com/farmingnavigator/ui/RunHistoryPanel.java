package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;

public final class RunHistoryPanel extends JPanel
{
    private final List<RunMetricsSnapshot> entries;
    private final JPanel results=new JPanel();
    private final JComboBox<RunHistoryPeriod> period=new JComboBox<>(RunHistoryPeriod.values());
    private final JComboBox<String> kind=new JComboBox<>();
    private List<RunMetricsSnapshot> displayed;
    private Instant now;
    private boolean comparisonOpen;
    public RunHistoryPanel(List<RunMetricsSnapshot> entries,boolean expanded,Consumer<Boolean> expansionChanged,Runnable clear)
    {
        this(entries,expanded,expansionChanged,clear,Instant.now(),RunHistoryPeriod.ALL,null,(window,type) -> {});
    }
    public RunHistoryPanel(List<RunMetricsSnapshot> entries,boolean expanded,Consumer<Boolean> expansionChanged,Runnable clear,
            Instant now,RunHistoryPeriod initialPeriod,RunHistoryStore.Kind initialKind,BiConsumer<RunHistoryPeriod,RunHistoryStore.Kind> filterChanged)
    {
        this.entries=List.copyOf(entries); this.now=now;
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); setBackground(ColorScheme.DARKER_GRAY_COLOR);
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
        kind.addItem("All run types"); for(RunHistoryStore.Kind type:RunHistoryStore.Kind.values()) { kind.addItem(type.toString()); }
        period.setSelectedItem(initialPeriod); kind.setSelectedIndex(initialKind==null?0:initialKind.ordinal()+1);
        for(JComboBox<?> selector:new JComboBox<?>[]{period,kind})
        { selector.setAlignmentX(Component.LEFT_ALIGNMENT); selector.setMaximumSize(new Dimension(190,25)); details.add(Box.createVerticalStrut(4)); details.add(selector); }
        results.setLayout(new BoxLayout(results,BoxLayout.Y_AXIS)); results.setOpaque(false); results.setAlignmentX(Component.LEFT_ALIGNMENT); details.add(results);
        java.awt.event.ActionListener change=event -> { displayed=null; render(); filterChanged.accept(selectedPeriod(),selectedKind()); };
        period.addActionListener(change); kind.addActionListener(change);
        details.add(text("Includes preparation, waiting and other Farming actions during each checklist. No crop attribution or predicted harvest XP. Filters cover only the saved last 20, not all account activity.",ColorScheme.LIGHT_GRAY_COLOR));
        details.add(text("Clear history removes all saved checklists, including those hidden by filters.",ColorScheme.LIGHT_GRAY_COLOR));
        JButton clearButton=new JButton("Clear history"); clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearButton.addActionListener(event -> { clearButton.setEnabled(false); clear.run(); }); details.add(clearButton);
        render();
    }
    private RunHistoryPeriod selectedPeriod() { return (RunHistoryPeriod)period.getSelectedItem(); }
    private RunHistoryStore.Kind selectedKind() { return kind.getSelectedIndex()==0?null:RunHistoryStore.Kind.values()[kind.getSelectedIndex()-1]; }
    /** Recheck rolling-window boundaries without rebuilding unchanged history on ordinary XP refreshes. */
    public void refreshTime(Instant now) { this.now=now; if(!entries.isEmpty()) { render(); } }
    private void render()
    {
        RunHistorySummary summary=RunHistorySummary.create(entries,selectedPeriod(),selectedKind(),now);
        if(summary.getEntries().equals(displayed)) { return; } displayed=summary.getEntries(); results.removeAll();
        NumberFormat numbers=NumberFormat.getIntegerInstance(Locale.getDefault());
        results.add(Box.createVerticalStrut(7)); results.add(text("Showing "+displayed.size()+" saved checklists: "+summary.getCompletedCount()+" completed; "+summary.getStoppedCount()+" stopped.",Color.WHITE));
        if(displayed.isEmpty()) { results.add(text("No saved checklists match these filters.",ColorScheme.LIGHT_GRAY_COLOR)); }
        else
        {
            results.add(text(summary.getKnownXpCount()>0?"Recorded Farming XP subtotal: "+numbers.format(summary.getKnownXp()):"Recorded Farming XP: data required.",ColorScheme.LIGHT_GRAY_COLOR));
            results.add(text("XP known for "+summary.getKnownXpCount()+" / "+displayed.size()+"; total elapsed "+summary.getElapsed().toMinutes()+" min.",ColorScheme.LIGHT_GRAY_COLOR));
            if(summary.getXpPerHour().isPresent()) { results.add(text("Recorded XP/hour (timed known entries): "+numbers.format(Math.round(summary.getXpPerHour().getAsDouble())),ColorScheme.LIGHT_GRAY_COLOR)); }
            results.add(text("Unknown XP and zero-duration entries are excluded from the rate.",ColorScheme.LIGHT_GRAY_COLOR));
        }
        if(!displayed.isEmpty())
        {
            results.add(Box.createVerticalStrut(7));
            results.add(new RunHistoryComparisonPanel(RunHistoryComparison.create(displayed,RunHistoryPeriod.ALL,now),comparisonOpen,open -> comparisonOpen=open));
        }
        DateTimeFormatter time=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        for(RunMetricsSnapshot entry:displayed)
        {
            results.add(Box.createVerticalStrut(7)); results.add(text(entry.getRunName()+" - "+(entry.isCompleted()?"completed":"stopped"),Color.WHITE));
            results.add(text("Ended "+time.format(entry.getFinishedAt())+"; "+entry.getElapsed().toMinutes()+" min.",ColorScheme.LIGHT_GRAY_COLOR));
            results.add(text(entry.isXpKnown()?"Recorded Farming XP: "+numbers.format(entry.getXpGained()):"Farming XP: data required.",ColorScheme.LIGHT_GRAY_COLOR));
            if(entry.getXpPerHour().isPresent()) { results.add(text("Recorded XP/hour: "+numbers.format(Math.round(entry.getXpPerHour().getAsDouble())),ColorScheme.LIGHT_GRAY_COLOR)); }
        }
        revalidate(); repaint();
    }
    private JLabel text(String value,Color color)
    {
        String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        JLabel label=new JLabel("<html><div style='width:135px'>"+escaped+"</div></html>");
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
