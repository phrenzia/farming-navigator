package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SharedTransportRepairTest
{
    private final SharedTransportRepair repair=new SharedTransportRepair();
    private VerifiedTransportResult spell(String label)
    { return new VerifiedTransportResult(new TransportOption(1,label,"Cast manually",TransportCategory.SPELL,TransportVerificationKind.TELEPORT_SPELL,TeleportSpellDefinition.FALADOR_TELEPORT),VerifiedTransportState.READY_NOW,""); }
    private VerifiedTransportResult ecto(VerifiedTransportState state)
    { return new VerifiedTransportResult(new TransportOption(2,"Ectophial","Use manually",TransportCategory.ITEM,TransportVerificationKind.ECTOPHIAL),state,""); }
    private HerbRunStop stop(HerbPatchDefinition patch,VerifiedTransportResult route)
    { return new HerbRunStop(patch,"Inspect",route,null,true); }
    private RequirementContext context(int laws,boolean bankKnown,Map<Integer,Long> bank)
    {
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of(),false);
        Map<Integer,Integer> inventory=Map.of(ItemID.AIRRUNE,60,ItemID.WATERRUNE,20,ItemID.LAWRUNE,laws);
        Map<Integer,Long> items=new HashMap<>(); inventory.forEach((id,count)->items.put(id,count.longValue()));
        return new RequirementContext(account,null,new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,inventory,Map.of(),Map.of()),
                new StorageSnapshot(items,Map.of(),bank,bankKnown,Instant.parse("2026-10-16T12:00:00Z")));
    }
    private SharedTransportRepair.Result<HerbRunStop> run(List<HerbRunStop> stops,List<VerifiedTransportResult> alternatives,RequirementContext context,RuneCost care)
    { return repair.repair(stops,stop -> alternatives,(stop,primary)->stop.withTransports(primary,HerbTransportSelector.fallback(alternatives,primary)),context,care); }
    @Test public void oneAlternateAvoidsReusingLastLawRuneAndPreservesOrderAndFallback()
    {
        VerifiedTransportResult spell=spell("Spell"),ecto=ecto(VerifiedTransportState.READY_NOW);
        List<HerbRunStop> original=List.of(stop(HerbPatchDefinition.FALADOR,spell),stop(HerbPatchDefinition.CATHERBY,spell));
        SharedTransportRepair.Result<HerbRunStop> result=run(original,List.of(spell,ecto),context(1,true,Map.of()),RuneCost.builder().build());
        assertEquals(1,result.getAttempts()); assertSame(ecto,result.getStops().get(0).getRecommended());
        assertSame(spell,result.getStops().get(0).getFallback()); assertSame(original.get(1),result.getStops().get(1));
        assertEquals(HerbPatchDefinition.FALADOR,result.getStops().get(0).getPatch());
        assertTrue(result.getResources().getChecks().stream().allMatch(RequirementResult::isSatisfied));
        assertSame(spell,original.get(0).getRecommended());
        try { result.getStops().clear(); fail("Mutable repair"); } catch(UnsupportedOperationException expected) { }
    }
    @Test public void knownBankCoverageKeepsExistingArrivalsWithoutSearch()
    {
        VerifiedTransportResult spell=spell("Spell"); List<HerbRunStop> stops=List.of(stop(HerbPatchDefinition.FALADOR,spell),stop(HerbPatchDefinition.CATHERBY,spell));
        SharedTransportRepair.Result<HerbRunStop> result=repair.repair(stops,stop -> { fail("Unnecessary search"); return List.of(); },(stop,route)->stop,
                context(1,true,Map.of(ItemID.LAWRUNE,1L)),RuneCost.builder().build());
        assertEquals(0,result.getAttempts()); assertTrue(result.getResources().getChecks().get(0).requiresBankWithdrawal());
        assertEquals(stops,result.getStops());
    }
    @Test public void farmingRunesRemainInEveryCandidateAndMultipleChangesAreNotPretendedSolved()
    {
        VerifiedTransportResult spell=spell("Spell"); List<HerbRunStop> stops=List.of(stop(HerbPatchDefinition.FALADOR,spell),stop(HerbPatchDefinition.CATHERBY,spell));
        RuneCost care=RuneCost.builder().add(RuneKind.LAW,1).build();
        SharedTransportRepair.Result<HerbRunStop> result=run(stops,List.of(spell,ecto(VerifiedTransportState.READY_NOW)),context(1,true,Map.of()),care);
        assertEquals(2,result.getAttempts()); assertEquals(stops,result.getStops());
        assertTrue(result.getResources().getChecks().stream().anyMatch(check -> !check.isSatisfied()));
        assertTrue(result.getResources().getNotes().stream().anyMatch(note -> note.contains("multiple changes")));
    }
    @Test public void unknownBankAndUnavailableAlternativesNeverProduceCoveredResult()
    {
        VerifiedTransportResult spell=spell("Spell"); List<HerbRunStop> stops=List.of(stop(HerbPatchDefinition.FALADOR,spell),stop(HerbPatchDefinition.CATHERBY,spell));
        SharedTransportRepair.Result<HerbRunStop> result=run(stops,List.of(spell,ecto(VerifiedTransportState.LOCKED),ecto(VerifiedTransportState.DATA_REQUIRED)),context(0,false,Map.of()),RuneCost.builder().build());
        assertEquals(0,result.getAttempts()); assertEquals(stops,result.getStops());
        assertTrue(result.getResources().getChecks().stream().anyMatch(check -> check.getState()==RequirementState.DATA_REQUIRED));
    }
    @Test public void missingManualArrivalCannotBeSilentlyDroppedToMakeBudgetCovered()
    {
        List<HerbRunStop> stops=List.of(stop(HerbPatchDefinition.FALADOR,null));
        SharedTransportRepair.Result<HerbRunStop> result=run(stops,List.of(ecto(VerifiedTransportState.READY_NOW)),context(0,true,Map.of()),RuneCost.builder().build());
        assertEquals(0,result.getAttempts()); assertNull(result.getStops().get(0).getRecommended());
        assertTrue(result.getResources().getChecks().stream().anyMatch(check -> !check.isSatisfied()));
    }
    @Test public void unmodelledDailyOrChargePoolIsNotAssumedFree()
    {
        VerifiedTransportResult charged=new VerifiedTransportResult(new TransportOption(1,"Ring of elements","Use manually",TransportCategory.ITEM,TransportVerificationKind.RING_OF_ELEMENTS),VerifiedTransportState.READY_NOW,"");
        List<HerbRunStop> stops=List.of(stop(HerbPatchDefinition.FALADOR,charged),stop(HerbPatchDefinition.CATHERBY,charged));
        SharedTransportRepair.Result<HerbRunStop> result=run(stops,List.of(charged,ecto(VerifiedTransportState.READY_NOW)),context(0,true,Map.of()),RuneCost.builder().build());
        assertEquals(stops,result.getStops()); assertTrue(result.getResources().getChecks().stream().anyMatch(check -> check.getState()==RequirementState.DATA_REQUIRED));
    }
    @Test public void searchStopsAtHardBoundAndDoesNotPublishPartialChanges()
    {
        VerifiedTransportResult baseline=spell("Baseline"); List<VerifiedTransportResult> choices=new ArrayList<>(); choices.add(baseline);
        for(int i=0;i<40;i++) { choices.add(spell("Alternative " + i)); } choices.add(ecto(VerifiedTransportState.READY_NOW));
        List<HerbRunStop> stops=List.of(stop(HerbPatchDefinition.FALADOR,baseline));
        SharedTransportRepair.Result<HerbRunStop> result=run(stops,choices,context(0,true,Map.of()),RuneCost.builder().build());
        assertEquals(SharedTransportRepair.MAX_ATTEMPTS,result.getAttempts()); assertEquals(stops,result.getStops());
    }
    @Test public void oneSubstitutionRepairsSharedJewelleryOrHouseTabletShortage()
    {
        for(boolean house:new boolean[]{false,true})
        {
            VerifiedTransportResult pooled=new VerifiedTransportResult(new TransportOption(1,house?"POH":"Necklace","Travel manually",
                    house?TransportCategory.PLAYER_OWNED_HOUSE:TransportCategory.JEWELLERY,
                    house?TransportVerificationKind.POH_FAIRY_RING:TransportVerificationKind.SKILLS_NECKLACE),VerifiedTransportState.READY_NOW,"");
            int item=house?ItemID.POH_TABLET_TELEPORTTOHOUSE:ItemID.JEWL_NECKLACE_OF_SKILLS_1;
            RequirementContext old=context(0,true,Map.of());
            Map<Integer,Integer> inv=Map.of(item,1);
            com.farmingnavigator.FarmingNavigatorConfig config=new com.farmingnavigator.FarmingNavigatorConfig() {
                @Override public com.farmingnavigator.poh.PohHouseLocation pohHouseLocation() { return com.farmingnavigator.poh.PohHouseLocation.RIMMINGTON; }
            };
            RequirementContext stock=new RequirementContext(old.getAccountState(),config,
                    new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,inv,Map.of(),Map.of()),
                    new StorageSnapshot(Map.of(item,1L),Map.of(),Map.of(),true,Instant.parse("2026-10-16T12:00:00Z")));
            VerifiedTransportResult alternate=ecto(VerifiedTransportState.READY_NOW);
            List<HerbRunStop> stops=List.of(stop(HerbPatchDefinition.FALADOR,pooled),stop(HerbPatchDefinition.CATHERBY,pooled));
            SharedTransportRepair.Result<HerbRunStop> result=run(stops,List.of(pooled,alternate),stock,RuneCost.builder().build());
            assertSame(alternate,result.getStops().get(0).getRecommended()); assertSame(pooled,result.getStops().get(1).getRecommended());
            assertTrue(result.getResources().getChecks().stream().allMatch(RequirementResult::isSatisfied));
            assertTrue(result.getResources().getConsumables().stream().noneMatch(check -> check.getState()==com.farmingnavigator.loadout.SupplyState.MISSING));
        }
    }
    @Test public void refreshRetainsRepairedArrivalOnlyWhileIndividuallyUsableAndModeSelected()
    {
        VerifiedTransportResult spell=spell("Spell"),old=ecto(VerifiedTransportState.READY_NOW),fresh=ecto(VerifiedTransportState.AVAILABLE_FROM_BANK);
        assertSame(fresh,HerbTransportSelector.selectForRefresh(List.of(spell,fresh),HerbTransportPreference.SHARED_BUDGET,old).get(0));
        assertSame(spell,HerbTransportSelector.selectForRefresh(List.of(spell,fresh),HerbTransportPreference.CATALOGUE,old).get(0));
        assertSame(spell,HerbTransportSelector.selectForRefresh(List.of(spell,ecto(VerifiedTransportState.MISSING)),HerbTransportPreference.SHARED_BUDGET,old).get(0));
    }
}
