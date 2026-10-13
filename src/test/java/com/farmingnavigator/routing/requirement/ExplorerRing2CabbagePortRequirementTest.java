package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.DailyTransportUseKind;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExplorerRing2CabbagePortRequirementTest
{
    @Test
    public void carriedRingWithThreeUsesRemainingIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.LUMBRIDGE_RING_MEDIUM,
                1
        );

        RequirementResult result =
                evaluate(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true,
                        dailyUses(
                                3
                        )
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.READY_NOW,
                result.getReadiness()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void equippedRingWithOneUseRemainingIsReadyNow()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.LUMBRIDGE_RING_MEDIUM,
                1
        );

        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        equipment,
                        Collections.emptyMap(),
                        true,
                        dailyUses(
                                1
                        )
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void bankedRingWithUsesRemainingIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.LUMBRIDGE_RING_MEDIUM,
                1L
        );

        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true,
                        dailyUses(
                                2
                        )
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.AVAILABLE_FROM_BANK,
                result.getReadiness()
        );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Explorer's ring 2"
                        )
        );
    }

    @Test
    public void carriedRingWithZeroUsesRemainingIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.LUMBRIDGE_RING_MEDIUM,
                1
        );

        RequirementResult result =
                evaluate(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true,
                        dailyUses(
                                0
                        )
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.NOT_AVAILABLE,
                result.getReadiness()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "0 remain today"
                        )
        );
    }

    @Test
    public void carriedRingWithUnknownDailyUseRequiresData()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.LUMBRIDGE_RING_MEDIUM,
                1
        );

        RequirementResult result =
                evaluate(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true,
                        Collections.emptyMap()
                );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "have not been verified"
                        )
        );
    }

    @Test
    public void missingRingIsBlockedEvenWhenDailyUseExists()
    {
        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true,
                        dailyUses(
                                3
                        )
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "no supported Explorer's ring 2"
                        )
        );
    }

    @Test
    public void explorerRing3DoesNotCountAsRing2Requirement()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.LUMBRIDGE_RING_HARD,
                1
        );

        RequirementResult result =
                evaluate(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true,
                        dailyUses(
                                3
                        )
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );
    }

    @Test
    public void unknownBankAndNoCarriedRingRequiresData()
    {
        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        dailyUses(
                                3
                        )
                );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );
    }

    @Test
    public void unknownRingUsesAndUnknownBankRequireData()
    {
        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        Collections.emptyMap()
                );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );
    }

    private RequirementResult evaluate(
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified,
            Map<
                    DailyTransportUseKind,
                    Integer
                    > dailyUses
    )
    {
        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.STANDARD,
                        null,
                        inventory,
                        equipment,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        dailyUses
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

        RequirementContext context =
                new RequirementContext(
                        null,
                        null,
                        travelSnapshot,
                        storageSnapshot
                );

        return new ExplorerRing2CabbagePortRequirement()
                .evaluate(
                        context
                );
    }

    private Map<
            DailyTransportUseKind,
            Integer
            > dailyUses(
            int remaining
    )
    {
        EnumMap<
                DailyTransportUseKind,
                Integer
                > dailyUses =
                new EnumMap<>(
                        DailyTransportUseKind.class
                );

        dailyUses.put(
                DailyTransportUseKind
                        .EXPLORER_RING_CABBAGE_PORT,
                remaining
        );

        return dailyUses;
    }

    private Map<Integer, Long>
    convertToLongMap(
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