package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.guided.*;
import com.farmingnavigator.routing.spirit.*;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlantedSpiritTransportTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private FarmingNavigatorConfig config(boolean enabled) { return new FarmingNavigatorConfig(){ @Override public boolean allowDeclaredSpiritAccess(){ return enabled; } }; }
    private RequirementContext context(boolean quest,Map<Integer,Integer> inv,Map<Integer,Integer> equipment,Map<Integer,Long> bank,boolean bankKnown,boolean enabled)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,quest?Map.of(Quest.TREE_GNOME_VILLAGE,QuestState.FINISHED):Map.of(),Map.of(),Map.of(),false);
        return new RequirementContext(account,config(enabled),new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,inv,equipment,Map.of()),new StorageSnapshot(
                inv.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e->e.getValue().longValue())),
                equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e->e.getValue().longValue())),bank,bankKnown,now));
    }
    private TransportOption option(PlantedSpiritTree tree) { return TransportOption.wealthToPlantedSpiritTree(1,"Planted arrival " + tree,"Travel manually",tree); }
    private VerifiedTransportResult evaluate(RequirementContext context,PlantedSpiritTree tree,Set<PlantedSpiritTree> declared)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),context.getStorageSnapshot(),
                Map.of("Destination",List.of(option(tree))),declared).get("Destination").get(0);
    }
    @Test public void everyDestinationRequiresEnabledFeatureAndExplicitCurrentSessionAccess()
    {
        for(PlantedSpiritTree tree:PlantedSpiritTree.values())
        {
            RequirementContext enabled=context(true,Map.of(ItemID.RING_OF_WEALTH_1,1),Map.of(),Map.of(),true,true);
            assertEquals(VerifiedTransportState.CONFIGURATION_REQUIRED,evaluate(enabled,tree,Set.of()).getState());
            VerifiedTransportResult ready=evaluate(enabled,tree,Set.of(tree)); assertEquals(VerifiedTransportState.READY_NOW,ready.getState()); assertTrue(ready.getDetail().contains("User-declared"));
            RequirementContext disabled=context(true,Map.of(ItemID.RING_OF_WEALTH_1,1),Map.of(),Map.of(),true,false);
            assertEquals(VerifiedTransportState.CONFIGURATION_REQUIRED,evaluate(disabled,tree,Set.of(tree)).getState());
            RequirementContext locked=context(false,Map.of(ItemID.RING_OF_WEALTH_1,1),Map.of(),Map.of(),true,true);
            assertEquals(VerifiedTransportState.LOCKED,evaluate(locked,tree,Set.of(tree)).getState());
        }
        assertFalse(new FarmingNavigatorConfig(){}.allowDeclaredSpiritAccess());
    }
    @Test public void chargedItemStockStillDistinguishesEquipmentBankAndKnownMissing()
    {
        for(PlantedSpiritTree tree:PlantedSpiritTree.values())
        {
            assertEquals(VerifiedTransportState.READY_NOW,evaluate(context(true,Map.of(),Map.of(ItemID.RING_OF_WEALTH_1,1),Map.of(),true,true),tree,Set.of(tree)).getState());
            assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK,evaluate(context(true,Map.of(),Map.of(),Map.of(ItemID.RING_OF_WEALTH_1,1L),true,true),tree,Set.of(tree)).getState());
            assertEquals(VerifiedTransportState.MISSING,evaluate(context(true,Map.of(),Map.of(),Map.of(),true,true),tree,Set.of(tree)).getState());
        }
    }
    @Test public void unknownBankAndLegacyCallersCannotCreateDeclaredAccess()
    {
        RequirementContext unknown=context(true,Map.of(ItemID.RING_OF_WEALTH_1,1),Map.of(),Map.of(),false,true);
        assertTrue(new TransportVerificationService().evaluateAll(unknown.getAccountState(),unknown.getConfig(),unknown.getTravelCapabilitySnapshot(),unknown.getStorageSnapshot(),Map.of("Destination",List.of(option(PlantedSpiritTree.PORT_SARIM))),Set.of(PlantedSpiritTree.PORT_SARIM)).isEmpty());
        RequirementContext known=context(true,Map.of(ItemID.RING_OF_WEALTH_1,1),Map.of(),Map.of(),true,true);
        VerifiedTransportResult legacy=new TransportVerificationService().evaluateAll(known.getAccountState(),known.getConfig(),known.getTravelCapabilitySnapshot(),known.getStorageSnapshot(),Map.of("Destination",List.of(option(PlantedSpiritTree.PORT_SARIM)))).get("Destination").get(0);
        assertEquals(VerifiedTransportState.CONFIGURATION_REQUIRED,legacy.getState());
        assertEquals(VerifiedTransportState.CONFIGURATION_REQUIRED,evaluate(known,PlantedSpiritTree.PORT_SARIM,Set.of(PlantedSpiritTree.HOSIDIUS)).getState());
    }
    @Test public void destinationMetadataIsRequiredAndSurvivesFieldArrivalCopying()
    {
        try { TransportOption.wealthToPlantedSpiritTree(1,"Arrival","Travel",null); fail("Missing destination"); } catch(IllegalArgumentException expected) { }
        try { new TransportOption(1,"Arrival","Travel",TransportCategory.ITEM,TransportVerificationKind.WEALTH_PLANTED_SPIRIT_TREE); fail("Missing typed metadata"); } catch(IllegalArgumentException expected) { }
        Map<FieldPatchDefinition,List<TransportOption>> routes=new FieldPatchTransportService().getAllOptions();
        TransportOption field=routes.get(FieldPatchDefinition.FALADOR_NORTH).stream().filter(route -> route.getVerificationKind()==TransportVerificationKind.WEALTH_PLANTED_SPIRIT_TREE).findFirst().get();
        assertEquals(PlantedSpiritTree.PORT_SARIM,field.getPlantedSpiritTree()); assertTrue(field.getInstructions().contains("Falador - North west"));
        assertFalse(new GardenPatchTransportService().getAllOptions().containsKey(com.farmingnavigator.farming.garden.GardenPatchDefinition.ETCETERIA_BUSH));
    }
    @Test public void distinctPlantedDestinationsShareOneWealthChargePool()
    {
        for(boolean extraBank:new boolean[]{false,true})
        {
            RequirementContext context=context(true,Map.of(ItemID.RING_OF_WEALTH_1,1),Map.of(),extraBank?Map.of(ItemID.RING_OF_WEALTH_1,1L):Map.of(),true,true);
            List<RunTravelStop> stops=new ArrayList<>();
            for(PlantedSpiritTree tree:List.of(PlantedSpiritTree.PORT_SARIM,PlantedSpiritTree.HOSIDIUS))
            {
                VerifiedTransportResult verified=evaluate(context,tree,Set.of(tree)); assertTrue(verified.isUsableForPlannedRun());
                stops.add(new RunTravelStop(){ public VerifiedTransportResult getRecommended(){return verified;} public String getTravelLabel(){return tree.toString();} });
            }
            RunTransportResources resources=new RunTransportResourcePlanner().evaluate(stops,context);
            assertEquals(1,resources.getChecks().size()); assertEquals(extraBank,resources.getChecks().get(0).isSatisfied());
            if(extraBank) { assertTrue(resources.getChecks().get(0).requiresBankWithdrawal()); } else { assertEquals(RequirementState.BLOCKED,resources.getChecks().get(0).getState()); }
        }
    }
    @Test public void guidedArrivalHighlightsChargedWealthAndRequiredBankWithdrawal()
    {
        RequirementContext context=context(true,Map.of(),Map.of(),Map.of(ItemID.RING_OF_WEALTH_1,1L),true,true);
        VerifiedTransportResult verified=evaluate(context,PlantedSpiritTree.PORT_SARIM,Set.of(PlantedSpiritTree.PORT_SARIM));
        Map<HerbPatchDefinition,List<VerifiedTransportResult>> routes=Map.of(HerbPatchDefinition.FALADOR,List.of(verified));
        HerbRunPlan plan=new HerbRunPlanner().plan(context.getAccountState(),List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of())),routes.keySet(),HerbRunCrop.RANARR,HerbRunCompost.NONE,Map.of(),routes,context.getStorageSnapshot(),now)
                .withTransports(routes,HerbTransportPreference.CATALOGUE,context);
        GuidedRunSession session=new GuidedRunSession(); assertTrue(session.start(plan,HerbRunCompost.NONE,now)); session.confirm(now.plusSeconds(1));
        GuidedRunTargets targets=GuidedRunTargets.from(session.snapshot(now),plan,context.getTravelCapabilitySnapshot());
        assertTrue(targets.getInventoryItems().contains(ItemID.RING_OF_WEALTH_1)); assertTrue(targets.getBankItems().contains(ItemID.RING_OF_WEALTH_1));
    }
}
