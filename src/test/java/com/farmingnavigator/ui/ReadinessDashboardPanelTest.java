package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.ReadinessDashboard;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReadinessDashboardPanelTest
{
    @Test public void overviewSeparatesConditionalEstimatesFromLastObservedProblems() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-15T12:00:00Z");
            ReadinessDashboard overview=ReadinessDashboard.at(now).fields(new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false),
                    EnumSet.of(FieldPatchDefinition.FALADOR_NORTH,FieldPatchDefinition.FALADOR_SOUTH),
                    Map.of(FieldPatchDefinition.FALADOR_NORTH,new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT,135),now,FarmingGrowthRate.NORMAL),
                            FieldPatchDefinition.FALADOR_SOUTH,new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.ALLOTMENT,6),now.minusSeconds(2400),FarmingGrowthRate.NORMAL))).build();
            AtomicBoolean expanded=new AtomicBoolean(); ReadinessDashboardPanel panel=new ReadinessDashboardPanel(overview,now,false,expanded::set);
            assertTrue(labels(panel).contains("Last observed diseased: 1")); assertTrue(labels(panel).contains("if healthy (estimate): 1"));
            JButton toggle=(JButton)Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get(); toggle.doClick(); assertTrue(expanded.get());
            assertTrue(labels(panel).contains("visit to confirm")); panel.setSize(225,900); layout(panel);
            BufferedImage image=new BufferedImage(225,900,BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory=new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image,"png",new File(directory,"farming-readiness.png")); } catch(Exception ex) { throw new AssertionError(ex); }
        });
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
