package com.farmingnavigator.loadout;

import com.farmingnavigator.account.*;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.*;
import com.farmingnavigator.guided.field.*;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarbarianPlantingToolsTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private AccountState account(BarbarianPlantingState state)
    {return new AccountState(true,99,99,EnumSet.of(Quest.BONE_VOYAGE,Quest.PRIEST_IN_PERIL),false).withBarbarianPlanting(state);}
    private boolean dibber(List<SupplyCheck> supplies){return supplies.stream().anyMatch(check -> check.getRequirement().getItemId()==ItemID.DIBBER);}
    private Map<Integer,Long> quantities(List<SupplyCheck> supplies)
    {Map<Integer,Long> result=new HashMap<>();supplies.forEach(check -> {if(check.getRequirement().getItemId()!=ItemID.DIBBER){result.merge(check.getRequirement().getItemId(),check.getRequirement().getQuantity(),Long::sum);}});return result;}
    private FieldRunPlan field(AccountState account,FieldPatchDefinition patch,FieldRunCrop crop)
    {return new FieldRunPlanner().plan(account,Set.of(patch),crop,FieldRunMode.HARVEST_ALL,false,Map.of(),null,now);}
    private GardenRunPlan garden(AccountState account,GardenPatchDefinition patch,GardenRunCrop crop)
    {return new GardenRunPlanner().plan(account,Set.of(patch),crop,GardenRunMode.REPLACE_MATURE,false,Map.of(),null,now);}
    private HerbRunPlan herb(AccountState account)
    {return new HerbRunPlanner().plan(account,List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of())),Set.of(HerbPatchDefinition.FALADOR),HerbRunCrop.RANARR,HerbRunCompost.NONE,Map.of(),Map.of(),null,now);}
    @Test public void policyOnlyOmitsForCompletedAccountsAndTheFiveProvenSeedFamilies()
    {
        for(BarbarianPlantingState state:BarbarianPlantingState.values())
        {
            AccountState account=account(state);boolean complete=state==BarbarianPlantingState.COMPLETED;
            assertEquals(!complete,PlantingToolPolicy.herbDibberRequired(account));
            for(FieldFamily family:FieldFamily.values())
            {boolean supported=family==FieldFamily.ALLOTMENT || family==FieldFamily.FLOWER || family==FieldFamily.HOPS;assertEquals(!complete || !supported,PlantingToolPolicy.fieldDibberRequired(account,family));}
            for(GardenFamily family:GardenFamily.values()){assertEquals(!complete || family!=GardenFamily.BUSH,PlantingToolPolicy.gardenDibberRequired(account,family));}
        }
        assertTrue(PlantingToolPolicy.herbDibberRequired(null));assertTrue(PlantingToolPolicy.fieldDibberRequired(account(BarbarianPlantingState.COMPLETED),null));
    }
    @Test public void realHerbPreparationAndPlantTargetsOmitOnlyTheDibberAfterCompletion()
    {
        HerbRunPlan before=herb(account(BarbarianPlantingState.PLANTED_SEED)),after=herb(account(BarbarianPlantingState.COMPLETED));
        assertTrue(dibber(before.getSupplies()));assertFalse(dibber(after.getSupplies()));assertEquals(quantities(before.getSupplies()),quantities(after.getSupplies()));
        GuidedRunSession session=new GuidedRunSession();assertTrue(session.start(after,HerbRunCompost.NONE,now));session.confirm(now.plusSeconds(1));
        session.observe(HerbPatchDestination.FALADOR.getLocation(),Map.of(HerbPatchDefinition.FALADOR,new com.farmingnavigator.farming.state.HerbPatchObservation(new com.farmingnavigator.farming.state.HerbPatchStateDecoder().decode(0),now.plusSeconds(2))),now.plusSeconds(2));
        assertEquals(GuidedRunStep.PLANT,session.snapshot(now).getStep());GuidedRunTargets targets=GuidedRunTargets.from(session.snapshot(now),after,null);
        assertTrue(targets.getInventoryItems().contains(ItemID.RANARR_SEED));assertFalse(targets.getInventoryItems().contains(ItemID.DIBBER));
        assertTrue(after.getSupplies().stream().anyMatch(check -> check.getRequirement().getNote().contains("Completed Barbarian Planting observed")));
    }
    @Test public void allotmentFlowerHopsAndBushKeepAllSeedAndCareQuantitiesWhileChangingPlantTargets()
    {
        FieldPatchDefinition[] patches={FieldPatchDefinition.FALADOR_NORTH,FieldPatchDefinition.FALADOR_FLOWER,FieldPatchDefinition.YANILLE_HOPS};
        FieldRunCrop[] crops={FieldRunCrop.POTATO,FieldRunCrop.LIMPWURT,FieldRunCrop.JUTE};
        for(int i=0;i<patches.length;i++)
        {
            FieldRunPlan before=field(account(BarbarianPlantingState.TRAINING),patches[i],crops[i]),after=field(account(BarbarianPlantingState.COMPLETED),patches[i],crops[i]);
            assertTrue(dibber(before.getSupplies()));assertFalse(dibber(after.getSupplies()));assertEquals(quantities(before.getSupplies()),quantities(after.getSupplies()));
            FieldRunSession session=new FieldRunSession();session.start(after,false,now);session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1));
            session.observe(patches[i].getLocation(),Map.of(patches[i],new FieldPatchObservation(new FieldPatchStateDecoder().decode(patches[i].getFamily(),0),now.plusSeconds(2),FarmingGrowthRate.NORMAL)),now.plusSeconds(2));
            assertEquals(FieldRunStep.PLANT,session.snapshot(now).getStep());assertFalse(GuidedRunTargets.fromField(session.snapshot(now),after,null).getInventoryItems().contains(ItemID.DIBBER));
        }
        GardenRunPlan before=garden(account(BarbarianPlantingState.NOT_STARTED),GardenPatchDefinition.RIMMINGTON_BUSH,GardenRunCrop.REDBERRIES),after=garden(account(BarbarianPlantingState.COMPLETED),GardenPatchDefinition.RIMMINGTON_BUSH,GardenRunCrop.REDBERRIES);
        assertTrue(dibber(before.getSupplies()));assertFalse(dibber(after.getSupplies()));assertEquals(quantities(before.getSupplies()),quantities(after.getSupplies()));
        GardenRunSession session=new GardenRunSession();session.start(after,false,now);session.command(GardenRunCommand.CONFIRM,now.plusSeconds(1));
        session.observe(GardenPatchDefinition.RIMMINGTON_BUSH.getLocation(),Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH,0),now.plusSeconds(2),FarmingGrowthRate.NORMAL)),now.plusSeconds(2));
        assertFalse(GuidedRunTargets.fromGarden(session.snapshot(now),after,null).getInventoryItems().contains(ItemID.DIBBER));
    }
    @Test public void specialFamiliesKeepTheirPlantingToolsEvenWithCompletedTraining()
    {
        AccountState complete=account(BarbarianPlantingState.COMPLETED);
        FieldPatchDefinition[] patches={FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH,FieldPatchDefinition.MORYTANIA_MUSHROOM,FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA};
        FieldRunCrop[] crops={FieldRunCrop.SEAWEED,FieldRunCrop.MUSHROOM,FieldRunCrop.BELLADONNA};
        for(int i=0;i<patches.length;i++){FieldRunPlan plan=field(complete,patches[i],crops[i]);assertTrue(dibber(plan.getSupplies()));assertTrue(plan.getSupplies().stream().anyMatch(check -> check.getRequirement().getNote().contains("special family is unverified")));}
        assertTrue(dibber(garden(complete,GardenPatchDefinition.AL_KHARID_CACTUS,GardenRunCrop.CACTUS).getSupplies()));
    }
    @Test public void completedUnlockPreparationExplainsTheOmittedToolInTheSidebar() throws Exception
    {
        FieldRunPlan plan=field(account(BarbarianPlantingState.COMPLETED),FieldPatchDefinition.FALADOR_NORTH,FieldRunCrop.POTATO);
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            com.farmingnavigator.ui.FieldRunPlanPanel panel=new com.farmingnavigator.ui.FieldRunPlanPanel(plan,true,value -> {});
            assertTrue(labels(panel).contains("Completed Barbarian Planting observed; no seed dibber is reserved"));
            assertFalse(labels(panel).contains("1 x Seed dibber"));assertTrue(labels(panel).contains("3 x Potato seed/spore"));
            panel.setSize(225,panel.getPreferredSize().height);layout(panel);
            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(225,panel.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();java.io.File dir=new java.io.File("build/verification");dir.mkdirs();
            try{javax.imageio.ImageIO.write(image,"png",new java.io.File(dir,"barbarian-planting-preparation.png"));}catch(Exception ex){throw new AssertionError(ex);}
        });
    }
    private String labels(java.awt.Component c)
    {StringBuilder s=new StringBuilder(c instanceof javax.swing.JLabel?((javax.swing.JLabel)c).getText():"");if(c instanceof java.awt.Container){for(java.awt.Component child:((java.awt.Container)c).getComponents()){s.append(labels(child));}}return s.toString();}
    private void layout(java.awt.Container p){p.doLayout();for(java.awt.Component c:p.getComponents()){if(c instanceof java.awt.Container){layout((java.awt.Container)c);}}}

}
