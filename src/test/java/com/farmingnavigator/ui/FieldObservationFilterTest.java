package com.farmingnavigator.ui;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.FarmingNavigatorPanel;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.storage.StorageSnapshot;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldObservationFilterTest
{
    private FieldPatchObservation observation(FieldFamily family,int raw,Instant at)
    { return new FieldPatchObservation(new FieldPatchStateDecoder().decode(family,raw),at,FarmingGrowthRate.NORMAL); }
    @Test public void familyFilterChangesVisiblePlotsAndCountsWithoutCountingInvalidEvidence() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z"); AtomicReference<FieldFamily> changed=new AtomicReference<>(FieldFamily.HOPS);
            FieldObservationsPanel panel=new FieldObservationsPanel(Map.of(
                    FieldPatchDefinition.VINERY_EAST_1,observation(FieldFamily.GRAPES,2,now),
                    FieldPatchDefinition.VINERY_WEST_6,observation(FieldFamily.GRAPES,15,now),
                    FieldPatchDefinition.VINERY_EAST_2,observation(FieldFamily.FLOWER,12,now),
                    FieldPatchDefinition.VINERY_EAST_3,observation(FieldFamily.GRAPES,11,now.plusSeconds(1)),
                    FieldPatchDefinition.FALADOR_NORTH,observation(FieldFamily.ALLOTMENT,10,now)),now,true,expanded -> {},FieldFamily.GRAPES,changed::set);
            assertEquals(FieldFamily.HOPS,changed.get()); assertTrue(labels(panel).contains("Observed patches: 2 / 12"));
            assertTrue(labels(panel).contains("Last observed harvestable: 1")); assertFalse(labels(panel).contains("Falador - North west"));
            assertTrue(labels(panel).contains("No grape planting checklist"));
            JComboBox<?> filter=filter(panel); assertNotNull(filter); filter.setSelectedIndex(0); assertNull(changed.get());
            assertTrue(labels(panel).contains("Observed patches: 3 / 52")); assertTrue(labels(panel).contains("Falador - North west"));
            filter.setSelectedItem(FieldFamily.GRAPES.getLabel()); assertEquals(FieldFamily.GRAPES,changed.get());
            panel.setSize(225,panel.getPreferredSize().height); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs();
            try { ImageIO.write(image,"png",new File(dir,"grape-observations.png")); } catch(Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void coralCollectionIsSeparateFromHarvestCountsAndHasNoNewGrowthTimer() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z");
            FieldObservationsPanel panel=new FieldObservationsPanel(Map.of(
                    FieldPatchDefinition.GREAT_CONCH_CORAL_EAST,observation(FieldFamily.CORAL,8,now),
                    FieldPatchDefinition.GREAT_CONCH_CORAL_WEST,observation(FieldFamily.CORAL,20,now)),now,true,value -> {},FieldFamily.CORAL,value -> {});
            assertTrue(labels(panel).contains("Observed patches: 2 / 2"));
            assertTrue(labels(panel).contains("Last observed harvestable: 0; diseased or dead: 1"));
            assertTrue(labels(panel).contains("Last observed collect option: 1; quantity unknown"));
            assertFalse(labels(panel).contains("Estimated harvest in"));
            assertTrue(labels(panel).contains("no planting checklist"));
            panel.setSize(225,Math.max(600,panel.getPreferredSize().height)); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs();
            try { ImageIO.write(image,"png",new File(dir,"coral-observations.png")); } catch(Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void animaWitheringIsNotShownAsHarvestReadinessOrOrdinaryDisease() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z");
            FieldObservationsPanel panel=new FieldObservationsPanel(Map.of(FieldPatchDefinition.FARMING_GUILD_ANIMA,
                    observation(FieldFamily.ANIMA,33,now)),now,true,value -> {},FieldFamily.ANIMA,value -> {});
            assertTrue(labels(panel).contains("Observed patches: 1 / 1"));
            assertTrue(labels(panel).contains("Last observed harvestable: 0; diseased or dead: 0"));
            assertTrue(labels(panel).contains("Last observed living anima: 0; withering: 1"));
            assertTrue(labels(panel).contains("Kronos - Withering anima"));
            assertTrue(labels(panel).contains("expiry times are not calculated")); assertFalse(labels(panel).contains("Estimated harvest in"));
            panel.setSize(225,Math.max(650,panel.getPreferredSize().height)); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs();
            try { ImageIO.write(image,"png",new File(dir,"anima-observations.png")); } catch(Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void rootSidebarRetainsFamilyFilterWhenObservationsRefresh() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.now().minusSeconds(1); FarmingNavigatorPanel root=new FarmingNavigatorPanel(() -> {},null);
            AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of(),false);
            StorageSnapshot storage=new StorageSnapshot(Map.of(),Map.of(),Map.of(),false,null); FarmingNavigatorConfig config=new FarmingNavigatorConfig(){};
            root.setFieldObservations(Map.of(FieldPatchDefinition.VINERY_EAST_1,observation(FieldFamily.GRAPES,2,now)));
            root.setAccountState(account,storage,List.of(),Map.of(),config,Map.of());
            JComboBox<?> initial=filter(root); assertNotNull(initial); initial.setSelectedItem(FieldFamily.GRAPES.getLabel());
            root.setFieldObservations(Map.of(FieldPatchDefinition.VINERY_EAST_1,observation(FieldFamily.GRAPES,11,now)));
            root.setAccountState(account,storage,List.of(),Map.of(),config,Map.of());
            assertEquals(FieldFamily.GRAPES.getLabel(),filter(root).getSelectedItem());
            assertTrue(labels(root).contains("Observed patches: 1 / 12"));
        });
    }
    @Test public void hesporiMaturityIsHistoricalAndSeparateFromHarvestCounts() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z");
            FieldObservationsPanel panel=new FieldObservationsPanel(Map.of(FieldPatchDefinition.FARMING_GUILD_HESPORI,
                    observation(FieldFamily.HESPORI,7,now)),now,true,value -> {},FieldFamily.HESPORI,value -> {});
            assertTrue(labels(panel).contains("Observed patches: 1 / 1"));
            assertTrue(labels(panel).contains("Last observed harvestable: 0; diseased or dead: 0"));
            assertTrue(labels(panel).contains("Last observed mature Hespori patch: 1; inspect manually"));
            assertTrue(labels(panel).contains("Mature bytes do not establish combat, rewards or entry access"));
            assertFalse(labels(panel).contains("Estimated harvest in"));
            panel.setSize(225,Math.max(600,panel.getPreferredSize().height)); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs();
            try { ImageIO.write(image,"png",new File(dir,"hespori-observations.png")); } catch(Exception ex) { throw new AssertionError(ex); }
        });
    }
    private JComboBox<?> filter(Container parent)
    {
        for(Component child:parent.getComponents())
        {
            if(child instanceof JComboBox && "All field families".equals(((JComboBox<?>)child).getItemAt(0))) { return (JComboBox<?>)child; }
            if(child instanceof Container) { JComboBox<?> found=filter((Container)child); if(found!=null) { return found; } }
        } return null;
    }
    private String labels(Component component)
    {
        StringBuilder text=new StringBuilder(component instanceof JLabel?((JLabel)component).getText():"");
        if(component instanceof Container) { for(Component child:((Container)component).getComponents()) { text.append(labels(child)); } } return text.toString();
    }
    private void layout(Container parent)
    { parent.doLayout(); for(Component child:parent.getComponents()) { if(child instanceof Container) { layout((Container)child); } } }
}
