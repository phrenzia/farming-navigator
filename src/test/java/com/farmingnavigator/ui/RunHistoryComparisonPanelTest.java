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
    private String labels(Component c)
    { StringBuilder s=new StringBuilder(c instanceof JLabel?((JLabel)c).getText():""); if(c instanceof Container){for(Component child:((Container)c).getComponents()){s.append(labels(child));}} return s.toString(); }
    private void layout(Container p){p.doLayout();for(Component c:p.getComponents()){if(c instanceof Container){layout((Container)c);}}}
}
