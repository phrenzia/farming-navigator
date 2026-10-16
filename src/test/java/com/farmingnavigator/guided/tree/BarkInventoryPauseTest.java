package com.farmingnavigator.guided.tree;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.ChecklistCommandGuard;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarkInventoryPauseTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final TreeRunSession session=new TreeRunSession();
    private final TreePatchDefinition patch=TreePatchDefinition.FARMING_GUILD_CELASTRUS;
    private final WorldPoint location=TreePatchDestination.FARMING_GUILD_CELASTRUS.getLocation();
    private TreeRunPlan plan;
    private void prepare()
    {
        AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false).withBoostedWoodcuttingLevel(99);
        plan=new TreeRunPlanner().plan(account,Set.of(patch),TreeRunCrop.CELASTRUS,false,Map.of(),null,now);assertTrue(session.start(plan,false,now));
    }
    private boolean observe(int raw,int at,int current)
    {return session.observe(location,Map.of(patch,new TreePatchObservation(new TreePatchStateDecoder().decode(TreeFamily.CELASTRUS,raw),now.plusSeconds(at),FarmingGrowthRate.NORMAL)),now.plusSeconds(current));}
    private void harvest(){prepare();session.command(TreeRunCommand.CONFIRM,now.plusSeconds(1));observe(14,2,2);assertEquals(TreeRunStep.HARVEST_BARK,step());}
    private TreeRunStep step(){return session.snapshot(now.plusSeconds(100)).getStep();}
    private boolean inventory(int free,WorldPoint player,int second){return session.observeInventory(free,player,now.plusSeconds(second));}
    @Test public void fullBarkHarvestPausesAndNeitherSpaceNorFullInventoryProvesDepletion()
    {
        harvest();long original=session.getRevision();assertTrue(inventory(0,location,3));assertEquals(TreeRunStep.MAKE_SPACE,step());assertTrue(session.getRevision()>original);
        TreeRunSnapshot paused=session.snapshot(now.plusSeconds(3));assertFalse(session.command(TreeRunCommand.REMOVE_TREE,now.plusSeconds(4)));
        assertFalse(observe(17,4,4));assertEquals(TreeRunStep.MAKE_SPACE,step());
        assertTrue(inventory(1,location,5));assertEquals(TreeRunStep.HARVEST_BARK,step());assertEquals(0,session.snapshot(now).getCompleted());
        assertEquals(TreeRunStep.MAKE_SPACE,paused.getStep());assertFalse(session.command(TreeRunCommand.REMOVE_TREE,now.plusSeconds(6)));
        assertTrue(observe(17,4,6));assertEquals(TreeRunStep.KEEP_OR_REMOVE,step());
        assertTrue(session.command(TreeRunCommand.REMOVE_TREE,now.plusSeconds(7)));assertEquals(TreeRunStep.REMOVE,step());
    }
    @Test public void staleFutureAndUnchangedBarkDataCannotTurnFreedSlotsIntoRemovalPermission()
    {
        harvest();inventory(0,location,3);inventory(28,location,40);
        assertFalse(observe(17,4,40));assertFalse(observe(17,42,41));assertFalse(observe(16,41,41));assertEquals(TreeRunStep.HARVEST_BARK,step());
        assertEquals(0,session.snapshot(now).getCompleted());assertTrue(observe(17,42,42));assertEquals(TreeRunStep.KEEP_OR_REMOVE,step());
    }
    @Test public void nonHarvestPhasesInvalidSlotDataAndOtherLocationsDoNotChangeTheChecklist()
    {
        prepare();assertFalse(inventory(0,location,0));session.command(TreeRunCommand.CONFIRM,now.plusSeconds(1));observe(13,2,2);
        assertEquals(TreeRunStep.CHECK_HEALTH,step());assertFalse(inventory(0,location,3));observe(14,3,3);
        WorldPoint far=new WorldPoint(location.getX()+100,location.getY(),0),plane=new WorldPoint(location.getX(),location.getY(),1);
        for(WorldPoint point:Arrays.asList(null,far,plane)){assertFalse(inventory(0,point,4));}
        for(int free:new int[]{-1,29,Integer.MAX_VALUE}){assertFalse(inventory(free,location,4));}
        assertFalse(inventory(0,location,2));inventory(0,location,4);
        for(WorldPoint point:Arrays.asList(null,far,plane)){assertFalse(inventory(1,point,5));}
        assertFalse(inventory(-1,location,5));assertFalse(inventory(1,location,3));assertEquals(TreeRunStep.MAKE_SPACE,step());
    }
    @Test public void pauseAndResumeInvalidateOldRemovalOrCompletionButtonsWithoutPollingChurn()
    {
        harvest();long before=session.getRevision();inventory(0,location,3);long paused=session.getRevision();
        assertFalse(ChecklistCommandGuard.dispatch(before,paused,()->session.command(TreeRunCommand.MARK_COMPLETE,now.plusSeconds(4))));
        assertFalse(inventory(0,location,4));assertEquals(paused,session.getRevision());inventory(1,location,5);
        assertFalse(ChecklistCommandGuard.dispatch(paused,session.getRevision(),()->session.command(TreeRunCommand.MARK_COMPLETE,now.plusSeconds(6))));
        assertTrue(session.getRevision()>paused);assertEquals(TreeRunStep.HARVEST_BARK,step());
    }
    @Test public void explicitOverridesResetPauseAndPausedTargetsContainNoHarvestOrRemovalOutline()
    {
        for(TreeRunCommand command:List.of(TreeRunCommand.CANCEL,TreeRunCommand.SKIP,TreeRunCommand.MARK_COMPLETE))
        {
            harvest();inventory(0,location,3);GuidedRunTargets targets=GuidedRunTargets.fromTree(session.snapshot(now),plan,null);
            assertTrue(targets.getInventoryItems().isEmpty());assertTrue(targets.getBankItems().isEmpty());assertNull(targets.getDestination());assertNull(targets.getTreeScenePatch());
            assertTrue(session.command(command,now.plusSeconds(4)));assertFalse(inventory(1,location,5));harvest();assertEquals(TreeRunStep.HARVEST_BARK,step());
        }
    }
    @Test public void barkPauseSidebarHasNoRemovalButtonAndExplainsFreshEvidence() throws Exception
    {
        harvest();inventory(0,location,3);
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            com.farmingnavigator.ui.TreeGuidedRunPanel panel=new com.farmingnavigator.ui.TreeGuidedRunPanel(session.snapshot(now.plusSeconds(3)),false,command -> fail("Unexpected command"));
            assertTrue(labels(panel).contains("Inventory full"));assertTrue(labels(panel).contains("does not prove bark depletion"));
            for(java.awt.Component c:panel.getComponents()){if(c instanceof javax.swing.JButton){assertFalse(((javax.swing.JButton)c).getText().toLowerCase(java.util.Locale.ROOT).contains("remove tree"));}}
            panel.setSize(225,panel.getPreferredSize().height);layout(panel);java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(225,panel.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();java.io.File dir=new java.io.File("build/verification");dir.mkdirs();
            try{javax.imageio.ImageIO.write(image,"png",new java.io.File(dir,"bark-inventory-pause.png"));}catch(Exception ex){throw new AssertionError(ex);}
        });
    }
    private String labels(java.awt.Component c){StringBuilder s=new StringBuilder(c instanceof javax.swing.JLabel?((javax.swing.JLabel)c).getText():"");if(c instanceof java.awt.Container){for(java.awt.Component child:((java.awt.Container)c).getComponents()){s.append(labels(child));}}return s.toString();}
    private void layout(java.awt.Container p){p.doLayout();for(java.awt.Component c:p.getComponents()){if(c instanceof java.awt.Container){layout((java.awt.Container)c);}}}
}
