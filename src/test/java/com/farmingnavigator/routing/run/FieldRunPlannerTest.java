package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
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

public class FieldRunPlannerTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final FieldRunPlanner planner = new FieldRunPlanner();
    private AccountState account(int boosted) { return new AccountState(true, 1, boosted, EnumSet.noneOf(Quest.class), false); }
    private AccountState unlocked() { return new AccountState(true, 1, 99, EnumSet.of(Quest.SONG_OF_THE_ELVES, Quest.PRIEST_IN_PERIL, Quest.CHILDREN_OF_THE_SUN, Quest.THE_GREAT_BRAIN_ROBBERY, Quest.BONE_VOYAGE), false); }
    private FieldPatchObservation observation(FieldFamily family, int raw)
    {
        return new FieldPatchObservation(new FieldPatchStateDecoder().decode(family, raw), now, FarmingGrowthRate.NORMAL);
    }
    private FieldRunPlan plan(FieldRunCrop crop, FieldRunMode mode, Map<FieldPatchDefinition, FieldPatchObservation> observations)
    {
        return planner.plan(account(99), EnumSet.of(FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_SOUTH, FieldPatchDefinition.FALADOR_FLOWER), crop, mode, true, observations, null, now);
    }
    private SupplyCheck supply(FieldRunPlan plan, int id)
    {
        return plan.getSupplies().stream().filter(check -> check.getRequirement().getItemId() == id).findFirst().get();
    }
    @Test public void twoAllotmentsReserveSixSeedsAndSeparatePayments()
    {
        FieldRunPlan plan = plan(FieldRunCrop.TOMATO, FieldRunMode.KEEP_MATURE_FLOWERS, Map.of());
        assertEquals(2, plan.getStops().size());
        assertEquals(6, supply(plan, ItemID.TOMATO_SEED).getRequirement().getQuantity());
        assertEquals(4, supply(plan, ItemID.SACK_CABBAGE_10).getRequirement().getQuantity());
        assertFalse(supply(plan, ItemID.SACK_CABBAGE_10).getRequirement().getAcceptedItemIds().contains(ItemID.CABBAGE));
    }
    @Test public void eachCropUsesBoostedPlantingLevelAndCorrectSeedCount()
    {
        for (FieldRunCrop crop : FieldRunCrop.values())
        {
            FieldPatchDefinition patch = Map.of(FieldFamily.ALLOTMENT, FieldPatchDefinition.FALADOR_NORTH,
                    FieldFamily.FLOWER, FieldPatchDefinition.FALADOR_FLOWER, FieldFamily.HOPS, FieldPatchDefinition.LUMBRIDGE_HOPS,
                    FieldFamily.SEAWEED, FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH, FieldFamily.MUSHROOM, FieldPatchDefinition.MORYTANIA_MUSHROOM,
                    FieldFamily.BELLADONNA, FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA).get(crop.getCrop().getFamily());
            assertTrue(planner.plan(account(crop.getRequiredLevel()-1), EnumSet.of(patch), crop, FieldRunMode.HARVEST_ALL, true, Map.of(), null, now).getStops().isEmpty());
            FieldRunPlan result = planner.plan(new AccountState(true, 1, crop.getRequiredLevel(), EnumSet.of(Quest.BONE_VOYAGE, Quest.PRIEST_IN_PERIL), false), EnumSet.of(patch), crop, FieldRunMode.HARVEST_ALL, true, Map.of(), null, now);
            assertEquals(1, result.getStops().size()); assertEquals(crop.getSeedsPerPatch(), supply(result, crop.getSeedId()).getRequirement().getQuantity());
        }
        assertFalse(planner.plan(AccountState.loggedOut(), EnumSet.allOf(FieldPatchDefinition.class), FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, true, Map.of(), null, now).getBlocker().isEmpty());
    }
    @Test public void matureFlowersAreKeptEvenWhenDifferentFromSelectedCrop()
    {
        FieldRunPlan result = plan(FieldRunCrop.LIMPWURT, FieldRunMode.KEEP_MATURE_FLOWERS, Map.of(FieldPatchDefinition.FALADOR_FLOWER, observation(FieldFamily.FLOWER, 41)));
        assertEquals(1, result.getStops().size()); assertFalse(result.getStops().get(0).isReplant());
        assertEquals(1, result.getSupplies().size()); assertTrue(result.getStops().get(0).getAction().contains("keep"));
        FieldRunPlan harvest = plan(FieldRunCrop.LIMPWURT, FieldRunMode.HARVEST_ALL, Map.of(FieldPatchDefinition.FALADOR_FLOWER, observation(FieldFamily.FLOWER, 41)));
        assertTrue(harvest.getStops().get(0).isReplant()); assertEquals(1, supply(harvest, ItemID.LIMPWURT_SEED).getRequirement().getQuantity());
        assertFalse(FieldRunCrop.LIMPWURT.hasFarmerProtection());
    }
    @Test public void matureAllotmentsAlwaysReserveReplacementRegardlessOfFlowerMode()
    {
        FieldRunPlan result = plan(FieldRunCrop.POTATO, FieldRunMode.KEEP_MATURE_FLOWERS, Map.of(FieldPatchDefinition.FALADOR_NORTH, observation(FieldFamily.ALLOTMENT, 10)));
        assertTrue(result.getStops().stream().allMatch(FieldRunStop::isReplant)); assertEquals(6, supply(result, ItemID.POTATO_SEED).getRequirement().getQuantity());
        assertEquals(4, supply(result, ItemID.BUCKET_COMPOST).getRequirement().getQuantity());
    }
    @Test public void scarecrowsAreExcludedWithoutAssumingRemovalOrReplanting()
    {
        for (int raw = 33; raw <= 36; raw++)
        {
            FieldRunPlan result = plan(FieldRunCrop.MARIGOLD, FieldRunMode.HARVEST_ALL, Map.of(FieldPatchDefinition.FALADOR_FLOWER, observation(FieldFamily.FLOWER, raw)));
            assertTrue(result.getStops().isEmpty()); assertTrue(result.getSupplies().isEmpty()); assertTrue(result.getExcluded().get(FieldPatchDefinition.FALADOR_FLOWER).contains("Scarecrow"));
        }
    }
    @Test public void diseasedAllotmentsAndFlowersReserveCureWithoutNewSeeds()
    {
        for (FieldPatchDefinition patch : List.of(FieldPatchDefinition.FALADOR_NORTH, FieldPatchDefinition.FALADOR_FLOWER))
        {
            FieldRunCrop crop = patch.getFamily() == FieldFamily.ALLOTMENT ? FieldRunCrop.POTATO : FieldRunCrop.MARIGOLD;
            FieldRunPlan result = planner.plan(account(99), EnumSet.of(patch), crop, FieldRunMode.HARVEST_ALL, true,
                    Map.of(patch, observation(patch.getFamily(), patch.getFamily() == FieldFamily.ALLOTMENT ? 135 : 137)), null, now);
            assertFalse(result.getStops().get(0).isReplant()); assertEquals(2, result.getSupplies().size()); assertEquals(1, supply(result, ItemID.PLANT_CURE).getRequirement().getQuantity());
        }
    }
    @Test public void futureAndWrongFamilyEvidenceCannotRemovePlantingSupplies()
    {
        FieldPatchDefinition patch = FieldPatchDefinition.FALADOR_FLOWER;
        for (FieldPatchObservation invalid : List.of(observation(FieldFamily.ALLOTMENT, 10), new FieldPatchObservation(new FieldPatchStateDecoder().decode(FieldFamily.FLOWER, 41), now.plusSeconds(1), FarmingGrowthRate.NORMAL)))
        {
            FieldRunPlan result = planner.plan(account(99), EnumSet.of(patch), FieldRunCrop.MARIGOLD, FieldRunMode.KEEP_MATURE_FLOWERS, true, Map.of(patch, invalid), null, now);
            assertEquals(1, supply(result, ItemID.MARIGOLD_SEED).getRequirement().getQuantity());
        }
    }
    @Test public void earliestWindowControlsVisitExclusionWithoutClaimingReadiness()
    {
        FieldPatchDefinition patch = FieldPatchDefinition.FALADOR_NORTH;
        Map<FieldPatchDefinition, FieldPatchObservation> observations = Map.of(patch, observation(FieldFamily.ALLOTMENT, 6));
        assertTrue(planner.plan(account(99), EnumSet.of(patch), FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, true, observations, null, now.plusSeconds(1799)).getStops().isEmpty());
        assertTrue(planner.plan(account(99), EnumSet.of(patch), FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, true, observations, null, now.plusSeconds(1800)).getStops().get(0).getAction().contains("Inspect first"));
    }
    @Test public void gatesAreSpecificToEachRegionAndGuildBeginnerArea()
    {
        FieldPatchAccess access = new FieldPatchAccess();
        for (FieldPatchDefinition patch : FieldPatchDefinition.values())
        {
            if (patch == FieldPatchDefinition.KASTORI_FLOWER || patch == FieldPatchDefinition.ENTRANA_HOPS || patch == FieldPatchDefinition.ALDARIN_HOPS || patch == FieldPatchDefinition.AUBURNVALE_BELLADONNA) { assertTrue(access.missingRequirement(patch, unlocked()).contains("Data required")); continue; }
            assertEquals("", access.missingRequirement(patch, unlocked()));
        }
        assertFalse(access.missingRequirement(FieldPatchDefinition.FARMING_GUILD_NORTH, account(44)).isEmpty());
        assertEquals("", access.missingRequirement(FieldPatchDefinition.FARMING_GUILD_FLOWER, account(45)));
        assertFalse(access.missingRequirement(FieldPatchDefinition.PRIFDDINAS_NORTH, account(99)).isEmpty());
        assertFalse(access.missingRequirement(FieldPatchDefinition.ORTUS_FARM_NORTH, account(99)).isEmpty());
        assertFalse(access.missingRequirement(FieldPatchDefinition.MORYTANIA_NORTH, account(99)).isEmpty());
    }
    @Test public void harmonyHasNoFarmerAndDoesNotRequireHerbDiaryReward()
    {
        FieldRunPlan result = planner.plan(unlocked(), EnumSet.of(FieldPatchDefinition.HARMONY_ISLAND_ALLOTMENT), FieldRunCrop.ONION, FieldRunMode.HARVEST_ALL, true, Map.of(), null, now);
        assertEquals(1, result.getStops().size()); assertEquals(4, result.getSupplies().size());
        assertFalse(result.getSupplies().stream().anyMatch(c -> c.getRequirement().getItemId() == ItemID.SACK_POTATO_10));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("no farmer")));
    }
    @Test public void mixedNotedPaymentsAndBankSeedQuantitiesAreCheckedExactly()
    {
        StorageSnapshot storage = new StorageSnapshot(Map.of(ItemID.SACK_CABBAGE_10, 1L, ItemID.Cert.SACK_CABBAGE_10, 1L, ItemID.TOMATO_SEED, 1L), Map.of(), Map.of(ItemID.TOMATO_SEED, 2L), true, now);
        FieldRunPlan result = planner.plan(account(99), EnumSet.of(FieldPatchDefinition.FALADOR_NORTH), FieldRunCrop.TOMATO, FieldRunMode.HARVEST_ALL, true, Map.of(), storage, now);
        assertEquals(SupplyState.ALREADY_CARRIED, supply(result, ItemID.SACK_CABBAGE_10).getState());
        assertEquals(SupplyState.AVAILABLE_FROM_BANK, supply(result, ItemID.TOMATO_SEED).getState()); assertEquals(2, supply(result, ItemID.TOMATO_SEED).getWithdrawQuantity());
        assertEquals(SupplyState.DATA_REQUIRED, supply(plan(FieldRunCrop.TOMATO, FieldRunMode.HARVEST_ALL, Map.of()), ItemID.TOMATO_SEED).getState());
    }
    @Test public void allEightFarmerPaymentsUseExactContainersAndQuantities()
    {
        FieldRunCrop[] crops = {FieldRunCrop.POTATO, FieldRunCrop.ONION, FieldRunCrop.CABBAGE, FieldRunCrop.TOMATO, FieldRunCrop.SWEETCORN, FieldRunCrop.STRAWBERRY, FieldRunCrop.WATERMELON, FieldRunCrop.SNAPE_GRASS};
        int[] items = {ItemID.BUCKET_COMPOST, ItemID.SACK_POTATO_10, ItemID.SACK_ONION_10, ItemID.SACK_CABBAGE_10, ItemID.JUTE_FIBRE, ItemID.BASKET_APPLE_5, ItemID.CURRY_LEAF, ItemID.JANGERBERRIES};
        int[] quantities = {2, 1, 1, 2, 10, 1, 10, 5};
        for (int i = 0; i < crops.length; i++)
        {
            com.farmingnavigator.loadout.SupplyRequirement payment = crops[i].protectionPayment(2).get();
            assertEquals(items[i], payment.getItemId()); assertEquals(2L * quantities[i], payment.getQuantity());
            assertEquals(2, payment.getAcceptedItemIds().size()); assertFalse(crops[i].protectionPayment(0).isPresent());
        }
    }
    @Test public void protectionCanBeDisabledWithoutDroppingSeeds()
    {
        FieldRunPlan result = planner.plan(account(99), EnumSet.of(FieldPatchDefinition.FALADOR_NORTH), FieldRunCrop.ONION, FieldRunMode.HARVEST_ALL, false, Map.of(), null, now);
        assertEquals(4, result.getSupplies().size()); assertEquals(3, supply(result, ItemID.ONION_SEED).getRequirement().getQuantity());
    }

    @Test public void compostApplicationAndPotatoPaymentDoNotReuseBuckets()
    {
        FieldRunPlan result = planner.plan(account(99), EnumSet.of(FieldPatchDefinition.FALADOR_NORTH), FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL,
                true, FieldRunCompost.COMPOST, Map.of(), new StorageSnapshot(Map.of(ItemID.BUCKET_COMPOST, 2L), Map.of(), Map.of(), true, now), now);
        List<SupplyCheck> compost = new java.util.ArrayList<>();
        result.getSupplies().stream().filter(c -> c.getRequirement().getItemId() == ItemID.BUCKET_COMPOST).forEach(compost::add);
        assertEquals(2, compost.size()); assertEquals(1, compost.get(0).getRequirement().getQuantity());
        assertEquals(SupplyState.ALREADY_CARRIED, compost.get(0).getState()); assertEquals(SupplyState.MISSING, compost.get(1).getState());
        assertEquals(1, compost.get(1).getMissingQuantity()); assertEquals(FieldRunCompost.COMPOST, result.getCompost());
    }
    @Test public void keptFlowersAndDiseaseStopsDoNotReserveCompost()
    {
        for (int raw : new int[]{41,166})
        {
            FieldRunPlan result = planner.plan(account(99), EnumSet.of(FieldPatchDefinition.FALADOR_FLOWER), FieldRunCrop.WHITE_LILY, FieldRunMode.KEEP_MATURE_FLOWERS,
                    true, FieldRunCompost.ULTRACOMPOST, Map.of(FieldPatchDefinition.FALADOR_FLOWER, observation(FieldFamily.FLOWER, raw)), null, now);
            assertFalse(result.getSupplies().stream().anyMatch(c -> c.getRequirement().getItemId() == ItemID.BUCKET_ULTRACOMPOST));
        }
    }

    @Test public void bottomlessVariantsReserveOneFilledBucketAndRequireManualContentsChecks()
    {
        for(FieldRunCompost compost: new FieldRunCompost[]{FieldRunCompost.BOTTOMLESS_COMPOST,FieldRunCompost.BOTTOMLESS_SUPERCOMPOST,FieldRunCompost.BOTTOMLESS_ULTRACOMPOST})
        {
            FieldRunPlan result=planner.plan(account(99),EnumSet.of(FieldPatchDefinition.FALADOR_NORTH,FieldPatchDefinition.FALADOR_SOUTH),FieldRunCrop.TOMATO,
                    FieldRunMode.HARVEST_ALL,true,compost,Map.of(),new StorageSnapshot(Map.of(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED,1L),Map.of(),Map.of(),true,now),now);
            SupplyCheck bucket=supply(result,ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);
            assertEquals(1,bucket.getRequirement().getQuantity()); assertEquals(SupplyState.ALREADY_CARRIED,bucket.getState());
            assertTrue(bucket.getRequirement().getNote().contains("at least 2")); assertTrue(bucket.getRequirement().getNote().contains("does not verify contents"));
            FieldRunPlan empty=planner.plan(account(99),EnumSet.of(FieldPatchDefinition.FALADOR_NORTH),FieldRunCrop.TOMATO,FieldRunMode.HARVEST_ALL,true,compost,Map.of(),
                    new StorageSnapshot(Map.of(ItemID.BOTTOMLESS_COMPOST_BUCKET,1L),Map.of(),Map.of(),true,now),now);
            assertEquals(SupplyState.MISSING,supply(empty,ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED).getState());
        }
    }

}
