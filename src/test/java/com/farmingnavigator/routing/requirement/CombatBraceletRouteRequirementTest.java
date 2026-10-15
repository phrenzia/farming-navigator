package com.farmingnavigator.routing.requirement;

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

public class CombatBraceletRouteRequirementTest
{
    @Test
    public void carriedOneChargeBraceletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.JEWL_BRACELET_OF_COMBAT_1,
                1
        );

        RequirementResult result =
                evaluate(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
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

        assertEquals(
                "Combat bracelet to Ranging Guild",
                result.getRequirement()
        );
    }

    @Test
    public void equippedSixChargeBraceletIsReadyNow()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.JEWL_BRACELET_OF_COMBAT_6,
                1
        );

        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        equipment,
                        Collections.emptyMap(),
                        false
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
    public void bankedSixChargeBraceletIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.JEWL_BRACELET_OF_COMBAT_6,
                1L
        );

        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true
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
                                "Combat bracelet(6)"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "AVAILABLE FROM BANK"
                        )
        );
    }

    @Test
    public void unchargedBraceletOnlyIsMissing()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.JEWL_BRACELET_OF_COMBAT,
                1
        );

        RequirementResult result =
                evaluate(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
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
                                "MISSING"
                        )
        );
    }

    @Test
    public void noBraceletWithVerifiedBankIsMissing()
    {
        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
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
                                "MISSING"
                        )
        );
    }

    @Test
    public void noBraceletWithUnknownBankRequiresData()
    {
        RequirementResult result =
                evaluate(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
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
                                "DATA REQUIRED"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Open any normal bank once"
                        )
        );
    }

    @Test
    public void carriedChargedBraceletDoesNotNeedBankVerification()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.JEWL_BRACELET_OF_COMBAT_4,
                1
        );

        RequirementResult result =
                evaluate(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    private RequirementResult evaluate(
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.STANDARD,
                        null,
                        inventory,
                        equipment,
                        Collections.emptyMap()
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

        return new CombatBraceletRouteRequirement()
                .evaluate(
                        context
                );
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