package com.farmingnavigator.ui;

import com.farmingnavigator.*;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.routing.spirit.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlantedSpiritAccessPanelTest
{
    @Test public void checkboxesStayDraftUntilApplyAndOnlyEmitAnImmutableLocalDeclaration() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SpiritTreeDeclarations model=new SpiritTreeDeclarations(); SpiritTreeDeclarations.Snapshot first=model.synchronize("A",true);
            model.apply("A",first.getRevision(),Set.of(PlantedSpiritTree.FARMING_GUILD));
            AtomicReference<Set<PlantedSpiritTree>> applied=new AtomicReference<>();
            PlantedSpiritAccessPanel panel=new PlantedSpiritAccessPanel(model.snapshot(),true,expanded -> {},applied::set);
            assertTrue(check(panel,"Farming Guild").isSelected()); assertFalse(check(panel,"Port Sarim").isSelected());
            panel.setSize(225,Math.max(700,panel.getPreferredSize().height)); layout(panel);
            BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File dir=new File("build/verification"); dir.mkdirs();
            try { ImageIO.write(image,"png",new File(dir,"planted-spirit-access.png")); } catch(Exception ex) { throw new AssertionError(ex); }
            check(panel,"Port Sarim").setSelected(true); assertNull(applied.get());
            JButton apply=button(panel,"Apply access declarations"); apply.doClick(); assertFalse(apply.isEnabled());
            assertEquals(Set.of(PlantedSpiritTree.FARMING_GUILD,PlantedSpiritTree.PORT_SARIM),applied.get());
            try { applied.get().clear(); fail("Mutable command"); } catch(UnsupportedOperationException expected) { }
        });
    }
    @Test public void absentCharacterProfileDisablesDeclarations() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger calls=new AtomicInteger(); PlantedSpiritAccessPanel panel=new PlantedSpiritAccessPanel(new SpiritTreeDeclarations().snapshot(),true,value -> {},trees -> calls.incrementAndGet());
            for(PlantedSpiritTree tree:PlantedSpiritTree.values()) { assertFalse(check(panel,tree.toString()).isEnabled()); }
            JButton apply=button(panel,"Apply access declarations"); assertFalse(apply.isEnabled()); apply.doClick(); assertEquals(0,calls.get());
        });
    }
    @Test public void rootCachesStagedChoicesButOldProfileButtonsCannotBorrowNewCallbacks() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SpiritTreeDeclarations model=new SpiritTreeDeclarations(); SpiritTreeDeclarations.Snapshot a=model.synchronize("A",true);
            FarmingNavigatorPanel root=new FarmingNavigatorPanel(() -> {},null);
            AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of(),false);
            StorageSnapshot storage=new StorageSnapshot(Map.of(),Map.of(),Map.of(),false,null);
            FarmingNavigatorConfig config=new FarmingNavigatorConfig(){ @Override public boolean allowDeclaredSpiritAccess(){return true;} };
            AtomicBoolean oldApplied=new AtomicBoolean(true); AtomicInteger newCalls=new AtomicInteger();
            root.setTreeObservations(Map.of(TreePatchDefinition.PORT_SARIM_SPIRIT,new TreePatchObservation(new TreePatchStateDecoder().decode(TreeFamily.SPIRIT_TREE,20),Instant.now().minusSeconds(1),com.farmingnavigator.farming.state.FarmingGrowthRate.NORMAL)));
            root.setSpiritDeclarations(a,trees -> oldApplied.set(model.apply(a.getProfile(),a.getRevision(),trees)));
            root.setAccountState(account,storage,List.of(),Map.of(),config,Map.of()); PlantedSpiritAccessPanel original=panel(root); assertNotNull(original);
            assertFalse(check(original,"Port Sarim").isSelected()); // Mature history never becomes a declaration.
            check(original,"Port Sarim").setSelected(true);
            root.setSpiritDeclarations(model.snapshot(),trees -> oldApplied.set(model.apply(a.getProfile(),a.getRevision(),trees)));
            root.setAccountState(account,storage,List.of(),Map.of(),config,Map.of()); assertSame(original,panel(root)); assertTrue(check(original,"Port Sarim").isSelected());
            SpiritTreeDeclarations.Snapshot b=model.synchronize("B",true);
            root.setSpiritDeclarations(b,trees -> { newCalls.incrementAndGet(); model.apply(b.getProfile(),b.getRevision(),trees); });
            root.setAccountState(account,storage,List.of(),Map.of(),config,Map.of()); assertNotSame(original,panel(root));
            button(original,"Apply access declarations").doClick(); assertFalse(oldApplied.get()); assertEquals(0,newCalls.get()); assertTrue(model.snapshot().getTrees().isEmpty());
            root.setSpiritDeclarations(model.snapshot(),trees -> {}); root.setAccountState(account,storage,List.of(),Map.of(),config,Map.of());
            assertTrue(button(panel(root),"Apply access declarations").isEnabled());
        });
    }
    private PlantedSpiritAccessPanel panel(Container parent)
    {
        for(Component child:parent.getComponents()) { if(child instanceof PlantedSpiritAccessPanel) {return (PlantedSpiritAccessPanel)child;} if(child instanceof Container) {PlantedSpiritAccessPanel found=panel((Container)child);if(found!=null){return found;}} } return null;
    }
    private JCheckBox check(Container parent,String name)
    {
        for(Component child:parent.getComponents()) { if(child instanceof JCheckBox && name.equals(((JCheckBox)child).getText())) {return (JCheckBox)child;} if(child instanceof Container) {JCheckBox found=check((Container)child,name);if(found!=null){return found;}} } return null;
    }
    private JButton button(Container parent,String name)
    {
        for(Component child:parent.getComponents()) { if(child instanceof JButton && name.equals(((JButton)child).getText())) {return (JButton)child;} if(child instanceof Container) {JButton found=button((Container)child,name);if(found!=null){return found;}} } return null;
    }
    private void layout(Container parent)
    {parent.doLayout();for(Component child:parent.getComponents()){if(child instanceof Container){layout((Container)child);}}}
}
