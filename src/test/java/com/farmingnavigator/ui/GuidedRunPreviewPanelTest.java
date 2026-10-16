package com.farmingnavigator.ui;

import com.farmingnavigator.routing.run.RunTravelStop;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class GuidedRunPreviewPanelTest
{
    private RunTravelStop stop(String label,boolean walk)
    {
        return new RunTravelStop() {
            @Override public String getTravelLabel() { return label; }
            @Override public boolean isLocalWalk() { return walk; }
            @Override public VerifiedTransportResult getRecommended() { return null; }
        };
    }
    @Test public void previewIsReadOnlyEscapesLabelsAndDistinguishesNearbyAndFinalStops() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            GuidedRunPreviewPanel first=new GuidedRunPreviewPanel(stop("Falador <north>",false),stop("Falador & south",true),true,false);
            assertTrue(labels(first).contains("First stop: Falador &lt;north&gt;"));
            assertTrue(labels(first).contains("Next planned stop: Falador &amp; south (nearby walk in plan)."));
            for(Component child:first.getComponents()) { assertTrue(child instanceof JLabel); }
            GuidedRunPreviewPanel last=new GuidedRunPreviewPanel(stop("Catherby",false),null,false,true);
            assertEquals(1,last.getComponentCount()); assertTrue(labels(last).contains("Final planned stop"));
            assertEquals(0,new GuidedRunPreviewPanel(null,null,false,false).getComponentCount());
            assertEquals(0,new GuidedRunPreviewPanel(stop("Legacy current",false),null,false,false).getComponentCount());
            first.setSize(225,120); first.doLayout(); BufferedImage image=new BufferedImage(225,120,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=image.createGraphics(); first.printAll(graphics); graphics.dispose();
            File directory=new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image,"png",new File(directory,"guided-upcoming-stop.png")); } catch(Exception ex) { throw new AssertionError(ex); }
        });
    }
    private String labels(JPanel panel)
    {
        StringBuilder text=new StringBuilder(); for(Component child:panel.getComponents()) { if(child instanceof JLabel) { text.append(((JLabel)child).getText()); } } return text.toString();
    }
}
