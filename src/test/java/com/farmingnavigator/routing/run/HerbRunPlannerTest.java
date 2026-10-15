package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchStateDecoder;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.TransportCategory;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationKind;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.skillcalculator.skills.FarmingAction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HerbRunPlannerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final List<HerbPatchStatus> patches = Arrays.asList(
            status(HerbPatchDefinition.FALADOR), status(HerbPatchDefinition.CATHERBY),
            new HerbPatchStatus(HerbPatchDefinition.WEISS, Collections.singletonList("Fire of Nourishment required")));

    @Test
    public void cropLevelsMatchRuneLiteAndSeedIdsAreDistinct()
    {
        Set<Integer> ids = new HashSet<>();
        for (HerbRunCrop crop : HerbRunCrop.values())
        {
            String action = crop.name();
            if (crop == HerbRunCrop.GUAM) { action = "GUAM_LEAF"; }
            if (crop == HerbRunCrop.RANARR) { action = "RANARR_WEED"; }
            if (crop == HerbRunCrop.IRIT) { action = "IRIT_LEAF"; }
            assertEquals(FarmingAction.valueOf(action).getLevel(), crop.getRequiredLevel());
            assertTrue(ids.add(crop.getSeedId()));
        }
        assertEquals(15, ids.size());
    }

    @Test
    public void excludesLockedAndUnselectedPatchesAndCountsOneSeedPerStop()
    {
        HerbRunPlan plan = plan(Collections.emptyMap(), Collections.emptyMap(), now);
        assertEquals(2, plan.getStops().size());
        assertEquals("Fire of Nourishment required", plan.getExcluded().get(HerbPatchDefinition.WEISS));
        assertEquals(2, supply(plan, ItemID.RANARR_SEED).getRequirement().getQuantity());
        assertTrue(plan.getStops().get(0).getAction().contains("Inspect"));
        assertEquals(SupplyState.DATA_REQUIRED, supply(plan, ItemID.RANARR_SEED).getState());
        HerbRunPlan selected = new HerbRunPlanner().plan(account(99, 99), patches,
                EnumSet.of(HerbPatchDefinition.CATHERBY), HerbRunCrop.RANARR, HerbRunCompost.ULTRACOMPOST,
                Collections.emptyMap(), Collections.emptyMap(), null, now);
        assertEquals(1, selected.getStops().size());
        assertEquals(HerbPatchDefinition.CATHERBY, selected.getStops().get(0).getPatch());
        assertEquals(1, supply(selected, ItemID.RANARR_SEED).getRequirement().getQuantity());
    }

    @Test
    public void cropLevelLocksPlanButAnObservedBoostPermitsPlanting()
    {
        HerbRunPlan locked = new HerbRunPlanner().plan(account(31, 31), patches, EnumSet.allOf(HerbPatchDefinition.class),
                HerbRunCrop.RANARR, HerbRunCompost.NONE, Collections.emptyMap(), Collections.emptyMap(), null, now);
        assertTrue(locked.getStops().isEmpty());
        assertTrue(locked.getSupplies().isEmpty());
        assertTrue(locked.getBlocker().contains("32 Farming"));
        HerbRunPlan boosted = new HerbRunPlanner().plan(account(31, 32), patches, EnumSet.allOf(HerbPatchDefinition.class),
                HerbRunCrop.RANARR, HerbRunCompost.NONE, Collections.emptyMap(), Collections.emptyMap(), null, now);
        assertEquals(2, boosted.getStops().size());
    }

    @Test
    public void readyTransportWinsOverBankedAndLockedOptionsCannotBeFallback()
    {
        List<VerifiedTransportResult> options = Arrays.asList(route(1, VerifiedTransportState.AVAILABLE_FROM_BANK),
                route(2, VerifiedTransportState.LOCKED), route(5, VerifiedTransportState.READY_NOW),
                route(3, VerifiedTransportState.READY_NOW));
        HerbRunStop stop = plan(Collections.emptyMap(), Collections.singletonMap(HerbPatchDefinition.FALADOR, options), now)
                .getStops().get(0);
        assertEquals(3, stop.getRecommended().getOption().getBaselineRank());
        assertEquals(5, stop.getFallback().getOption().getBaselineRank());
    }

    @Test
    public void bankedFallbackIsAllowedAndUnknownRouteIsNotClaimedUsable()
    {
        List<VerifiedTransportResult> options = Arrays.asList(route(1, VerifiedTransportState.DATA_REQUIRED),
                route(2, VerifiedTransportState.READY_NOW), route(3, VerifiedTransportState.AVAILABLE_FROM_BANK));
        HerbRunPlan plan = plan(Collections.emptyMap(), Collections.singletonMap(HerbPatchDefinition.FALADOR, options), now);
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, plan.getStops().get(0).getFallback().getState());
        assertNull(plan.getStops().get(1).getRecommended());
    }

    @Test
    public void growingPatchWaitsUntilEstimatedWindowThenRequiresInspection()
    {
        Map<HerbPatchDefinition, HerbPatchObservation> observed = Collections.singletonMap(
                HerbPatchDefinition.FALADOR, observation(32));
        HerbRunPlan early = plan(observed, Collections.emptyMap(), now);
        assertEquals(1, early.getStops().size());
        assertTrue(early.getExcluded().get(HerbPatchDefinition.FALADOR).contains("growing"));
        HerbRunPlan later = plan(observed, Collections.emptyMap(), now.plusSeconds(60 * 60));
        assertEquals(2, later.getStops().size());
        assertTrue(later.getStops().get(0).getAction().contains("Inspect"));
    }

    @Test
    public void diseasedStopNeedsCureWithoutSeedOrCompostForThatStop()
    {
        HerbRunPlan plan = plan(Collections.singletonMap(HerbPatchDefinition.FALADOR, observation(140)), Collections.emptyMap(), now);
        assertFalse(plan.getStops().get(0).isReplant());
        assertTrue(plan.getStops().get(0).getAction().contains("cure"));
        assertEquals(1, supply(plan, ItemID.RANARR_SEED).getRequirement().getQuantity());
        assertEquals(1, supply(plan, ItemID.BUCKET_ULTRACOMPOST).getRequirement().getQuantity());
        assertEquals(1, supply(plan, ItemID.PLANT_CURE).getRequirement().getQuantity());
    }

    @Test
    public void compostChoiceAndOptionalConveniencesRemainExplicit()
    {
        HerbRunPlan plan = new HerbRunPlanner().plan(account(99, 99), patches, EnumSet.allOf(HerbPatchDefinition.class),
                HerbRunCrop.RANARR, HerbRunCompost.NONE, Collections.emptyMap(), Collections.emptyMap(), null, now);
        assertFalse(plan.getSupplies().stream().anyMatch(s -> s.getRequirement().getItemId() == ItemID.BUCKET_ULTRACOMPOST));
        assertTrue(supply(plan, ItemID.DIBBER).getRequirement().isOptional());
        assertTrue(supply(plan, ItemID.DIBBER).getRequirement().getNote().contains("barbarian"));
        assertFalse(supply(plan, ItemID.SPADE).getRequirement().isOptional());
    }

    @Test
    public void noSelectedStopsProduceNoSupplyList()
    {
        HerbRunPlan plan = new HerbRunPlanner().plan(account(99, 99), patches, Collections.emptySet(),
                HerbRunCrop.RANARR, HerbRunCompost.ULTRACOMPOST, Collections.emptyMap(), Collections.emptyMap(), null, now);
        assertTrue(plan.getStops().isEmpty());
        assertTrue(plan.getSupplies().isEmpty());
    }

    @Test
    public void logoutProducesNoPlanOrUnverifiedSupplyClaims()
    {
        HerbRunPlan plan = new HerbRunPlanner().plan(AccountState.loggedOut(), patches, EnumSet.allOf(HerbPatchDefinition.class),
                HerbRunCrop.RANARR, HerbRunCompost.NONE, Collections.emptyMap(), Collections.emptyMap(), null, now);
        assertTrue(plan.getStops().isEmpty());
        assertTrue(plan.getBlocker().contains("Log in"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void planStopsAreImmutable()
    {
        plan(Collections.emptyMap(), Collections.emptyMap(), now).getStops().clear();
    }

    private HerbRunPlan plan(Map<HerbPatchDefinition, HerbPatchObservation> observations,
            Map<HerbPatchDefinition, List<VerifiedTransportResult>> transports, Instant time)
    {
        return new HerbRunPlanner().plan(account(99, 99), patches, EnumSet.allOf(HerbPatchDefinition.class),
                HerbRunCrop.RANARR, HerbRunCompost.ULTRACOMPOST, observations, transports, null, time);
    }

    private AccountState account(int real, int boosted)
    {
        return new AccountState(true, real, boosted, 99, 99, 99, 99, 99, 99,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
    }

    private HerbPatchStatus status(HerbPatchDefinition patch)
    {
        return new HerbPatchStatus(patch, Collections.emptyList());
    }

    private HerbPatchObservation observation(int raw)
    {
        return new HerbPatchObservation(new HerbPatchStateDecoder().decode(raw), now, FarmingGrowthRate.NORMAL);
    }

    private SupplyCheck supply(HerbRunPlan plan, int id)
    {
        return plan.getSupplies().stream().filter(s -> s.getRequirement().getItemId() == id).findFirst().get();
    }

    private VerifiedTransportResult route(int rank, VerifiedTransportState state)
    {
        return new VerifiedTransportResult(new TransportOption(rank, "Route " + rank, "Manual travel",
                TransportCategory.ITEM, TransportVerificationKind.ECTOPHIAL), state, state.name());
    }
}
