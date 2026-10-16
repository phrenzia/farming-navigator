package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.*;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.guided.*;
import com.farmingnavigator.routing.item.ArdougneMonasteryItems;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArdougneMonasteryTransportTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final int[] ids={ItemID.ARDY_CAPE_EASY,ItemID.ARDY_CAPE_MEDIUM,ItemID.ARDY_CAPE_HARD,ItemID.ARDY_CAPE_ELITE,ItemID.SKILLCAPE_MAX_ARDY};
    private RequirementContext context(DiaryTier diary,Map<Integer,Integer> inv,Map<Integer,Integer> equipment,Map<Integer,Long> bank,boolean known)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(DiaryArea.ARDOUGNE,diary),Map.of(),false);
        return new RequirementContext(account,new FarmingNavigatorConfig(){},new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,inv,equipment,Map.of()),
                new StorageSnapshot(inv.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().longValue())),equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().longValue())),bank,known,now));
    }
    private TransportOption monastery()
    {return new GardenPatchTransportService().getAllOptions().get(GardenPatchDefinition.ARDOUGNE_BUSH).stream().filter(option -> option.getVerificationKind()==TransportVerificationKind.ARDOUGNE_CLOAK_MONASTERY).findFirst().get();}
    private Map<String,List<VerifiedTransportResult>> results(RequirementContext context,TransportOption option)
    {return new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),context.getStorageSnapshot(),Map.of("Stop",List.of(option)));}
    private VerifiedTransportResult result(RequirementContext context,TransportOption option){return results(context,option).get("Stop").get(0);}
    @Test public void allFiveNamedCapesDistinguishCarriedEquippedAndVerifiedBankedStock()
    {
        assertEquals(5,ArdougneMonasteryItems.getItemIds().size());
        for(int id:ids)
        {
            assertEquals(VerifiedTransportState.READY_NOW,result(context(DiaryTier.EASY,Map.of(id,1),Map.of(),Map.of(),true),monastery()).getState());
            assertEquals(VerifiedTransportState.READY_NOW,result(context(DiaryTier.EASY,Map.of(),Map.of(id,1),Map.of(),true),monastery()).getState());
            VerifiedTransportResult banked=result(context(DiaryTier.EASY,Map.of(),Map.of(),Map.of(id,1L),true),monastery());
            assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK,banked.getState());assertTrue(banked.getDetail().contains("Choose Monastery"));
        }
        try{ArdougneMonasteryItems.getItemIds().clear();fail("Mutable variants");}catch(UnsupportedOperationException expected){}
    }
    @Test public void diaryBlockerMissingItemUnknownBankAndRegularMaxCapeStayDistinct()
    {
        assertEquals(VerifiedTransportState.LOCKED,result(context(DiaryTier.NONE,Map.of(ItemID.ARDY_CAPE_EASY,1),Map.of(),Map.of(),true),monastery()).getState());
        assertEquals(VerifiedTransportState.MISSING,result(context(DiaryTier.EASY,Map.of(),Map.of(),Map.of(),true),monastery()).getState());
        assertEquals(VerifiedTransportState.MISSING,result(context(DiaryTier.ELITE,Map.of(ItemID.SKILLCAPE_MAX_WORN,1),Map.of(),Map.of(),true),monastery()).getState());
        assertTrue(results(context(DiaryTier.EASY,Map.of(ItemID.ARDY_CAPE_EASY,1),Map.of(),Map.of(),false),monastery()).isEmpty());
    }
    @Test public void monasteryCapesDoNotSilentlyBecomeUnlimitedFarmTeleports()
    {
        TransportOption farm=new TransportOption(1,"Farm teleport","Choose farm",TransportCategory.ITEM,TransportVerificationKind.ARDOUGNE_CLOAK_UNLIMITED);
        for(int id:ids)
        {
            RequirementContext context=context(DiaryTier.EASY,Map.of(id,1),Map.of(),Map.of(),true);
            assertEquals(VerifiedTransportState.READY_NOW,result(context,monastery()).getState());
            assertEquals(id==ItemID.ARDY_CAPE_ELITE || id==ItemID.SKILLCAPE_MAX_ARDY?VerifiedTransportState.READY_NOW:VerifiedTransportState.MISSING,result(context,farm).getState());
        }
    }
    @Test public void repeatedMonasteryArrivalsUseAKnownReusablePoolWithoutChargesOrDailyUses()
    {
        RequirementContext context=context(DiaryTier.EASY,Map.of(),Map.of(ItemID.ARDY_CAPE_EASY,1),Map.of(),true);
        VerifiedTransportResult ready=result(context,monastery());List<RunTravelStop> stops=new ArrayList<>();
        for(String label:List.of("Bush","Northern farm")){stops.add(new RunTravelStop(){public VerifiedTransportResult getRecommended(){return ready;}public String getTravelLabel(){return label;}});}
        SharedTransportRepair.Result<RunTravelStop> repaired=new SharedTransportRepair().repair(stops,stop -> {fail("No repair search needed");return List.of();},(stop,option)->stop,context,RuneCost.builder().build(),true);
        assertEquals(0,repaired.getAttempts());assertTrue(repaired.getResources().isKnownPoolsRequired());assertTrue(repaired.getResources().getChecks().isEmpty());assertTrue(repaired.getResources().getConsumables().isEmpty());
    }
    @Test public void appendedRoutesPreserveDefaultsAndFieldCopyingWhileTravelHighlightsBankedCapes()
    {
        List<TransportOption> garden=new GardenPatchTransportService().getAllOptions().get(GardenPatchDefinition.ARDOUGNE_BUSH);
        assertEquals(TransportVerificationKind.TELEPORT_SPELL,garden.get(0).getVerificationKind());assertEquals(90,monastery().getBaselineRank());
        TransportOption herb=new HerbPatchTransportService().getAllOptions().get(HerbPatchDefinition.ARDOUGNE).stream().filter(option -> option.getVerificationKind()==TransportVerificationKind.ARDOUGNE_CLOAK_MONASTERY).findFirst().get();
        TransportOption field=new FieldPatchTransportService().getAllOptions().get(FieldPatchDefinition.ARDOUGNE_NORTH).stream().filter(option -> option.getVerificationKind()==TransportVerificationKind.ARDOUGNE_CLOAK_MONASTERY).findFirst().get();
        assertTrue(field.getInstructions().contains("allotment/flower area"));assertTrue(field.getInstructions().contains(FieldPatchDefinition.ARDOUGNE_NORTH.getLabel()));
        RequirementContext context=context(DiaryTier.EASY,Map.of(),Map.of(),Map.of(ItemID.ARDY_CAPE_EASY,1L),true);VerifiedTransportResult banked=result(context,herb);
        Map<HerbPatchDefinition,List<VerifiedTransportResult>> routes=Map.of(HerbPatchDefinition.ARDOUGNE,List.of(banked));
        HerbRunPlan plan=new HerbRunPlanner().plan(context.getAccountState(),List.of(new HerbPatchStatus(HerbPatchDefinition.ARDOUGNE,List.of())),routes.keySet(),HerbRunCrop.RANARR,HerbRunCompost.NONE,Map.of(),routes,context.getStorageSnapshot(),now).withTransports(routes,HerbTransportPreference.SHARED_BUDGET,context);
        GuidedRunSession session=new GuidedRunSession();assertTrue(session.start(plan,HerbRunCompost.NONE,now));session.confirm(now.plusSeconds(1));
        GuidedRunTargets targets=GuidedRunTargets.from(session.snapshot(now),plan,context.getTravelCapabilitySnapshot());assertTrue(targets.getInventoryItems().containsAll(ArdougneMonasteryItems.getItemIds()));assertTrue(targets.getBankItems().contains(ItemID.ARDY_CAPE_EASY));
    }
    @Test public void bankedMonasteryArrivalRendersWithManualDestinationInstructions() throws Exception
    {
        RequirementContext context=context(DiaryTier.EASY,Map.of(),Map.of(),Map.of(ItemID.ARDY_CAPE_EASY,1L),true);
        Map<GardenPatchDefinition,List<VerifiedTransportResult>> routes=new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),context.getStorageSnapshot(),new GardenPatchTransportService().getAllOptions());
        GardenRunPlan plan=new GardenRunPlanner().plan(context.getAccountState(),Set.of(GardenPatchDefinition.ARDOUGNE_BUSH),GardenRunCrop.REDBERRIES,GardenRunMode.KEEP_MATURE,false,Map.of(),context.getStorageSnapshot(),now).withTransports(routes,HerbTransportPreference.CATALOGUE,context);
        assertEquals(TransportVerificationKind.ARDOUGNE_CLOAK_MONASTERY,plan.getStops().get(0).getRecommended().getOption().getVerificationKind());
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            com.farmingnavigator.ui.GardenRunPlanPanel panel=new com.farmingnavigator.ui.GardenRunPlanPanel(plan,true,value -> {});
            assertTrue(labels(panel).contains("Ardougne cloak to monastery"));assertTrue(labels(panel).contains("AVAILABLE FROM BANK"));assertTrue(labels(panel).contains("Choose Monastery"));
            panel.setSize(225,panel.getPreferredSize().height);layout(panel);java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(225,panel.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();java.io.File dir=new java.io.File("build/verification");dir.mkdirs();
            try{javax.imageio.ImageIO.write(image,"png",new java.io.File(dir,"monastery-bush-arrival.png"));}catch(Exception ex){throw new AssertionError(ex);}
        });
    }
    private String labels(java.awt.Component c){StringBuilder s=new StringBuilder(c instanceof javax.swing.JLabel?((javax.swing.JLabel)c).getText():"");if(c instanceof java.awt.Container){for(java.awt.Component child:((java.awt.Container)c).getComponents()){s.append(labels(child));}}return s.toString();}
    private void layout(java.awt.Container p){p.doLayout();for(java.awt.Component c:p.getComponents()){if(c instanceof java.awt.Container){layout((java.awt.Container)c);}}}

}
