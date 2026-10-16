package com.farmingnavigator.ui;

import com.farmingnavigator.analytics.RunMetricsSnapshot;
import com.farmingnavigator.analytics.RunMetricsTracker;
import com.farmingnavigator.analytics.RunHistoryPeriod;
import com.farmingnavigator.analytics.RunHistoryStore;
import javax.swing.JComboBox;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunHistoryPanelTest
{
    private RunMetricsSnapshot ended(String name,boolean known,boolean completed)
    {
        Instant start=Instant.parse("2026-10-15T12:00:00Z"); RunMetricsTracker tracker=new RunMetricsTracker();
        tracker.start(name,known?100:-1,start); tracker.finish(known?250:-1,start.plusSeconds(60),completed); return tracker.snapshot(start.plusSeconds(120));
    }
    @Test public void rendersKnownAndUnknownRecordedXpAndDispatchesClearOnlyAfterClick() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean expanded=new AtomicBoolean(),cleared=new AtomicBoolean();
            RunHistoryPanel panel=new RunHistoryPanel(List.of(ended("Field run",true,true),ended("Tree run",false,false)),false,expanded::set,() -> cleared.set(true));
            assertTrue(labels(panel).contains("2 saved checklists")); assertTrue(labels(panel).contains("Field run - completed"));
            assertTrue(labels(panel).contains("Tree run - stopped")); assertTrue(labels(panel).contains("Farming XP: data required"));
            assertTrue(labels(panel).contains("No crop attribution")); assertFalse(cleared.get());
            button(panel,"Show recent runs").doClick(); assertTrue(expanded.get());
            panel.setSize(225,900); layout(panel); BufferedImage image=new BufferedImage(225,900,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory=new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image,"png",new File(directory,"run-history.png")); } catch(Exception ex) { throw new AssertionError(ex); }
            JButton clear=button(panel,"Clear history"); clear.doClick(); assertTrue(cleared.get()); assertFalse(clear.isEnabled());
            button(panel,"Hide recent runs").doClick(); assertFalse(expanded.get());
        });
    }
    @Test public void rollingWindowAndRunTypeFiltersUpdateWithoutClearingSavedHistory() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z");
            RunMetricsTracker recent=new RunMetricsTracker(); recent.start("Herb run",100,now.minusSeconds(3600)); recent.sample(200,now.minusSeconds(3540)); recent.finish(200,now.minusSeconds(3540),true);
            RunMetricsTracker older=new RunMetricsTracker(); older.start("Tree run",100,now.minusSeconds(172860)); older.sample(300,now.minusSeconds(172800)); older.finish(300,now.minusSeconds(172800),false);
            AtomicBoolean cleared=new AtomicBoolean(); RunHistoryPanel panel=new RunHistoryPanel(List.of(recent.snapshot(now),older.snapshot(now)),true,value -> {},() -> cleared.set(true),now,RunHistoryPeriod.ALL,null,(period,kind) -> {});
            assertTrue(labels(panel).contains("Showing 2 saved checklists"));
            JComboBox<?> period=selector(panel,RunHistoryPeriod.ALL); period.setSelectedItem(RunHistoryPeriod.LAST_24_HOURS);
            assertTrue(labels(panel).contains("Showing 1 saved checklists")); assertFalse(labels(panel).contains("Tree run - stopped"));
            JComboBox<?> kind=selector(panel,"All run types"); kind.setSelectedItem(RunHistoryStore.Kind.TREE.toString());
            assertTrue(labels(panel).contains("No saved checklists match")); assertFalse(cleared.get());
            kind.setSelectedItem("All run types"); panel.refreshTime(now.plusSeconds(86400)); assertTrue(labels(panel).contains("Showing 0 saved checklists"));
            assertNotNull(button(panel,"Clear history")); assertTrue(labels(panel).contains("including those hidden by filters"));
        });
    }
    @Test public void outcomeFilterUpdatesBothRecordsAndComparisonWhileClearStillCoversHiddenEntries() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z");
            RunMetricsTracker completed=new RunMetricsTracker();completed.start("Field run",100,now.minusSeconds(600));completed.finish(200,now.minusSeconds(540),true);
            RunMetricsTracker stopped=new RunMetricsTracker();stopped.start("Field run",100,now.minusSeconds(300));stopped.finish(300,now.minusSeconds(240),false);
            RunMetricsTracker unknown=new RunMetricsTracker();unknown.start("Tree run",-1,now.minusSeconds(200));unknown.finish(-1,now.minusSeconds(100),false);
            AtomicBoolean cleared=new AtomicBoolean();java.util.concurrent.atomic.AtomicReference<com.farmingnavigator.analytics.RunHistoryOutcome> changed=new java.util.concurrent.atomic.AtomicReference<>();
            RunHistoryPanel panel=new RunHistoryPanel(List.of(completed.snapshot(now),stopped.snapshot(now),unknown.snapshot(now)),true,value -> {},() -> cleared.set(true),now,RunHistoryPeriod.ALL,null,(period,kind) -> {},com.farmingnavigator.analytics.RunHistoryOutcome.ALL,changed::set);
            assertNull(changed.get());JComboBox<?> outcome=selector(panel,com.farmingnavigator.analytics.RunHistoryOutcome.ALL);outcome.setSelectedItem(com.farmingnavigator.analytics.RunHistoryOutcome.COMPLETED);
            assertEquals(com.farmingnavigator.analytics.RunHistoryOutcome.COMPLETED,changed.get());assertTrue(labels(panel).contains("Showing 1 saved checklists: 1 completed; 0 stopped"));
            assertFalse(labels(panel).contains("Tree run - stopped"));assertFalse(labels(panel).contains("Field run - stopped"));assertTrue(labels(panel).contains("Recorded Farming XP subtotal: 100"));
            panel.setSize(225,panel.getPreferredSize().height);layout(panel);BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
            File dir=new File("build/verification");dir.mkdirs();try{ImageIO.write(image,"png",new File(dir,"completed-history-filter.png"));}catch(Exception ex){throw new AssertionError(ex);}
            outcome.setSelectedItem(com.farmingnavigator.analytics.RunHistoryOutcome.STOPPED);assertTrue(labels(panel).contains("Showing 2 saved checklists: 0 completed; 2 stopped"));
            selector(panel,"All run types").setSelectedItem(RunHistoryStore.Kind.FIELD.toString());assertTrue(labels(panel).contains("Showing 1 saved checklists: 0 completed; 1 stopped"));
            panel.refreshTime(now.plusSeconds(1));assertEquals(com.farmingnavigator.analytics.RunHistoryOutcome.STOPPED,outcome.getSelectedItem());assertFalse(cleared.get());
            button(panel,"Clear history").doClick();assertTrue(cleared.get());assertTrue(labels(panel).contains("including those hidden by filters"));
        });
    }
    @Test public void rootRemembersOutcomeWhenNewSavedRecordsRebuildTheHistoryCard() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.now();RunMetricsTracker completed=new RunMetricsTracker();completed.start("Field run",0,now.minusSeconds(100));completed.finish(100,now.minusSeconds(90),true);
            RunMetricsTracker stopped=new RunMetricsTracker();stopped.start("Tree run",0,now.minusSeconds(60));stopped.finish(100,now.minusSeconds(50),false);
            com.farmingnavigator.FarmingNavigatorPanel root=new com.farmingnavigator.FarmingNavigatorPanel(() -> {},null);
            com.farmingnavigator.account.AccountState account=new com.farmingnavigator.account.AccountState(true,99,99,99,99,99,99,99,99,java.util.Map.of(),java.util.Map.of(),java.util.Map.of(),false);
            com.farmingnavigator.storage.StorageSnapshot storage=new com.farmingnavigator.storage.StorageSnapshot(java.util.Map.of(),java.util.Map.of(),java.util.Map.of(),false,null);
            com.farmingnavigator.FarmingNavigatorConfig config=new com.farmingnavigator.FarmingNavigatorConfig(){};
            root.setRunHistory(List.of(completed.snapshot(now)),() -> {});root.setAccountState(account,storage,List.of(),java.util.Map.of(),config,java.util.Map.of());
            selector(root,com.farmingnavigator.analytics.RunHistoryOutcome.ALL).setSelectedItem(com.farmingnavigator.analytics.RunHistoryOutcome.COMPLETED);
            root.setRunHistory(List.of(completed.snapshot(now),stopped.snapshot(now)),() -> {});root.setAccountState(account,storage,List.of(),java.util.Map.of(),config,java.util.Map.of());
            assertNotNull(selector(root,com.farmingnavigator.analytics.RunHistoryOutcome.COMPLETED));assertTrue(labels(root).contains("Showing 1 saved checklists: 1 completed; 0 stopped"));assertFalse(labels(root).contains("Tree run - stopped"));
        });
    }
    @Test public void historyShowsCapturedCropAndPlannedStopsWhileLegacyRecordsStayExplicitlyUnknown() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now=Instant.parse("2026-10-16T12:00:00Z");RunMetricsTracker current=new RunMetricsTracker();
            current.start("Field run",100,now.minusSeconds(600),com.farmingnavigator.analytics.RunHistoryContext.of(RunHistoryStore.Kind.FIELD,"POTATO",3));current.finish(200,now.minusSeconds(540),true);
            RunMetricsTracker legacy=new RunMetricsTracker();legacy.start("Herb run",100,now.minusSeconds(300));legacy.finish(150,now.minusSeconds(240),false);
            RunHistoryPanel panel=new RunHistoryPanel(List.of(current.snapshot(now),legacy.snapshot(now)),true,value -> {},() -> {},now,RunHistoryPeriod.ALL,null,(period,kind) -> {});
            assertTrue(labels(panel).contains("Potato; 3 planned stops"));assertTrue(labels(panel).contains("Crop / planned stops: unavailable"));assertFalse(labels(panel).contains("0 planned stops"));
            assertTrue(labels(panel).contains("Field run - completed"));assertTrue(labels(panel).contains("Herb run - stopped"));
            panel.setSize(225,panel.getPreferredSize().height);layout(panel);BufferedImage image=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
            File dir=new File("build/verification");dir.mkdirs();try{ImageIO.write(image,"png",new File(dir,"history-crop-context.png"));}catch(Exception ex){throw new AssertionError(ex);}
        });
    }
    private JComboBox<?> selector(Container parent,Object selected)
    {
        for(Component child:parent.getComponents())
        {
            if(child instanceof JComboBox && selected.equals(((JComboBox<?>)child).getSelectedItem())) { return (JComboBox<?>)child; }
            if(child instanceof Container) { JComboBox<?> found=selector((Container)child,selected); if(found!=null) { return found; } }
        }
        return null;
    }
    @Test public void emptyHistoryExplainsWhichInterruptionsAreNotSaved() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            RunHistoryPanel panel=new RunHistoryPanel(List.of(),false,value -> {},() -> fail("Unexpected clear"));
            assertTrue(labels(panel).contains("0 saved checklists")); assertTrue(labels(panel).contains("shutdown interruptions are not saved"));
            assertNull(button(panel,"Clear history"));
        });
    }
    private JButton button(Container parent,String text)
    {
        for(Component child:parent.getComponents())
        {
            if(child instanceof JButton && text.equals(((JButton)child).getText())) { return (JButton)child; }
            if(child instanceof Container) { JButton result=button((Container)child,text); if(result!=null) { return result; } }
        }
        return null;
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
