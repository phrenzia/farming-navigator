package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuneCostRequirementTest
{
    @Test
    public void ordinaryRunesCanSatisfyCostNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.AIRRUNE,
                3
        );

        inventory.put(
                ItemID.WATERRUNE,
                1
        );

        inventory.put(
                ItemID.LAWRUNE,
                1
        );

        RequirementResult result =
                createFaladorRuneRequirement()
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        false
                                )
                        );

        assertTrue(
                result.isSatisfied()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void combinationRuneSuppliesBothElements()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.MISTRUNE,
                3
        );

        inventory.put(
                ItemID.LAWRUNE,
                1
        );

        RequirementResult result =
                createFaladorRuneRequirement()
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        false
                                )
                        );

        /*
         * Three mist runes supply all three Air requirements
         * while also supplying the one Water requirement.
         */
        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void sunfireRuneCanSupplyTeleportFireRequirement()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.SUNFIRERUNE,
                1
        );

        inventory.put(
                ItemID.LAWRUNE,
                2
        );

        RuneCost cost =
                RuneCost.builder()
                        .add(
                                RuneKind.FIRE,
                                1
                        )
                        .add(
                                RuneKind.LAW,
                                2
                        )
                        .build();

        RequirementResult result =
                new RuneCostRequirement(
                        "Test teleport runes",
                        cost
                )
                        .evaluate(
                                createContext(
                                        60,
                                        60,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        false
                                )
                        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void aetherRuneSuppliesSoulAndCosmic()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.AETHERRUNE,
                2
        );

        RuneCost cost =
                RuneCost.builder()
                        .add(
                                RuneKind.SOUL,
                                2
                        )
                        .add(
                                RuneKind.COSMIC,
                                1
                        )
                        .build();

        RequirementResult result =
                new RuneCostRequirement(
                        "Aether test",
                        cost
                )
                        .evaluate(
                                createContext(
                                        99,
                                        99,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        false
                                )
                        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void carriedRunePouchContentsAreUsableNow()
    {
        Map<Integer, Integer> pouch =
                new LinkedHashMap<>();

        pouch.put(
                ItemID.MISTRUNE,
                3
        );

        pouch.put(
                ItemID.LAWRUNE,
                1
        );

        RequirementResult result =
                createFaladorRuneRequirement()
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        pouch,
                                        pouch,
                                        true,
                                        Collections.emptyMap(),
                                        false
                                )
                        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void bankedRunesAreAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.AIRRUNE,
                3L
        );

        bank.put(
                ItemID.WATERRUNE,
                1L
        );

        bank.put(
                ItemID.LAWRUNE,
                1L
        );

        RequirementResult result =
                createFaladorRuneRequirement()
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        bank,
                                        true
                                )
                        );

        assertTrue(
                result.isSatisfied()
        );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertEquals(
                RequirementReadiness.AVAILABLE_FROM_BANK,
                result.getReadiness()
        );
    }

    @Test
    public void bankedRunePouchContentsCanBePrepared()
    {
        Map<Integer, Integer> storedPouch =
                new LinkedHashMap<>();

        storedPouch.put(
                ItemID.MISTRUNE,
                3
        );

        storedPouch.put(
                ItemID.LAWRUNE,
                1
        );

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.BH_RUNE_POUCH,
                1L
        );

        RequirementResult result =
                createFaladorRuneRequirement()
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        storedPouch,
                                        false,
                                        bank,
                                        true
                                )
                        );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Rune pouch"
                        )
        );
    }

    @Test
    public void equippedElementalStaffSuppliesInfiniteRunes()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.LAWRUNE,
                1
        );

        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                1381,
                1
        );

        RuneCost cost =
                RuneCost.builder()
                        .add(
                                RuneKind.AIR,
                                5
                        )
                        .add(
                                RuneKind.LAW,
                                1
                        )
                        .build();

        RequirementResult result =
                new RuneCostRequirement(
                        "Staff test",
                        cost
                )
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        inventory,
                                        equipment,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        false
                                )
                        );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Staff of air"
                        )
        );
    }

    @Test
    public void inventoryCombinationStaffCanBeEquipped()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                20730,
                1
        );

        inventory.put(
                ItemID.LAWRUNE,
                1
        );

        RuneCost cost =
                RuneCost.builder()
                        .add(
                                RuneKind.AIR,
                                5
                        )
                        .add(
                                RuneKind.WATER,
                                10
                        )
                        .add(
                                RuneKind.LAW,
                                1
                        )
                        .build();

        RequirementResult result =
                new RuneCostRequirement(
                        "Mist staff test",
                        cost
                )
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        false
                                )
                        );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "equip Mist battlestaff"
                        )
        );
    }

    @Test
    public void twoDifferentStavesCannotBeCombined()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        /*
         * Staff of air and Staff of water.
         *
         * They cannot both be wielded simultaneously.
         */
        inventory.put(
                1381,
                1
        );

        inventory.put(
                1383,
                1
        );

        RuneCost cost =
                RuneCost.builder()
                        .add(
                                RuneKind.AIR,
                                1
                        )
                        .add(
                                RuneKind.WATER,
                                1
                        )
                        .build();

        RequirementResult result =
                new RuneCostRequirement(
                        "Two-staff test",
                        cost
                )
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        true
                                )
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );
    }

    @Test
    public void twinflameStaffRequiresRealMagicLevel()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                30634,
                1
        );

        inventory.put(
                ItemID.LAWRUNE,
                2
        );

        RuneCost cost =
                RuneCost.builder()
                        .add(
                                RuneKind.WATER,
                                1
                        )
                        .add(
                                RuneKind.FIRE,
                                1
                        )
                        .add(
                                RuneKind.LAW,
                                2
                        )
                        .build();

        RequirementResult blocked =
                new RuneCostRequirement(
                        "Twinflame test",
                        cost
                )
                        .evaluate(
                                createContext(
                                        59,
                                        60,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        true
                                )
                        );

        assertFalse(
                blocked.isSatisfied()
        );

        RequirementResult allowed =
                new RuneCostRequirement(
                        "Twinflame test",
                        cost
                )
                        .evaluate(
                                createContext(
                                        60,
                                        60,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        true
                                )
                        );

        assertTrue(
                allowed.isReadyNow()
        );

        assertTrue(
                allowed.getDetail()
                        .contains(
                                "Twinflame staff"
                        )
        );
    }

    @Test
    public void missingVerifiedRunesAreBlockedPrecisely()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.AIRRUNE,
                1
        );

        RequirementResult result =
                createFaladorRuneRequirement()
                        .evaluate(
                                createContext(
                                        50,
                                        50,
                                        inventory,
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        false,
                                        Collections.emptyMap(),
                                        true
                                )
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Air rune x2"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Water rune x1"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Law rune x1"
                        )
        );
    }

    private RuneCostRequirement
    createFaladorRuneRequirement()
    {
        RuneCost cost =
                RuneCost.builder()
                        .add(
                                RuneKind.AIR,
                                3
                        )
                        .add(
                                RuneKind.WATER,
                                1
                        )
                        .add(
                                RuneKind.LAW,
                                1
                        )
                        .build();

        return new RuneCostRequirement(
                "Falador Teleport runes",
                cost
        );
    }

    @Test
    public void structuredPreparationReportsEquippedCarriedAndBankedSources()
    {
        for (int location = 0; location < 3; location++)
        {
            Map<Integer, Integer> inventory = new LinkedHashMap<>();
            inventory.put(ItemID.WATERRUNE, 1);
            inventory.put(ItemID.LAWRUNE, 1);
            if (location == 0) { inventory.put(ItemID.STAFF_OF_AIR, 1); }
            RunePreparation preparation = createFaladorRuneRequirement().evaluatePreparation(createContext(99, 99, inventory,
                    location == 1 ? Collections.singletonMap(ItemID.STAFF_OF_AIR, 1) : Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(), false,
                    location == 2 ? Collections.singletonMap(ItemID.STAFF_OF_AIR, 1L) : Collections.emptyMap(), true));
            assertEquals(InfiniteRuneSource.STAFF_OF_AIR, preparation.getSource());
            assertEquals(location == 2, preparation.isBankedSource());
            assertEquals(location == 2, preparation.getResult().requiresBankWithdrawal());
            assertFalse(preparation.isWithdrawPouch());
        }
    }

    @Test
    public void structuredPreparationDoesNotSelectUnneededOrUnusableEquipment()
    {
        RunePreparation loose = createFaladorRuneRequirement().evaluatePreparation(createContext(99, 99,
                Map.of(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1),
                Map.of(ItemID.STAFF_OF_AIR, 1), Collections.emptyMap(), Collections.emptyMap(), false, Collections.emptyMap(), true));
        assertEquals(null, loose.getSource());
        RunePreparation missing = createFaladorRuneRequirement().evaluatePreparation(createContext(1, 99,
                Map.of(ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1, ItemID.AIR_BATTLESTAFF, 1), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), false, Collections.emptyMap(), true));
        assertEquals(null, missing.getSource());
        assertFalse(missing.getResult().isSatisfied());
    }

    @Test
    public void structuredPreparationIdentifiesRequiredBankedPouch()
    {
        RunePreparation preparation = createFaladorRuneRequirement().evaluatePreparation(createContext(99, 99,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                Map.of(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1), false,
                Collections.singletonMap(ItemID.BH_RUNE_POUCH, 1L), true));
        assertTrue(preparation.getResult().requiresBankWithdrawal());
        assertTrue(preparation.isWithdrawPouch());
        assertEquals(null, preparation.getSource());
    }

    private RequirementContext createContext(
            int realMagicLevel,
            int boostedMagicLevel,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Integer> usablePouch,
            Map<Integer, Integer> storedPouch,
            boolean pouchCarried,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        AccountState accountState =
                new AccountState(
                        true,
                        99,
                        99,
                        realMagicLevel,
                        boostedMagicLevel,
                        99,
                        99,
                        99,
                        99,
                        null,
                        null,
                        null
                );

        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.STANDARD,
                        null,
                        inventory,
                        equipment,
                        usablePouch,
                        storedPouch,
                        pouchCarried
                );

        StorageSnapshot storageSnapshot =
                new StorageSnapshot(
                        convertToLongMap(
                                inventory
                        ),
                        convertToLongMap(
                                equipment
                        ),
                        bank,
                        bankVerified,
                        bankVerified
                                ? Instant.now()
                                : null
                );

        return new RequirementContext(
                accountState,
                null,
                travelSnapshot,
                storageSnapshot
        );
    }

    private Map<Integer, Long> convertToLongMap(
            Map<Integer, Integer> source
    )
    {
        Map<Integer, Long> result =
                new LinkedHashMap<>();

        for (
                Map.Entry<Integer, Integer> entry
                : source.entrySet()
        )
        {
            result.put(
                    entry.getKey(),
                    entry.getValue()
                            .longValue()
            );
        }

        return result;
    }
}
