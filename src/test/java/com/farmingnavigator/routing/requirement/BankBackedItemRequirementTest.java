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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BankBackedItemRequirementTest
{
    @Test
    public void carriedItemIsReadyWithoutBankSync()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                1001,
                1
        );

        RequirementContext context =
                createContext(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        BankBackedItemRequirement requirement =
                new BankBackedItemRequirement(
                        "Test teleport",
                        1001,
                        "Test teleport item",
                        1
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertTrue(
                result.isSatisfied()
        );

        assertTrue(
                result.isReadyNow()
        );

        assertEquals(
                RequirementReadiness.READY_NOW,
                result.getReadiness()
        );
    }

    @Test
    public void bankedItemIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                1001,
                2L
        );

        RequirementContext context =
                createContext(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true
                );

        BankBackedItemRequirement requirement =
                new BankBackedItemRequirement(
                        "Test teleport",
                        1001,
                        "Test teleport item",
                        1
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertTrue(
                result.isSatisfied()
        );

        assertFalse(
                result.isReadyNow()
        );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertEquals(
                RequirementReadiness.AVAILABLE_FROM_BANK,
                result.getReadiness()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "withdraw 1"
                        )
        );
    }

    @Test
    public void carriedAndBankedQuantitiesCanCombine()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                1001,
                1
        );

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                1001,
                2L
        );

        RequirementContext context =
                createContext(
                        inventory,
                        Collections.emptyMap(),
                        bank,
                        true
                );

        BankBackedItemRequirement requirement =
                new BankBackedItemRequirement(
                        "Three test items",
                        1001,
                        "Test item",
                        3
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertTrue(
                result.isSatisfied()
        );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "withdraw 2"
                        )
        );
    }

    @Test
    public void unsyncedBankRequiresVerification()
    {
        RequirementContext context =
                createContext(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        BankBackedItemRequirement requirement =
                new BankBackedItemRequirement(
                        "Test teleport",
                        1001,
                        "Test teleport item",
                        1
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Open any normal bank once"
                        )
        );
    }

    @Test
    public void verifiedMissingItemIsBlocked()
    {
        RequirementContext context =
                createContext(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        BankBackedItemRequirement requirement =
                new BankBackedItemRequirement(
                        "Test teleport",
                        1001,
                        "Test teleport item",
                        1
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.NOT_AVAILABLE,
                result.getReadiness()
        );
    }

    @Test
    public void exactItemVariantIsReported()
    {
        Map<Integer, String> variants =
                new LinkedHashMap<>();

        variants.put(
                2001,
                "Explorer's ring 3"
        );

        variants.put(
                2002,
                "Explorer's ring 4"
        );

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                2002,
                1L
        );

        RequirementContext context =
                createContext(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true
                );

        BankBackedItemRequirement requirement =
                new BankBackedItemRequirement(
                        "Explorer's ring 3 or 4",
                        variants,
                        1
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Explorer's ring 4 x1"
                        )
        );
    }

    @Test
    public void allOfPropagatesBankPreparation()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                1001,
                1L
        );

        RequirementContext context =
                createContext(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true
                );

        RouteRequirement farmingRequirement =
                new SkillRequirement(
                        AccountSkill.FARMING,
                        40,
                        false
                );

        RouteRequirement itemRequirement =
                new BankBackedItemRequirement(
                        "Test teleport",
                        1001,
                        "Test teleport item",
                        1
                );

        AllOfRequirement requirement =
                new AllOfRequirement(
                        "Complete route",
                        farmingRequirement,
                        itemRequirement
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertTrue(
                result.isSatisfied()
        );

        assertTrue(
                result.requiresBankWithdrawal()
        );
    }

    @Test
    public void anyOfPrefersReadyNowOverBankedAlternative()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                2001,
                1
        );

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                1001,
                1L
        );

        RequirementContext context =
                createContext(
                        inventory,
                        Collections.emptyMap(),
                        bank,
                        true
                );

        RouteRequirement bankedAlternative =
                new BankBackedItemRequirement(
                        "Banked teleport",
                        1001,
                        "Banked teleport item",
                        1
                );

        RouteRequirement readyAlternative =
                new BankBackedItemRequirement(
                        "Ready teleport",
                        2001,
                        "Ready teleport item",
                        1
                );

        AnyOfRequirement requirement =
                new AnyOfRequirement(
                        "Any usable teleport",
                        bankedAlternative,
                        readyAlternative
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertTrue(
                result.isReadyNow()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Ready teleport"
                        )
        );
    }

    private RequirementContext createContext(
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        AccountState accountState =
                new AccountState(
                        true,
                        41,
                        41,
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