package com.farmingnavigator.guided;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchRegion;
import com.farmingnavigator.farming.state.HerbPatchStateDecoder;
import com.farmingnavigator.routing.run.HerbRunCompost;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbRunPlanner;
import com.farmingnavigator.transport.TransportCategory;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationKind;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuidedRunSessionTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final WorldPoint falador = HerbPatchDestination.FALADOR.getLocation();

    @Test
    public void harvestingCompostConfirmationAndObservedPlantingCompleteStop()
    {
        GuidedRunSession session = start(HerbRunCompost.ULTRACOMPOST, false);
        assertEquals(GuidedRunStep.PREPARE, session.snapshot().getStep());
        assertFalse(observe(session, 36, 1));
        assertTrue(session.confirm(now.plusSeconds(1)));
        assertTrue(observe(session, 36, 2));
        assertEquals(GuidedRunStep.HARVEST, session.snapshot().getStep());
        assertFalse(observe(session, 37, 3));
        assertTrue(observe(session, 0, 4));
        assertEquals(GuidedRunStep.COMPOST, session.snapshot().getStep());
        assertFalse(observe(session, 0, 5));
        assertTrue(session.confirm(now.plusSeconds(6)));
        assertEquals(GuidedRunStep.PLANT, session.snapshot().getStep());
        assertTrue(observe(session, 32, 7));
        assertEquals(GuidedRunStep.COMPLETE, session.snapshot().getStep());
        assertEquals(1, session.snapshot().getCompleted());
        assertEquals(0, session.snapshot().getManuallyCompleted());
    }

    @Test
    public void coalescedHarvestAndReplantStillRequireCorrectNewGrowingCrop()
    {
        GuidedRunSession session = start(HerbRunCompost.ULTRACOMPOST, false);
        session.confirm(now.plusSeconds(1));
        observe(session, 36, 2);
        assertFalse(observe(session, 4, 3));
        assertTrue(observe(session, 32, 4));
        assertEquals(GuidedRunStep.COMPLETE, session.snapshot().getStep());
        assertEquals(0, session.snapshot().getManuallyCompleted());
    }

    @Test
    public void staleFutureFarAwayAndWrongFloorDataCannotCompletePlanting()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        session.confirm(now.plusSeconds(1));
        observe(session, 0, 2);
        assertEquals(GuidedRunStep.PLANT, session.snapshot().getStep());
        assertFalse(session.observe(falador, observation(32, now), now.plusSeconds(3)));
        assertFalse(session.observe(falador, observation(32, now.plusSeconds(10)), now.plusSeconds(3)));
        assertFalse(session.observe(new WorldPoint(3200, 3200, 0), observation(32, now.plusSeconds(3)), now.plusSeconds(3)));
        assertFalse(session.observe(new WorldPoint(falador.getX(), falador.getY(), 1),
                observation(32, now.plusSeconds(3)), now.plusSeconds(3)));
        assertEquals(GuidedRunStep.PLANT, session.snapshot().getStep());
    }

    @Test
    public void wrongCropAndEstimatedTimeCannotProvePlanting()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        session.confirm(now.plusSeconds(1));
        observe(session, 0, 2);
        assertFalse(observe(session, 4, 3));
        assertEquals(GuidedRunStep.PLANT, session.snapshot().getStep());
        session.observe(falador, observation(32, now), now.plusSeconds(24 * 60 * 60));
        assertEquals(GuidedRunStep.PLANT, session.snapshot().getStep());
    }

    @Test
    public void weedsAndDeadHerbsNeedObservedEmptyPatch()
    {
        for (int raw : new int[] {3, 170})
        {
            GuidedRunSession session = start(HerbRunCompost.NONE, false);
            session.confirm(now.plusSeconds(1));
            observe(session, raw, 2);
            assertEquals(raw == 3 ? GuidedRunStep.RAKE : GuidedRunStep.CLEAR, session.snapshot().getStep());
            observe(session, 0, 3);
            assertEquals(GuidedRunStep.PLANT, session.snapshot().getStep());
        }
    }

    @Test
    public void diseasedHerbCompletesOnlyWhenSameCropIsObservedHealthy()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        session.confirm(now.plusSeconds(1));
        observe(session, 140, 2);
        assertEquals(GuidedRunStep.CURE, session.snapshot().getStep());
        assertFalse(observe(session, 4, 3));
        assertTrue(observe(session, 33, 4));
        assertEquals(GuidedRunStep.COMPLETE, session.snapshot().getStep());
    }

    @Test
    public void growingPatchWaitsUntilActualHarvestableObservation()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        session.confirm(now.plusSeconds(1));
        observe(session, 32, 2);
        assertEquals(GuidedRunStep.WAIT, session.snapshot().getStep());
        assertFalse(observe(session, 35, 3));
        assertTrue(observe(session, 36, 4));
        assertEquals(GuidedRunStep.HARVEST, session.snapshot().getStep());
    }

    @Test
    public void unknownStateReturnsToInspection()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        session.confirm(now.plusSeconds(1));
        observe(session, 36, 2);
        observe(session, 220, 3);
        assertEquals(GuidedRunStep.INSPECT, session.snapshot().getStep());
        observe(session, 36, 4);
        assertEquals(GuidedRunStep.HARVEST, session.snapshot().getStep());
    }

    @Test
    public void skipsAndManualCompletionsAreDistinguishedFromObservedWork()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, true);
        assertFalse(session.skip(now));
        assertFalse(session.markComplete(now));
        session.confirm(now.plusSeconds(1));
        session.skip(now.plusSeconds(2));
        assertEquals(HerbPatchDefinition.CATHERBY, session.snapshot().getStop().getPatch());
        session.markComplete(now.plusSeconds(3));
        assertEquals(GuidedRunStep.COMPLETE, session.snapshot().getStep());
        assertEquals(1, session.snapshot().getSkipped());
        assertEquals(1, session.snapshot().getCompleted());
        assertEquals(1, session.snapshot().getManuallyCompleted());
    }

    @Test
    public void fullInventoryPausesHarvestAndFreedSlotResumesSameEvidenceWindow()
    {
        GuidedRunSession session = start(HerbRunCompost.ULTRACOMPOST, false);
        session.confirm(now.plusSeconds(1));
        observe(session, 36, 2);
        assertFalse(session.observeInventory(-1, falador, now.plusSeconds(3)));
        assertFalse(session.observeInventory(0, new WorldPoint(3200, 3200, 0), now.plusSeconds(3)));
        assertTrue(session.observeInventory(0, falador, now.plusSeconds(3)));
        assertEquals(GuidedRunStep.NOTE_PRODUCE, session.snapshot().getStep());
        assertFalse(session.observeInventory(-1, falador, now.plusSeconds(4)));
        assertTrue(session.observeInventory(1, falador, now.plusSeconds(6)));
        assertEquals(GuidedRunStep.HARVEST, session.snapshot().getStep());
        assertTrue(session.observe(falador, observation(0, now.plusSeconds(5)), now.plusSeconds(6)));
        assertEquals(GuidedRunStep.COMPOST, session.snapshot().getStep());
    }

    @Test
    public void observedEmptyPatchCanEndInventoryInterruptionWithoutGuessingSpace()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        session.confirm(now.plusSeconds(1));
        observe(session, 36, 2);
        session.observeInventory(0, falador, now.plusSeconds(3));
        assertTrue(observe(session, 0, 4));
        assertEquals(GuidedRunStep.PLANT, session.snapshot().getStep());
        assertFalse(session.observeInventory(0, falador, now.plusSeconds(5)));
    }

    @Test
    public void cancellationClearsSessionAndEmptyPlanCannotStart()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        session.cancel();
        assertEquals(GuidedRunStep.INACTIVE, session.snapshot().getStep());
        assertEquals(0, session.snapshot().getTotal());
        assertFalse(session.start(null, HerbRunCompost.NONE, now));
    }

    @Test
    public void routeRefreshDropsExhaustedDailyTeleportAndKeepsUsableFallback()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        VerifiedTransportResult exhausted = route(1, VerifiedTransportState.MISSING);
        VerifiedTransportResult ready = route(2, VerifiedTransportState.READY_NOW);
        session.refreshTransports(Collections.singletonMap(HerbPatchDefinition.FALADOR, Arrays.asList(exhausted, ready)));
        assertEquals(2, session.snapshot().getStop().getRecommended().getOption().getBaselineRank());
    }

    @Test
    public void guidedRefreshKeepsReusableTransportPreference()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        VerifiedTransportResult ring = new VerifiedTransportResult(new TransportOption(9, "Unlimited ring", "Teleport",
                TransportCategory.DIARY_ITEM, TransportVerificationKind.EXPLORER_RING_UNLIMITED),
                VerifiedTransportState.AVAILABLE_FROM_BANK, "");
        VerifiedTransportResult ready = new VerifiedTransportResult(new TransportOption(1, "Tablet", "Teleport",
                TransportCategory.ITEM, TransportVerificationKind.ARDOUGNE_TELEPORT_TABLET),
                VerifiedTransportState.READY_NOW, "");
        session.refreshTransports(Collections.singletonMap(HerbPatchDefinition.FALADOR, Arrays.asList(ready, ring)),
                com.farmingnavigator.routing.run.HerbTransportPreference.REUSABLE_FIRST);
        assertEquals(ring, session.snapshot().getStop().getRecommended());
        assertEquals(ready, session.snapshot().getStop().getFallback());
    }

    @Test
    public void elapsedIncludesPreparationAndFreezesWhenRunCompletes()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        assertEquals(60, session.snapshot(now.plusSeconds(60)).getElapsed().getSeconds());
        session.confirm(now.plusSeconds(61));
        session.markComplete(now.plusSeconds(120));
        assertEquals(GuidedRunStep.COMPLETE, session.snapshot().getStep());
        assertEquals(120, session.snapshot(now.plusSeconds(999)).getElapsed().getSeconds());
        assertEquals(1, session.snapshot().getManuallyCompleted());
        session.cancel();
        assertEquals(0, session.snapshot(now.plusSeconds(1000)).getElapsed().getSeconds());
    }

    @Test
    public void clockBeforeRunStartDoesNotShowNegativeDuration()
    {
        GuidedRunSession session = start(HerbRunCompost.NONE, false);
        assertEquals(0, session.snapshot(now.minusSeconds(60)).getElapsed().getSeconds());
        session.confirm(now.plusSeconds(1));
        session.skip(now.plusSeconds(10));
        assertEquals(10, session.snapshot(now.plusSeconds(500)).getElapsed().getSeconds());
        assertEquals(1, session.snapshot().getSkipped());
    }

    @Test
    public void everyDestinationUsesCorrectRegionAndRequiresNearbyGroundFloor()
    {
        for (HerbPatchDestination destination : HerbPatchDestination.values())
        {
            HerbPatchDefinition patch = HerbPatchDefinition.valueOf(destination.name());
            WorldPoint anchor = destination.getLocation();
            assertTrue(HerbPatchDestination.isNear(patch, anchor));
            assertFalse(HerbPatchDestination.isNear(patch, new WorldPoint(anchor.getX() + 13, anchor.getY(), 0)));
            assertEquals(patch, HerbPatchRegion.forLocation(anchor).getPatch());
        }
    }

    @Test public void confirmedCompostSurvivesUnknownEvidenceButDoesNotCarryToNextStop()
    {
        GuidedRunSession session=start(HerbRunCompost.ULTRACOMPOST,true);
        session.confirm(now.plusSeconds(1)); observe(session,0,2); session.confirm(now.plusSeconds(3));
        observe(session,220,4); assertEquals(GuidedRunStep.INSPECT,session.snapshot().getStep());
        observe(session,0,5); assertEquals(GuidedRunStep.PLANT,session.snapshot().getStep());
        session.skip(now.plusSeconds(6));
        HerbPatchObservation next=new HerbPatchObservation(new HerbPatchStateDecoder().decode(0),now.plusSeconds(7),FarmingGrowthRate.NORMAL);
        session.observe(HerbPatchDestination.CATHERBY.getLocation(),Collections.singletonMap(HerbPatchDefinition.CATHERBY,next),now.plusSeconds(7));
        assertEquals(GuidedRunStep.COMPOST,session.snapshot().getStep());
    }
    @Test public void cureOnlyStopBecomingEmptyOrWeedyCannotRequestUnreservedReplant()
    {
        for(int raw:new int[]{0,3})
        {
            GuidedRunSession session=start(HerbRunCompost.ULTRACOMPOST,false,observation(140,now));
            assertFalse(session.snapshot().getStop().isReplant()); session.confirm(now.plusSeconds(1));
            observe(session,140,2); assertEquals(GuidedRunStep.CURE,session.snapshot().getStep());
            observe(session,220,3); observe(session,raw,4);
            assertEquals(GuidedRunStep.COMPLETE,session.snapshot().getStep());
            assertEquals(0,session.snapshot().getManuallyCompleted());
        }
    }
    @Test public void cureCanFinishOnSameCropObservedHealthyAndHarvestable()
    {
        GuidedRunSession session=start(HerbRunCompost.NONE,false,observation(140,now));
        session.confirm(now.plusSeconds(1)); observe(session,140,2);
        assertTrue(observe(session,36,3)); assertEquals(GuidedRunStep.COMPLETE,session.snapshot().getStep());
    }
    @Test public void observationAgeHasInclusiveThirtySecondLimitDuringActions()
    {
        GuidedRunSession session=start(HerbRunCompost.NONE,false);
        session.confirm(now.plusSeconds(1)); observe(session,0,2);
        assertFalse(session.observe(falador,observation(32,now.plusSeconds(3)),now.plusSeconds(34)));
        assertEquals(GuidedRunStep.PLANT,session.snapshot().getStep());
        assertTrue(session.observe(falador,observation(32,now.plusSeconds(4)),now.plusSeconds(34)));
        assertEquals(GuidedRunStep.COMPLETE,session.snapshot().getStep());
    }
    @Test public void resumedHarvestRejectsCommandsFromInventoryInterruptionRevision()
    {
        GuidedRunSession session=start(HerbRunCompost.NONE,false); session.confirm(now.plusSeconds(1)); observe(session,36,2);
        session.observeInventory(0,falador,now.plusSeconds(3)); long paused=session.getRevision();
        session.observeInventory(1,falador,now.plusSeconds(4));
        assertTrue(session.getRevision()>paused);
        assertFalse(ChecklistCommandGuard.dispatch(paused,session.getRevision(),() -> session.skip(now.plusSeconds(5))));
        assertEquals(GuidedRunStep.HARVEST,session.snapshot().getStep());
    }

    private GuidedRunSession start(HerbRunCompost compost, boolean twoStops)
    {
        return start(compost,twoStops,Collections.emptyMap());
    }
    private GuidedRunSession start(HerbRunCompost compost, boolean twoStops,
            java.util.Map<HerbPatchDefinition,HerbPatchObservation> observations)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
        HerbRunPlan plan = new HerbRunPlanner().plan(account, Arrays.asList(
                new HerbPatchStatus(HerbPatchDefinition.FALADOR, Collections.emptyList()),
                new HerbPatchStatus(HerbPatchDefinition.CATHERBY, Collections.emptyList())),
                twoStops ? EnumSet.of(HerbPatchDefinition.FALADOR, HerbPatchDefinition.CATHERBY) : EnumSet.of(HerbPatchDefinition.FALADOR),
                HerbRunCrop.RANARR, compost, observations, Collections.emptyMap(), null, now);
        GuidedRunSession session = new GuidedRunSession();
        assertTrue(session.start(plan, compost, now));
        return session;
    }

    private boolean observe(GuidedRunSession session, int raw, int seconds)
    {
        return session.observe(falador, observation(raw, now.plusSeconds(seconds)), now.plusSeconds(seconds));
    }

    private java.util.Map<HerbPatchDefinition, HerbPatchObservation> observation(int raw, Instant timestamp)
    {
        return Collections.singletonMap(HerbPatchDefinition.FALADOR, new HerbPatchObservation(
                new HerbPatchStateDecoder().decode(raw), timestamp, FarmingGrowthRate.NORMAL));
    }

    private VerifiedTransportResult route(int rank, VerifiedTransportState state)
    {
        return new VerifiedTransportResult(new TransportOption(rank, "Route " + rank, "Travel manually", TransportCategory.ITEM,
                TransportVerificationKind.ECTOPHIAL), state, "");
    }
}
