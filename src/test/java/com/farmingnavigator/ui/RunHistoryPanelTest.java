package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.RunMetricsSnapshot;
import com.farmingnavigator.analytics.RunMetricsTracker;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistoryPanelTest
{
    private RunMetricsSnapshot ended(String name,boolean known,boolean completed)
    {
        Instant start=Instant.parse("2026-10-15T12:00:00Z"); RunMetricsTracker tracker=new RunMetricsTracker();
        tracker.start(name,known?100:-1,start); tracker.finish(known?250:-1,start.plusSeconds(60),completed); return tracker.snapshot(start.plusSeconds(120));
    }
    @Test public void rendersKnownAndUnknownRecordedXpAndDispatchesClearOnlyAfterClick() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean expanded=new AtomicBoolean(),cleared=new AtomicBoolean();
            RunHistoryPanel panel=new RunHistoryPanel(List.of(ended("Field run",true,true),ended("Tree run",false,false)),false,expanded::set,() -> cleared.set(true));
            assertTrue(labels(panel).contains("2 saved checklists")); assertTrue(labels(panel).contains("Field run - completed"));
            assertTrue(labels(panel).contains("Tree run - stopped")); assertTrue(labels(panel).contains("Farming XP: data required"));
            assertTrue(labels(panel).contains("No crop attribution")); assertFalse(cleared.get());
            button(panel,"Show recent runs").doClick(); assertTrue(expanded.get());
            panel.setSize(225,900); layout(panel); BufferedImage image=new BufferedImage(225,900,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory=new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image,"png",new File(directory,"run-history.png")); } catch(Exception ex) { throw new AssertionError(ex); }
            JButton clear=button(panel,"Clear history"); clear.doClick(); assertTrue(cleared.get()); assertFalse(clear.isEnabled());
            button(panel,"Hide recent runs").doClick(); assertFalse(expanded.get());
        });
    }
    @Test public void emptyHistoryExplainsWhichInterruptionsAreNotSaved() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            RunHistoryPanel panel=new RunHistoryPanel(List.of(),false,value -> {},() -> fail("Unexpected clear"));
            assertTrue(labels(panel).contains("0 saved checklists")); assertTrue(labels(panel).contains("shutdown interruptions are not saved"));
            assertNull(button(panel,"Clear history"));
        });
    }
    private JButton button(Container parent,String text)
    {
        for(Component child:parent.getComponents())
        {
            if(child instanceof JButton && text.equals(((JButton)child).getText())) { return (JButton)child; }
            if(child instanceof Container) { JButton result=button((Container)child,text); if(result!=null) { return result; } }
        }
        return null;
    }
    private String labels(Component component)
    {
        StringBuilder result=new StringBuilder(component instanceof JLabel?((JLabel)component).getText():"");
        if(component instanceof Container) { for(Component child:((Container)component).getComponents()) { result.append(labels(child)); } }
        return result.toString();
    }
    private void layout(Container parent)
    {
        parent.doLayout(); for(Component child:parent.getComponents()) { if(child instanceof Container) { layout((Container)child); } }
    }
}
