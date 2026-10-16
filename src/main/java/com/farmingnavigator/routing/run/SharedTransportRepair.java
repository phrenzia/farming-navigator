package com.farmingnavigator.routing.run;

import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RuneCost;
import com.farmingnavigator.transport.VerifiedTransportResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Bounded single-arrival repair, preserving stop order. Not a global route, time or cost optimizer. */
public final class SharedTransportRepair
{
    static final int MAX_ATTEMPTS = 32;
    public static final class Result<S extends RunTravelStop>
    {
        private final List<S> stops;
        private final RunTransportResources resources;
        private final int attempts;
        private Result(List<S> stops, RunTransportResources resources, int attempts, String note)
        { this.stops=List.copyOf(stops); this.resources=resources.withNote(note); this.attempts=attempts; }
        public List<S> getStops() { return stops; }
        public RunTransportResources getResources() { return resources; }
        public int getAttempts() { return attempts; }
    }
    public <S extends RunTravelStop> Result<S> repair(List<S> stops,
            Function<S,List<VerifiedTransportResult>> alternatives, BiFunction<S,VerifiedTransportResult,S> replace,
            RequirementContext context, RuneCost farmingRunes)
    {
        RunTransportResourcePlanner planner=new RunTransportResourcePlanner();
        RunTransportResources baseline=planner.evaluateKnownPools(stops,context,farmingRunes);
        if (covered(stops,baseline)) { return new Result<>(stops,baseline,0,"Shared transport budget is covered; existing arrivals retained."); }
        if (context==null || context.getAccountState()==null || !context.getAccountState().isLoggedIn()
                || stops.stream().anyMatch(stop -> !stop.isLocalWalk() && stop.getRecommended()==null))
        { return new Result<>(stops,baseline,0,"Shared-budget alternatives need complete verified arrivals and account data."); }
        int attempts=0;
        for (int i=0;i<stops.size();i++)
        {
            S stop=stops.get(i); if (stop.isLocalWalk()) { continue; }
            List<VerifiedTransportResult> options=HerbTransportSelector.select(alternatives.apply(stop),HerbTransportPreference.CATALOGUE);
            for (VerifiedTransportResult option:options)
            {
                if (HerbTransportSelector.sameOption(option,stop.getRecommended())) { continue; }
                if (attempts==MAX_ATTEMPTS) { return failed(stops,baseline,attempts); }
                attempts++;
                List<S> candidate=new ArrayList<>(stops); candidate.set(i,replace.apply(stop,option));
                RunTransportResources resources=planner.evaluateKnownPools(candidate,context,farmingRunes);
                if (covered(candidate,resources))
                { return new Result<>(candidate,resources,attempts,"Alternate arrival selected for " + stop.getTravelLabel()
                        + " to cover the shared transport and farming-spell budget. Review withdrawal checks before travel."); }
            }
        }
        return failed(stops,baseline,attempts);
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
