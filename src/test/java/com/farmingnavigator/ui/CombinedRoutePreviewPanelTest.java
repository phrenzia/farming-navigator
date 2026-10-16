package com.farmingnavigator.ui;

import com.farmingnavigator.routing.run.RunTravelStop;
import com.farmingnavigator.transport.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombinedRoutePreviewPanelTest
{
    private RunTravelStop stop(String label,boolean local,VerifiedTransportResult arrival)
    { return new RunTravelStop(){ public String getTravelLabel(){return label;} public boolean isLocalWalk(){return local;} public VerifiedTransportResult getRecommended(){return arrival;} }; }
    private VerifiedTransportResult arrival(VerifiedTransportState state)
    { return new VerifiedTransportResult(new TransportOption(1,"Ring & spirit tree","Travel manually",TransportCategory.ITEM,TransportVerificationKind.RING_OF_WEALTH),state,"Current leg check"); }
    @Test public void orderRepeatedAreasLocalWalksAndAllTransportStatesRemainVisible() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            List<RunTravelStop> stops=new ArrayList<>(); stops.add(stop("Shared Guild area",false,arrival(VerifiedTransportState.READY_NOW)));
            stops.add(stop("Shared Guild area",false,arrival(VerifiedTransportState.AVAILABLE_FROM_BANK)));
            stops.add(stop("Adjacent plot",true,new VerifiedTransportResult(new TransportOption(1,"Ghost arrival","Do not show",TransportCategory.ITEM,TransportVerificationKind.RING_OF_WEALTH),VerifiedTransportState.READY_NOW,"Old metadata")));
            for(VerifiedTransportState state:VerifiedTransportState.values()) { stops.add(stop("Check "+state.name(),false,arrival(state))); }
            stops.add(stop("Unknown arrival",false,null));
            AtomicBoolean expanded=new AtomicBoolean(); CombinedRoutePreviewPanel panel=new CombinedRoutePreviewPanel(stops,false,expanded::set);
            String labels=labels(panel); assertTrue(labels.indexOf("1. Shared Guild area")<labels.indexOf("2. Shared Guild area"));
            assertTrue(labels.indexOf("2. Shared Guild area")<labels.indexOf("3. Adjacent plot"));
            assertTrue(labels.contains("Local walk inside this component")); assertFalse(labels.contains("Ghost arrival"));
            for(VerifiedTransportState state:VerifiedTransportState.values()) { assertTrue(labels.contains(state.getDisplayName()+": Current leg check")); }
            assertTrue(labels.contains("DATA REQUIRED: choose a verified arrival")); assertTrue(labels.contains("Ring &amp; spirit tree"));
            assertTrue(labels.contains("shared budget can still require withdrawals or block a sequence"));
            stops.clear(); assertTrue(labels(panel).contains("1. Shared Guild area"));
            JButton toggle=(JButton)panel.getComponent(0); toggle.doClick(); assertTrue(expanded.get());
            panel.setSize(225,panel.getPreferredSize().height); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs(); try {ImageIO.write(image,"png",new File(dir,"combined-arrival-sequence.png"));} catch(Exception ex){throw new AssertionError(ex);}
            toggle.doClick(); assertFalse(expanded.get());
        });
    }
    @Test public void emptyAndIncompleteOptionMetadataRemainDataRequired() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(labels(new CombinedRoutePreviewPanel(List.of(),true,value -> {})).contains("No planned arrivals"));
            CombinedRoutePreviewPanel panel=new CombinedRoutePreviewPanel(List.of(stop("Unverified option",false,new VerifiedTransportResult(null,VerifiedTransportState.DATA_REQUIRED,"Incomplete"))),true,value -> {});
            assertTrue(labels(panel).contains("DATA REQUIRED: choose a verified arrival"));
        });
    }
    private String labels(Component c)
    { StringBuilder s=new StringBuilder(c instanceof JLabel?((JLabel)c).getText():""); if(c instanceof Container){for(Component child:((Container)c).getComponents()){s.append(labels(child));}} return s.toString(); }
    private void layout(Container p){p.doLayout();for(Component c:p.getComponents()){if(c instanceof Container){layout((Container)c);}}}
}
