package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenRunPlannerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final GardenRunPlanner planner = new GardenRunPlanner();
    private AccountState account(int boosted) { return new AccountState(true, 1, boosted, EnumSet.noneOf(Quest.class), false); }
    private GardenPatchObservation observation(GardenFamily family, int raw)
    {
        return new GardenPatchObservation(new GardenPatchStateDecoder().decode(family, raw), now, FarmingGrowthRate.NORMAL);
    }
    private GardenRunPlan plan(GardenRunCrop crop, GardenRunMode mode, Map<GardenPatchDefinition, GardenPatchObservation> observations)
    {
        return planner.plan(account(99), EnumSet.allOf(GardenPatchDefinition.class), crop, mode, true, observations, null, now);
    }
    private SupplyCheck supply(GardenRunPlan plan, int id)
    {
        return plan.getSupplies().stream().filter(check -> check.getRequirement().getItemId() == id).findFirst().get();
    }
    @Test public void onlySelectedMatchingAccessiblePatchesAreIncluded()
    {
        GardenRunPlan result = plan(GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE, Map.of());
        assertEquals(4, result.getStops().size()); assertEquals(1, result.getExcluded().size());
        assertTrue(result.getExcluded().get(GardenPatchDefinition.ETCETERIA_BUSH).contains("Data required"));
        assertEquals(4, supply(result, ItemID.REDBERRY_BUSH_SEED).getRequirement().getQuantity());
        assertEquals(16, supply(result, ItemID.SACK_CABBAGE_10).getRequirement().getQuantity());
        GardenRunPlan selected = planner.plan(account(99), EnumSet.of(GardenPatchDefinition.FARMING_GUILD_BUSH, GardenPatchDefinition.FARMING_GUILD_CACTUS),
                GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE, true, Map.of(), null, now);
        assertEquals(1, selected.getStops().size());
    }
    @Test public void loggedOutAndBoostedPlantingLevelGateThePlan()
    {
        assertFalse(planner.plan(AccountState.loggedOut(), EnumSet.allOf(GardenPatchDefinition.class), GardenRunCrop.CACTUS,
                GardenRunMode.KEEP_MATURE, true, Map.of(), null, now).getBlocker().isEmpty());
        assertTrue(planner.plan(account(54), EnumSet.allOf(GardenPatchDefinition.class), GardenRunCrop.CACTUS,
                GardenRunMode.KEEP_MATURE, true, Map.of(), null, now).getStops().isEmpty());
        assertEquals(2, planner.plan(account(55), EnumSet.allOf(GardenPatchDefinition.class), GardenRunCrop.CACTUS,
                GardenRunMode.KEEP_MATURE, true, Map.of(), null, now).getStops().size());
    }
    @Test public void guildBeginnerEntryUsesBoosted45ForBothPatches()
    {
        GardenPatchAccess access = new GardenPatchAccess();
        for (GardenPatchDefinition patch : List.of(GardenPatchDefinition.FARMING_GUILD_BUSH, GardenPatchDefinition.FARMING_GUILD_CACTUS))
        {
            assertFalse(access.missingRequirement(patch, account(44)).isEmpty()); assertEquals("", access.missingRequirement(patch, account(45)));
        }
        assertEquals("", access.missingRequirement(GardenPatchDefinition.CHAMPIONS_GUILD_BUSH, account(10)));
    }
    @Test public void keptMatureAndUncheckedPlantsDoNotReserveSeedsOrProtection()
    {
        for (int raw : new int[]{250, 10, 14})
        {
            GardenRunPlan result = planner.plan(account(99), EnumSet.of(GardenPatchDefinition.RIMMINGTON_BUSH), GardenRunCrop.WHITEBERRIES,
                    GardenRunMode.KEEP_MATURE, true, Map.of(GardenPatchDefinition.RIMMINGTON_BUSH, observation(GardenFamily.BUSH, raw)), null, now);
            assertEquals(1, result.getStops().size()); assertFalse(result.getStops().get(0).isReplant());
            assertTrue(result.getStops().get(0).getAction().contains("keep"));
            assertEquals(1, result.getSupplies().size()); assertEquals(ItemID.SPADE, result.getSupplies().get(0).getRequirement().getItemId());
        }
    }
    @Test public void explicitReplacementBudgetsOneSeedAndExactPaymentPerCactus()
    {
        GardenRunPlan result = plan(GardenRunCrop.CACTUS, GardenRunMode.REPLACE_MATURE, Map.of(
                GardenPatchDefinition.AL_KHARID_CACTUS, observation(GardenFamily.CACTUS, 31),
                GardenPatchDefinition.FARMING_GUILD_CACTUS, observation(GardenFamily.CACTUS, 15)));
        assertEquals(2, supply(result, ItemID.CACTUS_SEED).getRequirement().getQuantity());
        assertEquals(12, supply(result, ItemID.CADAVABERRIES).getRequirement().getQuantity());
        assertTrue(supply(result, ItemID.CADAVABERRIES).getRequirement().getAcceptedItemIds().contains(ItemID.Cert.CADAVABERRIES));
        assertTrue(result.getStops().stream().allMatch(GardenRunStop::isReplant));
    }
    @Test public void diseasedBushesPruneAndCactiUseCureWithoutReplacementSeeds()
    {
        GardenRunPlan bush = planner.plan(account(99), EnumSet.of(GardenPatchDefinition.RIMMINGTON_BUSH), GardenRunCrop.REDBERRIES,
                GardenRunMode.REPLACE_MATURE, true, Map.of(GardenPatchDefinition.RIMMINGTON_BUSH, observation(GardenFamily.BUSH, 70)), null, now);
        assertEquals(2, bush.getSupplies().size()); assertFalse(bush.getStops().get(0).isReplant());
        assertTrue(supply(bush, ItemID.SECATEURS).getRequirement().getAcceptedItemIds().contains(ItemID.FAIRY_ENCHANTED_SECATEURS));
        GardenRunPlan cacti = plan(GardenRunCrop.CACTUS, GardenRunMode.REPLACE_MATURE, Map.of(
                GardenPatchDefinition.AL_KHARID_CACTUS, observation(GardenFamily.CACTUS, 19),
                GardenPatchDefinition.FARMING_GUILD_CACTUS, observation(GardenFamily.CACTUS, 20)));
        assertEquals(2, cacti.getSupplies().size()); assertEquals(2, supply(cacti, ItemID.PLANT_CURE).getRequirement().getQuantity());
    }
    @Test public void growingExclusionEndsAtConditionalEarliestWithoutClaimingReadiness()
    {
        GardenPatchDefinition patch = GardenPatchDefinition.RIMMINGTON_BUSH;
        GardenPatchObservation growing = observation(GardenFamily.BUSH, 5);
        Instant earliest = now.plusSeconds(80 * 60);
        assertTrue(planner.plan(account(99), EnumSet.of(patch), GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE,
                true, Map.of(patch, growing), null, earliest.minusSeconds(1)).getStops().isEmpty());
        assertTrue(planner.plan(account(99), EnumSet.of(patch), GardenRunCrop.REDBERRIES, GardenRunMode.KEEP_MATURE,
                true, Map.of(patch, growing), null, earliest).getStops().get(0).getAction().contains("Inspect first"));
    }
    @Test public void futureAndWrongFamilyRecordsCannotSuppressReplacementSupplies()
    {
        GardenPatchDefinition patch = GardenPatchDefinition.AL_KHARID_CACTUS;
        for (GardenPatchObservation invalid : List.of(observation(GardenFamily.BUSH, 250), new GardenPatchObservation(
                new GardenPatchStateDecoder().decode(GardenFamily.CACTUS, 31), now.plusSeconds(1), FarmingGrowthRate.NORMAL)))
        {
            GardenRunPlan result = planner.plan(account(99), EnumSet.of(patch), GardenRunCrop.CACTUS, GardenRunMode.KEEP_MATURE, true, Map.of(patch, invalid), null, now);
            assertEquals(1, supply(result, ItemID.CACTUS_SEED).getRequirement().getQuantity());
        }
    }
    @Test public void poisonIvyHasNoInventedFarmerPayment()
    {
        GardenRunPlan result = plan(GardenRunCrop.POISON_IVY, GardenRunMode.REPLACE_MATURE, Map.of());
        assertEquals(4, result.getSupplies().size()); assertFalse(GardenRunCrop.POISON_IVY.hasFarmerProtection());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("no farmer payment")));
    }
    @Test public void fullSackPaymentsAcceptNotedSacksButRejectLooseCabbages()
    {
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.SACK_CABBAGE_10, 2L, ItemID.Cert.SACK_CABBAGE_10, 2L), Map.of(), Map.of(), true, now);
        GardenRunPlan result = planner.plan(account(99), EnumSet.of(GardenPatchDefinition.RIMMINGTON_BUSH), GardenRunCrop.REDBERRIES,
                GardenRunMode.REPLACE_MATURE, true, Map.of(), storage, now);
        assertEquals(SupplyState.ALREADY_CARRIED, supply(result, ItemID.SACK_CABBAGE_10).getState());
        assertFalse(supply(result, ItemID.SACK_CABBAGE_10).getRequirement().getAcceptedItemIds().contains(ItemID.CABBAGE));
    }
}
