package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistoryComparisonPanelTest
{
    @Test public void expandedComparisonFitsSidebarAndSeparatesUnknownXpFromUnknownCompletionTime() throws Exception
    {
        Instant now=Instant.parse("2026-10-16T12:00:00Z");
        RunMetricsTracker herb=new RunMetricsTracker(); herb.start("Herb run",100,now.minusSeconds(360)); herb.finish(460,now.minusSeconds(300),true);
        RunMetricsTracker tree=new RunMetricsTracker(); tree.start("Tree run",-1,now.minusSeconds(200)); tree.finish(-1,now.minusSeconds(100),false);
        RunMetricsTracker field=new RunMetricsTracker(); field.start("Field run",-1,now.minusSeconds(200)); field.finish(-1,now.minusSeconds(50),true);
        RunHistoryComparison comparison=RunHistoryComparison.create(List.of(herb.snapshot(now),tree.snapshot(now),field.snapshot(now)),RunHistoryPeriod.ALL,now);
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean expanded=new AtomicBoolean(); RunHistoryComparisonPanel panel=new RunHistoryComparisonPanel(comparison,false,expanded::set);
            assertTrue(labels(panel).contains("mean 1 min 0 sec")); assertTrue(labels(panel).contains("mean 2 min 30 sec"));
            assertTrue(labels(panel).contains("no positive-duration completed checklists")); assertTrue(labels(panel).contains("subtotal data required"));
            assertTrue(labels(panel).contains("timed known entries in this filtered sample")); assertTrue(labels(panel).contains("not a ranking or a forecast"));
            JButton toggle=(JButton)panel.getComponent(0); toggle.doClick(); assertTrue(expanded.get());
            panel.setSize(225,panel.getPreferredSize().height); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs(); try {ImageIO.write(image,"png",new File(dir,"run-type-comparison.png"));} catch(Exception ex){throw new AssertionError(ex);}
            toggle.doClick(); assertFalse(expanded.get());
        });
    }
    @Test public void recordedCropComparisonExplainsDifferentPlannedStopCountsAndLegacySetup() throws Exception
    {
        Instant now=Instant.parse("2026-10-16T12:00:00Z");RunMetricsTracker first=new RunMetricsTracker(),second=new RunMetricsTracker(),legacy=new RunMetricsTracker();
        first.start("Herb run",100,now.minusSeconds(600),RunHistoryContext.of(RunHistoryStore.Kind.HERB,"RANARR",2));first.finish(200,now.minusSeconds(540),true);
        second.start("Herb run",200,now.minusSeconds(400),RunHistoryContext.of(RunHistoryStore.Kind.HERB,"RANARR",4));second.finish(500,now.minusSeconds(280),false);
        legacy.start("Herb run",-1,now.minusSeconds(200));legacy.finish(-1,now.minusSeconds(100),true);
        RunHistoryComparison comparison=RunHistoryComparison.create(List.of(first.snapshot(now),second.snapshot(now),legacy.snapshot(now)),RunHistoryPeriod.ALL,now);
        SwingUtilities.invokeAndWait(() -> {
            RunHistoryComparisonPanel panel=new RunHistoryComparisonPanel(comparison,true,value -> {});
            JButton toggle=button(panel,"Compare recorded crops");assertNotNull(toggle);toggle.doClick();assertEquals("Hide crop comparison",toggle.getText());
            assertTrue(labels(panel).contains("Herb run - Ranarr"));assertTrue(labels(panel).contains("Herb run - Setup unavailable"));
            assertTrue(labels(panel).contains("Planned stop counts varied: 2 to 4"));assertTrue(labels(panel).contains("Planned stops unavailable"));assertFalse(labels(panel).contains("Planned stops: 0"));
            assertTrue(labels(panel).contains("Setup labels describe intended crops"));
            panel.setSize(225,panel.getPreferredSize().height);layout(panel);BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
            File dir=new File("build/verification");dir.mkdirs();try{ImageIO.write(image,"png",new File(dir,"history-crop-comparison.png"));}catch(Exception ex){throw new AssertionError(ex);}
            toggle.doClick();assertEquals("Compare recorded crops",toggle.getText());
        });
    }
    private JButton button(Container parent,String text)
    {for(Component c:parent.getComponents()){if(c instanceof JButton && text.equals(((JButton)c).getText())){return (JButton)c;}if(c instanceof Container){JButton found=button((Container)c,text);if(found!=null){return found;}}}return null;}
    private String labels(Component c)
    { StringBuilder s=new StringBuilder(c instanceof JLabel?((JLabel)c).getText():""); if(c instanceof Container){for(Component child:((Container)c).getComponents()){s.append(labels(child));}} return s.toString(); }
    private void layout(Container p){p.doLayout();for(Component c:p.getComponents()){if(c instanceof Container){layout((Container)c);}}}
}
