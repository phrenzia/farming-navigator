package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SharedBudgetPanelTest
{
    @Test public void repairedArrivalAndSharedBudgetExplanationRenderTogether() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z");
            AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of(),false);
            Map<Integer,Integer> inv=Map.of(ItemID.AIRRUNE,6,ItemID.WATERRUNE,2,ItemID.LAWRUNE,0);
            StorageSnapshot stock=new StorageSnapshot(Map.of(ItemID.AIRRUNE,6L,ItemID.WATERRUNE,2L,ItemID.LAWRUNE,0L),Map.of(),Map.of(),true,now);
            RequirementContext context=new RequirementContext(account,null,new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,inv,Map.of(),Map.of()),stock);
            VerifiedTransportResult spell=new VerifiedTransportResult(new TransportOption(1,"Synthetic spell","Cast manually",TransportCategory.SPELL,TransportVerificationKind.TELEPORT_SPELL,TeleportSpellDefinition.FALADOR_TELEPORT),VerifiedTransportState.READY_NOW,"");
            VerifiedTransportResult reusable=new VerifiedTransportResult(new TransportOption(2,"Synthetic reusable","Use manually",TransportCategory.ITEM,TransportVerificationKind.ECTOPHIAL),VerifiedTransportState.READY_NOW,"");
            Map<HerbPatchDefinition,List<VerifiedTransportResult>> routes=Map.of(HerbPatchDefinition.FALADOR,List.of(spell,reusable),HerbPatchDefinition.CATHERBY,List.of(spell,reusable));
            HerbRunPlan plan=new HerbRunPlanner().plan(account,List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of()),new HerbPatchStatus(HerbPatchDefinition.CATHERBY,List.of())),routes.keySet(),HerbRunCrop.RANARR,HerbRunCompost.NONE,Map.of(),routes,stock,now)
                    .withTransports(routes,HerbTransportPreference.SHARED_BUDGET_PAIR,context);
            HerbRunPlanPanel panel=new HerbRunPlanPanel(plan,true,expanded -> {});
            assertTrue(labels(panel).contains("Synthetic reusable")); assertTrue(labels(panel).contains("Alternate arrivals selected"));
            assertTrue(labels(panel).contains("Review withdrawal checks"));
            panel.setSize(225,panel.getPreferredSize().height); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs();
            try { ImageIO.write(image,"png",new File(dir,"paired-budget-repair.png")); } catch(Exception ex) { throw new AssertionError(ex); }
        });
    }
    private String labels(Component component)
    {
        StringBuilder text=new StringBuilder(component instanceof JLabel?((JLabel)component).getText():"");
        if(component instanceof Container) { for(Component child:((Container)component).getComponents()) { text.append(labels(child)); } } return text.toString();
    }
    private void layout(Container component)
    { component.doLayout(); for(Component child:component.getComponents()) { if(child instanceof Container) { layout((Container)child); } } }
}
