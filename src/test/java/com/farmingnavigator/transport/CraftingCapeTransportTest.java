package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.guided.garden.*;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.item.CraftingCapeItems;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class CraftingCapeTransportTest
{
    private final Instant now=Instant.parse("2026-10-17T18:00:00Z");
    private RequirementContext context(int level,Map<Integer,Integer> inventory,Map<Integer,Integer> equipment,Map<Integer,Long> bank,boolean known)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of()).withRealCraftingLevel(level);
        return new RequirementContext(account,new FarmingNavigatorConfig(){
            @Override public PohHouseLocation pohHouseLocation(){return PohHouseLocation.NONE;}
        },new TravelCapabilitySnapshot(true,SpellbookType.LUNAR,null,inventory,equipment,Map.of()),
                new StorageSnapshot(inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().longValue())),
                        equipment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().longValue())),bank,known,now));
    }
    private TransportOption option()
    { return new GardenPatchTransportService().getAllOptions().get(GardenPatchDefinition.RIMMINGTON_BUSH).stream()
            .filter(value -> value.getVerificationKind()==TransportVerificationKind.CRAFTING_CAPE).findFirst().get(); }
    private VerifiedTransportResult verify(RequirementContext context)
    { return new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),
            context.getStorageSnapshot(),Map.of(1,List.of(option()))).get(1).get(0); }
    private GardenRunPlan plan(RequirementContext context,HerbTransportPreference preference)
    {
        return new GardenRunPlanner().plan(context.getAccountState(),Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),GardenRunCrop.REDBERRIES,
                GardenRunMode.KEEP_MATURE,false,Map.of(),context.getStorageSnapshot(),now)
                .withTransports(new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),
                        context.getStorageSnapshot(),new GardenPatchTransportService().getAllOptions()),preference,context);
    }

    @Test public void fourCapeFormsUseInventoryEquipmentOrVerifiedBankWithManualGuildExit()
    {
        assertEquals(Set.of(ItemID.SKILLCAPE_CRAFTING,ItemID.SKILLCAPE_CRAFTING_TRIMMED,ItemID.SKILLCAPE_MAX,ItemID.SKILLCAPE_MAX_WORN),CraftingCapeItems.getItemIds());
        assertEquals(TransportCategory.ITEM,option().getCategory()); assertEquals(97,option().getBaselineRank());
        assertTrue(option().getInstructions().contains("leave the guild")); assertTrue(option().getInstructions().contains("south-east"));
        for(int id:CraftingCapeItems.getItemIds())
        {
            assertEquals(VerifiedTransportState.READY_NOW,verify(context(99,Map.of(id,1),Map.of(),Map.of(),true)).getState());
            assertEquals(VerifiedTransportState.READY_NOW,verify(context(99,Map.of(),Map.of(id,1),Map.of(),true)).getState());
            assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK,verify(context(99,Map.of(),Map.of(),Map.of(id,1L),true)).getState());
        }
        try{CraftingCapeItems.getItemIds().clear();fail("Mutable cape IDs");}catch(UnsupportedOperationException expected){}
    }

    @Test public void unknownOrLowBaseSkillCannotBeBypassedByHoldingMaxCape()
    {
        for(int level:new int[]{-1,0,100})
        { assertEquals(VerifiedTransportState.DATA_REQUIRED,verify(context(level,Map.of(ItemID.SKILLCAPE_MAX,1),Map.of(),Map.of(),true)).getState()); }
        assertEquals(VerifiedTransportState.LOCKED,verify(context(98,Map.of(ItemID.SKILLCAPE_MAX,1),Map.of(),Map.of(),true)).getState());
        RequirementContext unknown=context(99,Map.of(ItemID.SKILLCAPE_CRAFTING,1),Map.of(),Map.of(),false);
        assertTrue(new TransportVerificationService().evaluateAll(unknown.getAccountState(),unknown.getConfig(),unknown.getTravelCapabilitySnapshot(),
                unknown.getStorageSnapshot(),Map.of(1,List.of(option()))).isEmpty());
    }

    @Test public void hoodsDummyAndCosmeticMaxVariantsDoNotQualify()
    {
        for(int id:new int[]{ItemID.SKILLCAPE_CRAFTING_HOOD,ItemID.DUMMY_SKILLCAPE_CRAFTING,ItemID.SKILLCAPE_MAX_ARDY,ItemID.SKILLCAPE_MAX_FIRECAPE,ItemID.SKILLCAPE_CONSTRUCTION})
        { assertEquals(VerifiedTransportState.MISSING,verify(context(99,Map.of(id,1),Map.of(),Map.of(),true)).getState()); }
        assertEquals(VerifiedTransportState.MISSING,verify(context(99,Map.of(),Map.of(),Map.of(),true)).getState());
    }

    @Test public void sharedBudgetsTreatRepeatedGuildTeleportsAsReusableWithoutEntryOrChargeCosts()
    {
        RequirementContext context=context(99,Map.of(ItemID.SKILLCAPE_CRAFTING,1),Map.of(),Map.of(),true);
        VerifiedTransportResult arrival=verify(context);
        RunTravelStop stop=new RunTravelStop(){
            public VerifiedTransportResult getRecommended(){return arrival;}
            public String getTravelLabel(){return "Crafting Guild to Rimmington";}
        };
        SharedTransportRepair.Result<RunTravelStop> repaired=new SharedTransportRepair().repair(java.util.Collections.nCopies(100,stop),
                value -> List.of(arrival),(value,replacement) -> value,context,RuneCost.builder().build(),true);
        assertEquals(0,repaired.getAttempts());assertTrue(repaired.getResources().isKnownPoolsRequired());
        assertTrue(repaired.getResources().getChecks().stream().allMatch(RequirementResult::isReadyNow));
        assertTrue(repaired.getResources().getConsumables().isEmpty());
    }

    @Test public void reusablePreferenceAndGuidanceIncludeCapeWithoutHouseSupplies()
    {
        RequirementContext context=context(99,Map.of(ItemID.LAWRUNE,1,ItemID.AIRRUNE,3,ItemID.WATERRUNE,1),Map.of(),Map.of(ItemID.SKILLCAPE_CRAFTING,1L),true);
        GardenRunPlan planned=plan(context,HerbTransportPreference.REUSABLE_FIRST);
        assertEquals(TransportVerificationKind.CRAFTING_CAPE,planned.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(planned.getTransportResources().getConsumables().isEmpty());
        GardenRunSession session=new GardenRunSession();assertTrue(session.start(planned,false,now));
        assertTrue(GuidedRunTargets.fromGarden(session.snapshot(now),planned,context.getTravelCapabilitySnapshot()).getBankItems().contains(ItemID.SKILLCAPE_CRAFTING));
        session.command(GardenRunCommand.CONFIRM,now.plusSeconds(1));
        GuidedRunTargets targets=GuidedRunTargets.fromGarden(session.snapshot(now),planned,context.getTravelCapabilitySnapshot());
        assertTrue(targets.getInventoryItems().containsAll(CraftingCapeItems.getItemIds()));
        assertFalse(targets.getInventoryItems().contains(ItemID.POH_TABLET_TELEPORTTOHOUSE));
        assertFalse(targets.getInventoryItems().contains(ItemID.LAWRUNE));
    }
}
