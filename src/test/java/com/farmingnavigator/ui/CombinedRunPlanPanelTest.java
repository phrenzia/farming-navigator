package com.farmingnavigator.ui;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.run.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombinedRunPlanPanelTest
{
    @Test public void combinedWorksheetExplainsUnknownSuppliesAndHasNoGuidanceAction() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z");
            AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
            RequirementContext context=new RequirementContext(account,new FarmingNavigatorConfig(){},null,null);
            GardenRunPlan garden=new GardenRunPlanner().plan(account,Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),GardenRunCrop.REDBERRIES,GardenRunMode.REPLACE_MATURE,true,Map.of(),null,now)
                    .withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
            FieldRunPlan field=new FieldRunPlanner().plan(account,Set.of(FieldPatchDefinition.FALADOR_NORTH),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,true,Map.of(),null,now)
                    .withTransports(Map.of(),HerbTransportPreference.CATALOGUE,context);
            CombinedRunPlan plan=new CombinedRunPlanner().plan(Set.of(CombinedRunKind.GARDEN,CombinedRunKind.FIELD),null,null,garden,field,context);
            AtomicBoolean expanded=new AtomicBoolean(); CombinedRunPlanPanel panel=new CombinedRunPlanPanel(plan,false,expanded::set);
            assertTrue(labels(panel).contains("2 total stops")); assertTrue(labels(panel).contains("Availability unknown"));
            JButton toggle=(JButton)Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            toggle.doClick(); assertTrue(expanded.get()); assertEquals(1,Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).count());
            panel.setSize(225,panel.getPreferredSize().height); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory=new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image,"png",new File(directory,"combined-preparation.png")); } catch(Exception ex) { throw new AssertionError(ex); }
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
