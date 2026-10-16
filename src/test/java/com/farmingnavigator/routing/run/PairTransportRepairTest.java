package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
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

public class PairTransportRepairTest
{
    private static final class Stop implements RunTravelStop
    {
        private final String label; private final boolean local; private final VerifiedTransportResult arrival;
        private Stop(String label,boolean local,VerifiedTransportResult arrival) { this.label=label; this.local=local; this.arrival=arrival; }
        public String getTravelLabel(){return label;} public boolean isLocalWalk(){return local;} public VerifiedTransportResult getRecommended(){return arrival;}
        private Stop with(VerifiedTransportResult next){return new Stop(label,local,next);}
    }
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final SharedTransportRepair repair=new SharedTransportRepair();
    private VerifiedTransportResult spell(String label)
    { return new VerifiedTransportResult(new TransportOption(1,label,"Cast manually",TransportCategory.SPELL,TransportVerificationKind.TELEPORT_SPELL,TeleportSpellDefinition.FALADOR_TELEPORT),VerifiedTransportState.READY_NOW,""); }
    private VerifiedTransportResult ecto(VerifiedTransportState state)
    {return new VerifiedTransportResult(new TransportOption(2,"Reusable alternative","Use manually",TransportCategory.ITEM,TransportVerificationKind.ECTOPHIAL),state,"");}
    private RequirementContext context(int laws,boolean known,Map<Integer,Long> bank)
    {
        Map<Integer,Integer> inv=Map.of(ItemID.LAWRUNE,laws,ItemID.AIRRUNE,100,ItemID.WATERRUNE,100);
        Map<Integer,Long> stock=new HashMap<>(); inv.forEach((id,count)->stock.put(id,count.longValue()));
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,Map.of(),Map.of(),Map.of(),false);
        return new RequirementContext(account,new FarmingNavigatorConfig(){},new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,inv,Map.of(),Map.of()),new StorageSnapshot(stock,Map.of(),bank,known,now));
    }
    private SharedTransportRepair.Result<Stop> run(List<Stop> stops,List<VerifiedTransportResult> options,RequirementContext context,RuneCost care)
    {return repair.repair(stops,stop -> options,Stop::with,context,care,true);}
    private RuneCost empty(){return RuneCost.builder().build();}
    @Test public void twoChangesCanReserveTheLastLawRuneForFarmingWithoutPublishingPartialSingles()
    {
        VerifiedTransportResult baseline=spell("Spell"),alternate=ecto(VerifiedTransportState.READY_NOW);
        List<Stop> stops=List.of(new Stop("First",false,baseline),new Stop("Second",false,baseline));
        RuneCost care=RuneCost.builder().add(RuneKind.LAW,1).build(); RequirementContext context=context(1,true,Map.of());
        SharedTransportRepair.Result<Stop> old=repair.repair(stops,stop -> List.of(baseline,alternate),Stop::with,context,care);
        assertEquals(stops,old.getStops()); assertEquals(0,old.getPairAttempts());
        SharedTransportRepair.Result<Stop> result=run(stops,List.of(baseline,alternate),context,care);
        assertEquals(3,result.getAttempts()); assertEquals(1,result.getPairAttempts());
        assertSame(alternate,result.getStops().get(0).getRecommended()); assertSame(alternate,result.getStops().get(1).getRecommended());
        assertEquals("First",result.getStops().get(0).getTravelLabel()); assertEquals("Second",result.getStops().get(1).getTravelLabel());
        assertTrue(result.getResources().getChecks().stream().allMatch(RequirementResult::isSatisfied));
        assertTrue(result.getResources().getNotes().stream().anyMatch(note -> note.contains("Alternate arrivals selected for First and Second")));
        assertSame(baseline,stops.get(0).getRecommended()); assertSame(baseline,stops.get(1).getRecommended());
        try {result.getStops().clear();fail("Mutable repair");}catch(UnsupportedOperationException expected){}
    }
    @Test public void pairModeStillPrefersASingleChangeAndSkipsSearchWhenBaselineIsCovered()
    {
        VerifiedTransportResult baseline=spell("Spell"),alternate=ecto(VerifiedTransportState.READY_NOW);
        List<Stop> stops=List.of(new Stop("First",false,baseline),new Stop("Second",false,baseline));
        SharedTransportRepair.Result<Stop> one=run(stops,List.of(baseline,alternate),context(1,true,Map.of()),empty());
        assertEquals(1,one.getAttempts()); assertEquals(0,one.getPairAttempts()); assertSame(stops.get(1),one.getStops().get(1));
        SharedTransportRepair.Result<Stop> covered=repair.repair(stops,stop -> {fail("No search required");return List.of();},Stop::with,
                context(1,true,Map.of(ItemID.LAWRUNE,1L)),empty(),true);
        assertEquals(0,covered.getAttempts()); assertEquals(0,covered.getPairAttempts()); assertEquals(stops,covered.getStops());
    }
    @Test public void farmingBankWithdrawalAndUnknownBankStayPartOfEveryPairBudget()
    {
        VerifiedTransportResult baseline=spell("Spell"),alternate=ecto(VerifiedTransportState.READY_NOW);
        List<Stop> stops=List.of(new Stop("First",false,baseline),new Stop("Second",false,baseline)); RuneCost care=RuneCost.builder().add(RuneKind.LAW,1).build();
        SharedTransportRepair.Result<Stop> known=run(stops,List.of(baseline,alternate),context(0,true,Map.of(ItemID.LAWRUNE,1L)),care);
        assertEquals(1,known.getPairAttempts()); assertSame(alternate,known.getStops().get(0).getRecommended());
        assertTrue(known.getResources().getChecks().stream().anyMatch(RequirementResult::requiresBankWithdrawal));
        SharedTransportRepair.Result<Stop> unknown=run(stops,List.of(baseline,alternate),context(0,false,Map.of(ItemID.LAWRUNE,1L)),care);
        assertEquals(stops,unknown.getStops()); assertTrue(unknown.getResources().getChecks().stream().anyMatch(check -> check.getState()==RequirementState.DATA_REQUIRED));
        SharedTransportRepair.Result<Stop> missing=run(stops,List.of(baseline,alternate),context(0,true,Map.of()),care);
        assertEquals(stops,missing.getStops()); assertTrue(missing.getResources().getChecks().stream().anyMatch(check -> !check.isSatisfied()));
    }
    @Test public void unavailableOptionsAndMissingManualArrivalsCannotBePaired()
    {
        VerifiedTransportResult baseline=spell("Spell"); List<Stop> stops=List.of(new Stop("First",false,baseline),new Stop("Second",false,baseline));
        List<VerifiedTransportResult> unavailable=new ArrayList<>(); unavailable.add(baseline);
        for(VerifiedTransportState state:VerifiedTransportState.values()){if(state!=VerifiedTransportState.READY_NOW && state!=VerifiedTransportState.AVAILABLE_FROM_BANK){unavailable.add(ecto(state));}}
        SharedTransportRepair.Result<Stop> result=run(stops,unavailable,context(0,true,Map.of()),empty());
        assertEquals(0,result.getAttempts()); assertEquals(stops,result.getStops());
        List<Stop> manual=List.of(new Stop("Manual",false,null)); result=run(manual,List.of(ecto(VerifiedTransportState.READY_NOW)),context(0,true,Map.of()),empty());
        assertEquals(0,result.getAttempts()); assertEquals(manual,result.getStops());
    }
    @Test public void hardBoundsRetainOriginalBlockersEvenAfterAllPairCandidatesFail()
    {
        VerifiedTransportResult baseline=spell("Baseline"); List<VerifiedTransportResult> choices=new ArrayList<>(); choices.add(baseline);
        for(int i=0;i<5;i++){choices.add(spell("Alternative "+i));}
        List<Stop> stops=new ArrayList<>(); for(int i=0;i<10;i++){stops.add(new Stop("Stop "+i,false,baseline));}
        SharedTransportRepair.Result<Stop> result=run(stops,choices,context(0,true,Map.of()),empty());
        assertEquals(SharedTransportRepair.MAX_ATTEMPTS+SharedTransportRepair.MAX_PAIR_ATTEMPTS,result.getAttempts());
        assertEquals(SharedTransportRepair.MAX_PAIR_ATTEMPTS,result.getPairAttempts()); assertEquals(stops,result.getStops());
        assertTrue(result.getResources().getChecks().stream().anyMatch(check -> !check.isSatisfied()));
        assertTrue(result.getResources().getNotes().stream().anyMatch(note -> note.contains("four alternatives per stop")));
    }
    @Test public void pairAlternativesAreCappedAndDeduplicatedWithoutPretendingSearchIsExhaustive()
    {
        VerifiedTransportResult baseline=spell("Baseline"),alternate=ecto(VerifiedTransportState.READY_NOW);
        List<Stop> stops=List.of(new Stop("First",false,baseline),new Stop("Second",false,baseline));
        List<VerifiedTransportResult> choices=new ArrayList<>();choices.add(baseline);for(int i=0;i<4;i++){choices.add(spell("Alternative "+i));} choices.add(alternate);
        SharedTransportRepair.Result<Stop> capped=run(stops,choices,context(0,true,Map.of()),empty());
        assertEquals(16,capped.getPairAttempts()); assertEquals(stops,capped.getStops());
        SharedTransportRepair.Result<Stop> deduplicated=run(stops,List.of(baseline,alternate,alternate),context(0,true,Map.of()),empty());
        assertEquals(1,deduplicated.getPairAttempts()); assertSame(alternate,deduplicated.getStops().get(1).getRecommended());
    }
    @Test public void localWalksAreNeverReplacedAndAnUnmodelledThirdArrivalStillBlocks()
    {
        VerifiedTransportResult baseline=spell("Spell"),alternate=ecto(VerifiedTransportState.READY_NOW);
        Stop local=new Stop("Adjacent",true,baseline);List<Stop> stops=List.of(new Stop("First",false,baseline),local,new Stop("Second",false,baseline));
        SharedTransportRepair.Result<Stop> result=repair.repair(stops,stop -> {assertFalse(stop.local);return List.of(baseline,alternate);},Stop::with,context(0,true,Map.of()),empty(),true);
        assertEquals(1,result.getPairAttempts()); assertSame(local,result.getStops().get(1));
        VerifiedTransportResult unknown=new VerifiedTransportResult(new TransportOption(1,"Unknown charges","Use manually",TransportCategory.ITEM,TransportVerificationKind.RING_OF_ELEMENTS),VerifiedTransportState.READY_NOW,"");
        List<Stop> unmodelled=List.of(new Stop("First",false,unknown),new Stop("Second",false,unknown),new Stop("Third",false,unknown));
        result=run(unmodelled,List.of(unknown,alternate),context(0,true,Map.of()),empty());assertEquals(unmodelled,result.getStops());
        assertTrue(result.getResources().getChecks().stream().anyMatch(check -> check.getState()==RequirementState.DATA_REQUIRED));
    }
    @Test public void theAcceptedPairCanRepairTwoDifferentSharedPoolsInsteadOfOnlyTheFirstTwoStops()
    {
        VerifiedTransportResult baseline=spell("Spell"),alternate=ecto(VerifiedTransportState.READY_NOW);
        VerifiedTransportResult necklace=new VerifiedTransportResult(new TransportOption(1,"Necklace","Use manually",TransportCategory.JEWELLERY,TransportVerificationKind.SKILLS_NECKLACE),VerifiedTransportState.READY_NOW,"");
        List<Stop> stops=List.of(new Stop("Spell first",false,baseline),new Stop("Spell second",false,baseline),new Stop("Necklace first",false,necklace),new Stop("Necklace second",false,necklace));
        RequirementContext source=context(1,true,Map.of()); Map<Integer,Integer> inventory=new HashMap<>(Map.of(ItemID.LAWRUNE,1,ItemID.AIRRUNE,100,ItemID.WATERRUNE,100));
        inventory.put(ItemID.JEWL_NECKLACE_OF_SKILLS_1,1); Map<Integer,Long> stock=new HashMap<>();inventory.forEach((id,count)->stock.put(id,count.longValue()));
        RequirementContext context=new RequirementContext(source.getAccountState(),source.getConfig(),new TravelCapabilitySnapshot(true,SpellbookType.STANDARD,null,inventory,Map.of(),Map.of()),new StorageSnapshot(stock,Map.of(),Map.of(),true,now));
        SharedTransportRepair.Result<Stop> result=repair.repair(stops,stop -> List.of(stop.arrival,alternate),Stop::with,context,empty(),true);
        assertEquals(2,result.getPairAttempts()); assertSame(alternate,result.getStops().get(0).getRecommended()); assertSame(alternate,result.getStops().get(2).getRecommended());
        assertSame(stops.get(1),result.getStops().get(1)); assertSame(stops.get(3),result.getStops().get(3));
        assertTrue(result.getResources().getChecks().stream().allMatch(RequirementResult::isSatisfied));
    }

}
