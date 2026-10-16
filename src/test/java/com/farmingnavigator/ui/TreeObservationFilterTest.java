package com.farmingnavigator.ui;

import com.farmingnavigator.*;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.*;
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

public class TreeObservationFilterTest
{
    private TreePatchObservation observation(TreeFamily family,int raw,Instant at)
    { return new TreePatchObservation(new TreePatchStateDecoder().decode(family,raw),at,FarmingGrowthRate.NORMAL); }
    @Test public void spiritFilterSeparatesMatureTravelFromHealthCheckAndRejectsInvalidEvidence() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z"); AtomicReference<TreeFamily> changed=new AtomicReference<>(TreeFamily.TREE);
            TreeObservationsPanel panel=new TreeObservationsPanel(Map.of(
                    TreePatchDefinition.PORT_SARIM_SPIRIT,observation(TreeFamily.SPIRIT_TREE,44,now),
                    TreePatchDefinition.FARMING_GUILD_SPIRIT,observation(TreeFamily.SPIRIT_TREE,20,now),
                    TreePatchDefinition.ETCETERIA_SPIRIT,observation(TreeFamily.TREE,12,now),
                    TreePatchDefinition.HOSIDIUS_SPIRIT,observation(TreeFamily.SPIRIT_TREE,44,now.plusSeconds(1)),
                    TreePatchDefinition.VARROCK_TREE,observation(TreeFamily.TREE,12,now)),now,true,value -> {},TreeFamily.SPIRIT_TREE,changed::set);
            assertEquals(TreeFamily.TREE,changed.get()); assertTrue(labels(panel).contains("Observed patches: 2 / 5"));
            assertTrue(labels(panel).contains("Last observed health-check ready: 1")); assertFalse(labels(panel).contains("Varrock"));
            assertTrue(labels(panel).contains("Mature travel-enabled stage; current teleport access is unverified"));
            filter(panel).setSelectedIndex(0); assertNull(changed.get()); assertTrue(labels(panel).contains("Observed patches: 3 / 30"));
            filter(panel).setSelectedItem(TreeFamily.SPIRIT_TREE.getLabel()); assertEquals(TreeFamily.SPIRIT_TREE,changed.get());
            panel.setSize(225,Math.max(1000,panel.getPreferredSize().height)); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs();
            try { ImageIO.write(image,"png",new File(dir,"spirit-tree-observations.png")); } catch(Exception ex) { throw new AssertionError(ex); }
        });
    }
    @Test public void rootSidebarRetainsTreeFamilyOnObservationRefresh() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.now().minusSeconds(1); FarmingNavigatorPanel root=new FarmingNavigatorPanel(() -> {},null);
            AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of(),false);
            StorageSnapshot storage=new StorageSnapshot(Map.of(),Map.of(),Map.of(),false,null); FarmingNavigatorConfig config=new FarmingNavigatorConfig(){};
            root.setTreeObservations(Map.of(TreePatchDefinition.PORT_SARIM_SPIRIT,observation(TreeFamily.SPIRIT_TREE,8,now)));
            root.setAccountState(account,storage,List.of(),Map.of(),config,Map.of()); assertNotNull(filter(root));
            filter(root).setSelectedItem(TreeFamily.SPIRIT_TREE.getLabel());
            root.setTreeObservations(Map.of(TreePatchDefinition.PORT_SARIM_SPIRIT,observation(TreeFamily.SPIRIT_TREE,44,now)));
            root.setAccountState(account,storage,List.of(),Map.of(),config,Map.of());
            assertEquals(TreeFamily.SPIRIT_TREE.getLabel(),filter(root).getSelectedItem()); assertTrue(labels(root).contains("Observed patches: 1 / 5"));
        });
    }
    private JComboBox<?> filter(Container parent)
    {
        for(Component child:parent.getComponents())
        {
            if(child instanceof JComboBox && "All tree families".equals(((JComboBox<?>)child).getItemAt(0))) { return (JComboBox<?>)child; }
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
