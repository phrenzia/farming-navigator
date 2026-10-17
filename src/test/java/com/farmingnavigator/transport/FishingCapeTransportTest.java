package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.guided.*;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.item.FishingCapeItems;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FishingCapeTransportTest
{
    private final Instant now=Instant.parse("2026-10-17T18:00:00Z");
    private RequirementContext context(int fishing,Map<Integer,Integer> inventory,Map<Integer,Integer> equipment,Map<Integer,Long> bank)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of()).withRealFishingLevel(fishing);
        return new RequirementContext(account,new FarmingNavigatorConfig(){},new TravelCapabilitySnapshot(true,SpellbookType.LUNAR,null,inventory,equipment,Map.of()),
                new StorageSnapshot(inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().longValue())),
                        equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().longValue())),bank,true,now));
    }
    private TransportOption option()
    { return new HerbPatchTransportService().getAllOptions().get(HerbPatchDefinition.ARDOUGNE).stream()
            .filter(value -> value.getVerificationKind()==TransportVerificationKind.FISHING_CAPE_GUILD).findFirst().get(); }
    private VerifiedTransportResult verify(RequirementContext context,TransportOption option)
    { return new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),
            context.getStorageSnapshot(),Map.of(1,List.of(option))).get(1).get(0); }
    private HerbRunPlan herb(RequirementContext context,TransportOption option,HerbPatchDefinition patch)
    {
        Map<HerbPatchDefinition,List<VerifiedTransportResult>> routes=Map.of(patch,List.of(verify(context,option)));
        HerbRunPlan plan=new HerbRunPlanner().plan(context.getAccountState(),List.of(new HerbPatchStatus(patch,List.of())),Set.of(patch),HerbRunCrop.RANARR,
                HerbRunCompost.NONE,Map.of(),routes,context.getStorageSnapshot(),now);
        return plan.withCareResources(HerbRunCareResources.evaluate(plan,HerbRunCure.PLANT_CURE,HerbRunCompost.NONE,context))
                .withTransports(routes,HerbTransportPreference.SHARED_BUDGET,context);
    }

    @Test public void allFourFishingCapesWorkInThreeStorageLocationsAndOtherCapesDoNot()
    {
        assertEquals(Set.of(ItemID.SKILLCAPE_FISHING,ItemID.SKILLCAPE_FISHING_TRIMMED,ItemID.SKILLCAPE_MAX,ItemID.SKILLCAPE_MAX_WORN),FishingCapeItems.getItemIds());
        assertEquals(TransportCategory.ITEM,option().getCategory());assertEquals(97,option().getBaselineRank());
        for(int id:FishingCapeItems.getItemIds())
        {
            assertEquals(VerifiedTransportState.READY_NOW,verify(context(99,Map.of(id,1),Map.of(),Map.of()),option()).getState());
            assertEquals(VerifiedTransportState.READY_NOW,verify(context(99,Map.of(),Map.of(id,1),Map.of()),option()).getState());
            assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK,verify(context(99,Map.of(),Map.of(),Map.of(id,1L)),option()).getState());
        }
        for(int id:new int[]{ItemID.SKILLCAPE_FISHING_HOOD,ItemID.DUMMY_SKILLCAPE_FISHING,ItemID.SKILLCAPE_MAX_ARDY,ItemID.SKILLCAPE_CRAFTING})
        {assertEquals(VerifiedTransportState.MISSING,verify(context(99,Map.of(id,1),Map.of(),Map.of()),option()).getState());}
        assertEquals(VerifiedTransportState.MISSING,verify(context(99,Map.of(),Map.of(),Map.of()),option()).getState());
        try{FishingCapeItems.getItemIds().clear();fail("Mutable cape IDs");}catch(UnsupportedOperationException expected){}
    }

    @Test public void baseFishingMustBeKnownAndAtLeastNinetyNine()
    {
        for(int invalid:new int[]{-1,0,100})
        {assertEquals(VerifiedTransportState.DATA_REQUIRED,verify(context(invalid,Map.of(ItemID.SKILLCAPE_MAX,1),Map.of(),Map.of()),option()).getState());}
        assertEquals(VerifiedTransportState.LOCKED,verify(context(98,Map.of(ItemID.SKILLCAPE_FISHING,1),Map.of(),Map.of()),option()).getState());
    }

    @Test public void allThreeFieldCopiesKeepGuildDestinationAndLabelledApproach()
    {
        for(FieldPatchDefinition patch:List.of(FieldPatchDefinition.ARDOUGNE_NORTH,FieldPatchDefinition.ARDOUGNE_SOUTH,FieldPatchDefinition.ARDOUGNE_FLOWER))
        {
            TransportOption copied=new FieldPatchTransportService().getAllOptions().get(patch).stream()
                    .filter(value -> value.getVerificationKind()==TransportVerificationKind.FISHING_CAPE_GUILD).findFirst().get();
            assertEquals(TransportCategory.ITEM,copied.getCategory());
            assertTrue(copied.getInstructions().contains("Choose Fishing Guild"));assertTrue(copied.getInstructions().contains("leave the guild"));
            assertTrue(copied.getInstructions().contains("allotment/flower area"));assertTrue(copied.getInstructions().contains(patch.getLabel()));
            assertFalse(copied.getInstructions().contains("herb patch"));
        }
    }

    @Test public void oneCapeCoversHerbAndPairedAllotmentsWithoutChargesOrHouseEntry()
    {
        RequirementContext context=context(99,Map.of(ItemID.SKILLCAPE_FISHING,1),Map.of(),Map.of());
        HerbRunPlan herb=herb(context,option(),HerbPatchDefinition.ARDOUGNE);
        FieldRunPlan field=new FieldRunPlanner().plan(context.getAccountState(),Set.of(FieldPatchDefinition.ARDOUGNE_NORTH,FieldPatchDefinition.ARDOUGNE_SOUTH),
                FieldRunCrop.POTATO,FieldRunMode.HARVEST_ALL,false,Map.of(),context.getStorageSnapshot(),now)
                .withTransports(new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),
                        context.getStorageSnapshot(),new FieldPatchTransportService().getAllOptions()),HerbTransportPreference.REUSABLE_FIRST,context);
        CombinedRunPlan combined=new CombinedRunPlanner().plan(EnumSet.of(CombinedRunKind.HERB,CombinedRunKind.FIELD),herb,null,null,field,context);
        assertTrue(combined.getBlockers().toString(),combined.getBlockers().isEmpty());assertEquals(3,combined.getStops().size());
        assertEquals(TransportVerificationKind.FISHING_CAPE_GUILD,field.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(combined.getResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        assertTrue(combined.getResources().getConsumables().isEmpty());
    }

    @Test public void bankedFishingCapeGetsPreparationAndTravelHighlights()
    {
        RequirementContext context=context(99,Map.of(),Map.of(),Map.of(ItemID.SKILLCAPE_FISHING,1L));
        HerbRunPlan herb=herb(context,option(),HerbPatchDefinition.ARDOUGNE);
        GuidedRunSession session=new GuidedRunSession();assertTrue(session.start(herb,HerbRunCompost.NONE,now));
        assertTrue(GuidedRunTargets.from(session.snapshot(now),herb,context.getTravelCapabilitySnapshot()).getBankItems().contains(ItemID.SKILLCAPE_FISHING));
        session.confirm(now.plusSeconds(1));
        assertTrue(GuidedRunTargets.from(session.snapshot(now),herb,context.getTravelCapabilitySnapshot()).getInventoryItems().containsAll(FishingCapeItems.getItemIds()));
    }

    @Test public void bothOrdinaryMaxFormsSupportFarmingGuildAndGuidanceButCosmeticCapeDoesNot()
    {
        TransportOption farming=new TransportOption(1,"Farming Guild cape","Teleport to the Farming Guild",TransportCategory.ITEM,TransportVerificationKind.FARMING_CAPE);
        for(int id:new int[]{ItemID.SKILLCAPE_MAX,ItemID.SKILLCAPE_MAX_WORN})
        {
            assertEquals(VerifiedTransportState.READY_NOW,verify(context(1,Map.of(id,1),Map.of(),Map.of()),farming).getState());
            assertEquals(VerifiedTransportState.READY_NOW,verify(context(1,Map.of(),Map.of(id,1),Map.of()),farming).getState());
            RequirementContext bank=context(1,Map.of(),Map.of(),Map.of(id,1L));
            assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK,verify(bank,farming).getState());
            HerbRunPlan plan=herb(bank,farming,HerbPatchDefinition.FARMING_GUILD);GuidedRunSession session=new GuidedRunSession();
            assertTrue(session.start(plan,HerbRunCompost.NONE,now));session.confirm(now.plusSeconds(1));
            GuidedRunTargets targets=GuidedRunTargets.from(session.snapshot(now),plan,bank.getTravelCapabilitySnapshot());
            assertTrue(targets.getInventoryItems().contains(id));assertTrue(targets.getBankItems().contains(id));
        }
        assertEquals(VerifiedTransportState.MISSING,verify(context(99,Map.of(ItemID.SKILLCAPE_MAX_ARDY,1),Map.of(),Map.of()),farming).getState());
    }
}
