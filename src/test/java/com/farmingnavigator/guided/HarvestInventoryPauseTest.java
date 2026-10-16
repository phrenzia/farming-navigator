package com.farmingnavigator.guided;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.field.*;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class HarvestInventoryPauseTest
{
    @Parameterized.Parameters(name="field={0}") public static Object[] families(){return new Object[]{true,false};}
    private final boolean field;
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final FieldRunSession f=new FieldRunSession(); private final GardenRunSession g=new GardenRunSession();
    private final FieldPatchDefinition fp=FieldPatchDefinition.FALADOR_NORTH;
    private final GardenPatchDefinition gp=GardenPatchDefinition.RIMMINGTON_BUSH;
    private FieldRunPlan fieldPlan; private GardenRunPlan gardenPlan;
    public HarvestInventoryPauseTest(boolean field){this.field=field;}
    private void prepare()
    {
        AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
        if(field){fieldPlan=new FieldRunPlanner().plan(account,Set.of(fp),FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,false,Map.of(),null,now);assertTrue(f.start(fieldPlan,false,now));}
        else{gardenPlan=new GardenRunPlanner().plan(account,Set.of(gp),GardenRunCrop.REDBERRIES,GardenRunMode.KEEP_MATURE,false,Map.of(),null,now);assertTrue(g.start(gardenPlan,false,now));}
    }
    private void harvest(){prepare();command("CONFIRM",1);observe(field?10:14,2,2);assertEquals("HARVEST",step());}
    private WorldPoint location(){return field?fp.getLocation():gp.getLocation();}
    private String step(){return field?f.snapshot(now.plusSeconds(100)).getStep().name():g.snapshot(now.plusSeconds(100)).getStep().name();}
    private long revision(){return field?f.getRevision():g.getRevision();}
    private int completed(){return field?f.snapshot(now).getCompleted():g.snapshot(now).getCompleted();}
    private boolean command(String command,int second){return field?f.command(FieldRunCommand.valueOf(command),now.plusSeconds(second)):g.command(GardenRunCommand.valueOf(command),now.plusSeconds(second));}
    private boolean inventory(int free,WorldPoint player,int second){return field?f.observeInventory(free,player,now.plusSeconds(second)):g.observeInventory(free,player,now.plusSeconds(second));}
    private boolean observe(int raw,int at,int current)
    {
        return field?f.observe(location(),Map.of(fp,new FieldPatchObservation(new FieldPatchStateDecoder().decode(fp.getFamily(),raw),now.plusSeconds(at),FarmingGrowthRate.NORMAL)),now.plusSeconds(current))
                :g.observe(location(),Map.of(gp,new GardenPatchObservation(new GardenPatchStateDecoder().decode(gp.getFamily(),raw),now.plusSeconds(at),FarmingGrowthRate.NORMAL)),now.plusSeconds(current));
    }
    @Test public void fullInventoryPausesOnlyHarvestAndFreeSpaceResumesWithoutCompletingIt()
    {
        prepare();assertFalse(inventory(0,location(),0));command("CONFIRM",1);assertFalse(inventory(0,location(),1));observe(field?10:14,2,2);
        long before=revision();assertTrue(inventory(0,location(),3));assertEquals("MAKE_SPACE",step());assertTrue(revision()>before);assertEquals(0,completed());
        long paused=revision();assertFalse(inventory(0,location(),4));assertEquals(paused,revision());
        assertFalse(command("CONFIRM",4));assertEquals("MAKE_SPACE",step());
        assertTrue(inventory(1,location(),5));assertEquals("HARVEST",step());assertEquals(0,completed());assertTrue(revision()>paused);
        assertFalse(inventory(28,location(),6));assertEquals("HARVEST",step());
    }
    @Test public void nearbyFreshPatchEvidenceStillDecidesProgressAfterSpaceIsMade()
    {
        harvest();inventory(0,location(),3);assertFalse(observe(0,4,4));assertEquals("MAKE_SPACE",step());
        inventory(1,location(),5);assertEquals("HARVEST",step());
        // This valid observation predates the resume but belongs to the same interrupted action.
        assertTrue(observe(0,4,6));assertEquals("PLANT",step());assertEquals(0,completed());
    }
    @Test public void staleAndFutureEvidenceCannotBecomeCompletionAfterALongPause()
    {
        harvest();inventory(0,location(),3);inventory(1,location(),40);
        assertFalse(observe(0,4,40));assertEquals("HARVEST",step());assertFalse(observe(0,42,41));assertEquals(0,completed());
        assertTrue(observe(0,41,41));assertEquals("PLANT",step());
    }
    @Test public void spatialUnknownSlotAndBackwardTimeGuardsCannotPauseOrResumeAnotherLocation()
    {
        harvest();WorldPoint far=new WorldPoint(location().getX()+100,location().getY(),0),plane=new WorldPoint(location().getX(),location().getY(),1);
        for(WorldPoint player:Arrays.asList(null,far,plane)){assertFalse(inventory(0,player,3));}
        for(int free:new int[]{-1,29,Integer.MAX_VALUE}){assertFalse(inventory(free,location(),3));}
        assertFalse(inventory(0,location(),1));assertEquals("HARVEST",step());inventory(0,location(),3);
        for(WorldPoint player:Arrays.asList(null,far,plane)){assertFalse(inventory(1,player,4));}
        assertFalse(inventory(-1,location(),4));assertFalse(inventory(29,location(),4));assertFalse(inventory(1,location(),2));assertEquals("MAKE_SPACE",step());
        assertTrue(inventory(28,location(),4));assertEquals("HARVEST",step());
    }
    @Test public void pauseAndResumeInvalidateOldSidebarActionsWhileUnchangedPollsKeepRevisionStable()
    {
        harvest();long harvestRevision=revision();inventory(0,location(),3);
        assertFalse(ChecklistCommandGuard.dispatch(harvestRevision,revision(),()->command("MARK_COMPLETE",4)));long paused=revision();
        assertFalse(inventory(0,location(),4));assertFalse(observe(field?10:14,4,4));assertEquals(paused,revision());
        inventory(1,location(),5);assertFalse(ChecklistCommandGuard.dispatch(paused,revision(),()->command("SKIP",6)));assertEquals(0,completed());
        long resumed=revision();inventory(0,location(),7);assertTrue(revision()>resumed);inventory(1,location(),8);
        assertTrue(observe(0,6,9));assertEquals("PLANT",step());
    }
    @Test public void manualOverridesAndCancellationClearThePauseBeforeAnotherChecklist()
    {
        for(String command:List.of("CANCEL","SKIP","MARK_COMPLETE"))
        {
            harvest();inventory(0,location(),3);assertTrue(command(command,4));assertNotEquals("MAKE_SPACE",step());
            assertFalse(inventory(1,location(),5));harvest();assertEquals("HARVEST",step());
            assertFalse(observe(0,1,3));assertEquals("HARVEST",step());
        }
    }
    @Test public void pausedHarvestSuppressesInventoryBankMapAndSceneTargets()
    {
        harvest();GuidedRunTargets before=field?GuidedRunTargets.fromField(f.snapshot(now),fieldPlan,null):GuidedRunTargets.fromGarden(g.snapshot(now),gardenPlan,null);
        assertNotNull(field?before.getFieldScenePatch():before.getGardenScenePatch());inventory(0,location(),3);
        GuidedRunTargets targets=field?GuidedRunTargets.fromField(f.snapshot(now),fieldPlan,null):GuidedRunTargets.fromGarden(g.snapshot(now),gardenPlan,null);
        assertTrue(targets.getInventoryItems().isEmpty());assertTrue(targets.getBankItems().isEmpty());assertNull(targets.getDestination());
        assertNull(targets.getFieldScenePatch());assertNull(targets.getGardenScenePatch());
        String instruction=field?f.snapshot(now).getInstruction():g.snapshot(now).getInstruction();assertTrue(instruction.contains("Inventory full"));assertTrue(instruction.contains("does not verify"));
    }
    @Test public void nonHarvestCareAndKeptFlowersDoNotRequestInventorySpace()
    {
        if(field)
        {
            AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);FieldPatchDefinition flower=FieldPatchDefinition.FALADOR_FLOWER;
            FieldRunPlan plan=new FieldRunPlanner().plan(account,Set.of(flower),FieldRunCrop.WHITE_LILY,FieldRunMode.KEEP_MATURE_FLOWERS,false,Map.of(),null,now);
            f.start(plan,false,now);f.command(FieldRunCommand.CONFIRM,now.plusSeconds(1));
            f.observe(flower.getLocation(),Map.of(flower,new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.FLOWER,41),now.plusSeconds(2),FarmingGrowthRate.NORMAL)),now.plusSeconds(2));
            assertEquals("KEEP_FLOWER",step());assertFalse(f.observeInventory(0,flower.getLocation(),now.plusSeconds(3)));
        }
        else{prepare();command("CONFIRM",1);observe(250,2,2);assertEquals("CHECK_HEALTH",step());assertFalse(inventory(0,location(),3));}
    }
    @Test public void inventoryPauseInstructionFitsSidebarAndOnlyOffersExplicitChecklistOverrides() throws Exception
    {
        harvest();inventory(0,location(),3);
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
            javax.swing.JPanel panel=field?new com.farmingnavigator.ui.FieldGuidedRunPanel(f.snapshot(now.plusSeconds(3)),false,command -> calls.incrementAndGet())
                    :new com.farmingnavigator.ui.GardenGuidedRunPanel(g.snapshot(now.plusSeconds(3)),false,command -> calls.incrementAndGet());
            assertTrue(labels(panel).contains("Inventory full"));assertTrue(labels(panel).contains("free slot to resume"));
            assertTrue(labels(panel).contains("does not verify produce quantity or harvest completion"));assertEquals(0,calls.get());
            for(java.awt.Component component:panel.getComponents())
            {if(component instanceof javax.swing.JButton){assertNotEquals("Produce checked (manual)",((javax.swing.JButton)component).getText());}}
            panel.setSize(225,panel.getPreferredSize().height);layout(panel);
            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(225,panel.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
            java.io.File dir=new java.io.File("build/verification");dir.mkdirs();
            try{javax.imageio.ImageIO.write(image,"png",new java.io.File(dir,field?"field-inventory-pause.png":"garden-inventory-pause.png"));}catch(Exception ex){throw new AssertionError(ex);}
        });
        assertEquals("MAKE_SPACE",step());assertEquals(0,completed());
    }
    private String labels(java.awt.Component c)
    {StringBuilder s=new StringBuilder(c instanceof javax.swing.JLabel?((javax.swing.JLabel)c).getText():"");if(c instanceof java.awt.Container){for(java.awt.Component child:((java.awt.Container)c).getComponents()){s.append(labels(child));}}return s.toString();}
    private void layout(java.awt.Container p){p.doLayout();for(java.awt.Component c:p.getComponents()){if(c instanceof java.awt.Container){layout((java.awt.Container)c);}}}

}
