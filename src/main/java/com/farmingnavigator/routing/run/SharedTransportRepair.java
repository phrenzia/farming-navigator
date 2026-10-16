package com.farmingnavigator.routing.run;

import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RuneCost;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Bounded one/two-arrival repair, preserving stop order. Not a global route, time or cost optimizer. */
public final class SharedTransportRepair
{
    static final int MAX_ATTEMPTS = 32;
    static final int MAX_PAIR_ATTEMPTS = 64;
    static final int MAX_PAIR_ALTERNATIVES_PER_STOP = 4;
    public static final class Result<S extends RunTravelStop>
    {
        private final List<S> stops;
        private final RunTransportResources resources;
        private final int attempts,pairAttempts;
        private Result(List<S> stops, RunTransportResources resources, int attempts, String note)
        { this(stops,resources,attempts,0,note); }
        private Result(List<S> stops,RunTransportResources resources,int attempts,int pairAttempts,String note)
        { this.stops=List.copyOf(stops); this.resources=resources.withNote(note); this.attempts=attempts; this.pairAttempts=pairAttempts; }
        public List<S> getStops() { return stops; }
        public RunTransportResources getResources() { return resources; }
        public int getAttempts() { return attempts; }
        public int getPairAttempts() { return pairAttempts; }
    }
    public <S extends RunTravelStop> Result<S> repair(List<S> stops,
            Function<S,List<VerifiedTransportResult>> alternatives, BiFunction<S,VerifiedTransportResult,S> replace,
            RequirementContext context, RuneCost farmingRunes)
    { return repair(stops,alternatives,replace,context,farmingRunes,false); }

    public <S extends RunTravelStop> Result<S> repair(List<S> stops,
            Function<S,List<VerifiedTransportResult>> alternatives,BiFunction<S,VerifiedTransportResult,S> replace,
            RequirementContext context,RuneCost farmingRunes,boolean allowPairs)
    {
        RunTransportResourcePlanner planner=new RunTransportResourcePlanner();
        RunTransportResources baseline=planner.evaluateKnownPools(stops,context,farmingRunes);
        if (covered(stops,baseline)) { return new Result<>(stops,baseline,0,"Shared transport budget is covered; existing arrivals retained."); }
        if (context==null || context.getAccountState()==null || !context.getAccountState().isLoggedIn()
                || stops.stream().anyMatch(stop -> !stop.isLocalWalk() && stop.getRecommended()==null))
        { return new Result<>(stops,baseline,0,"Shared-budget alternatives need complete verified arrivals and account data."); }
        int attempts=0;
        singleSearch:
        for (int i=0;i<stops.size();i++)
        {
            S stop=stops.get(i); if (stop.isLocalWalk()) { continue; }
            List<VerifiedTransportResult> options=HerbTransportSelector.select(alternatives.apply(stop),HerbTransportPreference.CATALOGUE);
            for (VerifiedTransportResult option:options)
            {
                if (HerbTransportSelector.sameOption(option,stop.getRecommended())) { continue; }
                if (attempts==MAX_ATTEMPTS) { break singleSearch; }
                attempts++;
                List<S> candidate=new ArrayList<>(stops); candidate.set(i,replace.apply(stop,option));
                RunTransportResources resources=planner.evaluateKnownPools(candidate,context,farmingRunes);
                if (covered(candidate,resources))
                { return new Result<>(candidate,resources,attempts,"Alternate arrival selected for " + stop.getTravelLabel()
                        + " to cover the shared transport and farming-spell budget. Review withdrawal checks before travel."); }
            }
        }
        if(allowPairs) { return repairPairs(stops,alternatives,replace,context,farmingRunes,baseline,attempts); }
        return failed(stops,baseline,attempts);
    }

    private <S extends RunTravelStop> Result<S> repairPairs(List<S> stops,
            Function<S,List<VerifiedTransportResult>> alternatives,BiFunction<S,VerifiedTransportResult,S> replace,
            RequirementContext context,RuneCost farmingRunes,RunTransportResources baseline,int singleAttempts)
    {
        List<List<VerifiedTransportResult>> choices=new ArrayList<>();
        for(S stop:stops)
        {
            List<VerifiedTransportResult> bounded=new ArrayList<>();
            if(!stop.isLocalWalk())
            {
                for(VerifiedTransportResult option:HerbTransportSelector.select(alternatives.apply(stop),HerbTransportPreference.CATALOGUE))
                {
                    if(HerbTransportSelector.sameOption(option,stop.getRecommended())
                            || bounded.stream().anyMatch(known -> HerbTransportSelector.sameOption(known,option))) { continue; }
                    bounded.add(option); if(bounded.size()==MAX_PAIR_ALTERNATIVES_PER_STOP) { break; }
                }
            }
            choices.add(bounded);
        }
        RunTransportResourcePlanner planner=new RunTransportResourcePlanner(); int pairs=0;
        for(int i=0;i<stops.size();i++)
        {
            for(int j=i+1;j<stops.size();j++)
            {
                for(VerifiedTransportResult first:choices.get(i))
                {
                    for(VerifiedTransportResult second:choices.get(j))
                    {
                        if(pairs==MAX_PAIR_ATTEMPTS) { return failedPairs(stops,baseline,singleAttempts,pairs); }
                        pairs++; List<S> candidate=new ArrayList<>(stops);
                        candidate.set(i,replace.apply(stops.get(i),first)); candidate.set(j,replace.apply(stops.get(j),second));
                        RunTransportResources resources=planner.evaluateKnownPools(candidate,context,farmingRunes);
                        if(covered(candidate,resources))
                        {
                            return new Result<>(candidate,resources,singleAttempts+pairs,pairs,
                                    "Alternate arrivals selected for " + stops.get(i).getTravelLabel()+" and "+stops.get(j).getTravelLabel()
                                            +" to cover the shared transport and farming-spell budget. Review withdrawal checks before travel.");
                        }
                    }
                }
            }
        }
        return failedPairs(stops,baseline,singleAttempts,pairs);
    }
    private <S extends RunTravelStop> Result<S> failedPairs(List<S> stops,RunTransportResources baseline,int singles,int pairs)
    {
        return new Result<>(stops,baseline,singles+pairs,pairs,
                "No covered one/two-arrival alternative found within the bounded search (32 single candidates, 64 pairs, four alternatives per stop for pairs). Existing arrivals and blockers retained; more changes or different supplies may be needed.");
    }
    private <S extends RunTravelStop> Result<S> failed(List<S> stops,RunTransportResources baseline,int attempts)
    { return new Result<>(stops,baseline,attempts,"No covered single-arrival alternative found. Existing arrivals and blockers retained; multiple changes may be needed."); }
    private boolean covered(List<? extends RunTravelStop> stops,RunTransportResources resources)
    {
        return stops.stream().allMatch(stop -> stop.isLocalWalk() || stop.getRecommended()!=null && stop.getRecommended().isUsableForPlannedRun())
                && resources.getChecks().stream().allMatch(check -> check.isSatisfied())
                && resources.getConsumables().stream().noneMatch(check -> check.getState()==SupplyState.MISSING || check.getState()==SupplyState.DATA_REQUIRED);
    }
}
