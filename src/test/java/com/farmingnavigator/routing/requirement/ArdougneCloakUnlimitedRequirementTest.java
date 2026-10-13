package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArdougneCloakUnlimitedRequirementTest
{
    @Test
    public void carriedArdougneCloak4IsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.ARDY_CAPE_ELITE,
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

        assertTrue(
                result.getDetail()
                        .contains(
                                "Ardougne cloak 4"
                        )
        );
    }

    @Test
    public void equippedArdougneMaxCapeIsReadyNow()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.SKILLCAPE_MAX_ARDY,
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

        assertEquals(
                RequirementReadiness.READY_NOW,
                result.getReadiness()
        );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Ardougne max cape"
                        )
        );
    }

    @Test
    public void bankedArdougneCloak4IsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.ARDY_CAPE_ELITE,
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
                                "Ardougne cloak 4"
                        )
        );
    }

    @Test
    public void bankedArdougneMaxCapeIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.SKILLCAPE_MAX_ARDY,
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

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Ardougne max cape"
                        )
        );
    }

    @Test
    public void ardougneCloak3IsNotTreatedAsUnlimited()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.ARDY_CAPE_HARD,
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
    }

    @Test
    public void ardougneCloak2IsNotTreatedAsUnlimited()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.ARDY_CAPE_MEDIUM,
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

        assertFalse(
                result.isSatisfied()
        );
    }

    @Test
    public void verifiedMissingUnlimitedCloakIsBlocked()
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
    public void unsyncedBankRequiresDataWhenNoUnlimitedCloakIsCarried()
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
                                "Open any normal bank once"
                        )
        );
    }

    private RequirementResult evaluate(
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        AccountState accountState =
                new AccountState(
                        true,
                        99,
                        99,
                        EnumSet.noneOf(
                                Quest.class
                        ),
                        false
                );

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
                        accountState,
                        null,
                        travelSnapshot,
                        storageSnapshot
                );

        return new ArdougneCloakUnlimitedRequirement()
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