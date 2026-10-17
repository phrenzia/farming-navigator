package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.RunHistoryComparison;
import com.farmingnavigator.analytics.RunHistorySummary;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;

/** Compact expandable comparison; every row uses the current history filters and saved sample. */
public final class RunHistoryComparisonPanel extends JPanel
{
    public RunHistoryComparisonPanel(RunHistoryComparison comparison,boolean expanded,Consumer<Boolean> expansionChanged)
    {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); setOpaque(false); setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel details=new JPanel(); details.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS)); details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT); details.setVisible(expanded);
        JButton toggle=new JButton(expanded?"Hide type comparison":"Compare run types"); toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(event -> { boolean show=!details.isVisible(); details.setVisible(show);
            toggle.setText(show?"Hide type comparison":"Compare run types"); expansionChanged.accept(show); revalidate(); repaint(); });
        add(toggle); add(details); NumberFormat numbers=NumberFormat.getIntegerInstance(Locale.getDefault());
        details.add(text("Same filtered saved sample. Different crops, routes and preparation make these descriptive results, not a ranking or a forecast.",ColorScheme.LIGHT_GRAY_COLOR));
        for(RunHistoryComparison.Row row:comparison.getRows())
        {
            RunHistorySummary summary=row.getSummary(); details.add(Box.createVerticalStrut(6)); details.add(text(row.getKind().toString(),Color.WHITE));
            details.add(text(summary.getCompletedCount()+" completed; "+summary.getStoppedCount()+" stopped.",ColorScheme.LIGHT_GRAY_COLOR));
            details.add(text(summary.getStopOutcomes().describe(),ColorScheme.LIGHT_GRAY_COLOR));
            details.add(text(summary.getStopOutcomes().describeCoverage(),ColorScheme.LIGHT_GRAY_COLOR));
            details.add(text("XP known for "+summary.getKnownXpCount()+" / "+summary.getEntries().size()+"; subtotal "+(summary.getKnownXpCount()>0?numbers.format(summary.getKnownXp()):"data required")+".",ColorScheme.LIGHT_GRAY_COLOR));
            details.add(text("Recorded XP/hour: "+(summary.getXpPerHour().isPresent()?numbers.format(Math.round(summary.getXpPerHour().getAsDouble())):"data required")+" (timed known entries in this filtered sample).",ColorScheme.LIGHT_GRAY_COLOR));
            if(row.getMeanCompletedTime().isPresent())
            {
                details.add(text("Completed time: mean "+duration(row.getMeanCompletedTime().get())+"; range "+duration(row.getMinimumCompletedTime().get())+" to "+duration(row.getMaximumCompletedTime().get())+" ("+row.getTimedCompletedCount()+" timed).",ColorScheme.LIGHT_GRAY_COLOR));
            }
            else { details.add(text("Completed time: data required (no positive-duration completed checklists).",ColorScheme.LIGHT_GRAY_COLOR)); }
        }
        JPanel cropDetails=new JPanel();cropDetails.setLayout(new BoxLayout(cropDetails,BoxLayout.Y_AXIS));cropDetails.setOpaque(false);
        cropDetails.setAlignmentX(Component.LEFT_ALIGNMENT);cropDetails.setVisible(false);
        JButton cropToggle=new JButton("Compare recorded crops");cropToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        cropToggle.addActionListener(event -> {boolean show=!cropDetails.isVisible();cropDetails.setVisible(show);
            cropToggle.setText(show?"Hide crop comparison":"Compare recorded crops");revalidate();repaint();});
        details.add(Box.createVerticalStrut(7));details.add(cropToggle);details.add(cropDetails);
        cropDetails.add(text("Recorded crop groups use the same filters. Setup labels describe intended crops; planned stop counts and routes can differ.",ColorScheme.LIGHT_GRAY_COLOR));
        for(RunHistoryComparison.Row row:comparison.getRows())
        {
            for(RunHistoryComparison.CropRow crop:row.getCrops())
            {
                RunHistoryComparison.Row statistics=crop.getStatistics();RunHistorySummary summary=statistics.getSummary();
                cropDetails.add(Box.createVerticalStrut(6));cropDetails.add(text(row.getKind()+" - "+crop.getCropLabel(),Color.WHITE));
                cropDetails.add(text(summary.getCompletedCount()+" completed; "+summary.getStoppedCount()+" stopped; XP known for "+summary.getKnownXpCount()+" / "+summary.getEntries().size()+".",ColorScheme.LIGHT_GRAY_COLOR));
                cropDetails.add(text(summary.getStopOutcomes().describe(),ColorScheme.LIGHT_GRAY_COLOR));
                cropDetails.add(text(summary.getStopOutcomes().describeCoverage(),ColorScheme.LIGHT_GRAY_COLOR));
                cropDetails.add(text("Recorded XP/hour: "+(summary.getXpPerHour().isPresent()?numbers.format(Math.round(summary.getXpPerHour().getAsDouble())):"data required")+".",ColorScheme.LIGHT_GRAY_COLOR));
                cropDetails.add(text(statistics.getMeanCompletedTime().isPresent()?"Mean completed time: "+duration(statistics.getMeanCompletedTime().get())+" ("+statistics.getTimedCompletedCount()+" timed).":"Completed time: data required.",ColorScheme.LIGHT_GRAY_COLOR));
                cropDetails.add(text(!crop.isKnown()?"Planned stops unavailable.":crop.getMinimumPlannedStops()==crop.getMaximumPlannedStops()?"Planned stops: "+crop.getMinimumPlannedStops()+".":"Planned stop counts varied: "+crop.getMinimumPlannedStops()+" to "+crop.getMaximumPlannedStops()+".",ColorScheme.LIGHT_GRAY_COLOR));
            }
        }
        details.add(text("Completion times exclude stopped and zero-duration checklists; unknown XP does not exclude a valid completion time. Includes preparation, waiting and unrelated Farming XP.",ColorScheme.LIGHT_GRAY_COLOR));
        details.add(text("Stop subtotals describe checklist decisions, not harvested crops or verified farming actions. Manual completions are included in completed stops.",ColorScheme.LIGHT_GRAY_COLOR));
    }
    private static String duration(Duration elapsed)
    {
        if(elapsed.compareTo(Duration.ofSeconds(1))<0) { return "under 1 sec"; }
        return elapsed.toMinutes()+" min "+(elapsed.getSeconds()%60)+" sec";
    }
    private static JLabel text(String value,Color color)
    {
        String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        JLabel label=new JLabel("<html><div style='width:135px'>"+escaped+"</div></html>"); label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
}
