package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import net.runelite.api.Quest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LiveItemRequirementTest
{
    @Test
    public void inventoryRequirementUsesInventoryOnly()
    {
        RequirementContext context =
                createContext();

        LiveItemRequirement requirement =
                new LiveItemRequirement(
                        "Three test items",
                        1001,
                        3,
                        LiveItemLocation.INVENTORY
                );

        assertTrue(
                requirement
                        .evaluate(context)
                        .isSatisfied()
        );
    }

    @Test
    public void equipmentRequirementUsesEquipmentOnly()
    {
        RequirementContext context =
                createContext();

        LiveItemRequirement requirement =
                new LiveItemRequirement(
                        "Equipped test item",
                        2001,
                        1,
                        LiveItemLocation.EQUIPMENT
                );

        assertTrue(
                requirement
                        .evaluate(context)
                        .isSatisfied()
        );
    }

    @Test
    public void carriedRequirementCombinesInventoryAndEquipment()
    {
        RequirementContext context =
                createContext();

        LiveItemRequirement requirement =
                new LiveItemRequirement(
                        "Carried test item",
                        3001,
                        3,
                        LiveItemLocation
                                .INVENTORY_OR_EQUIPMENT
                );

        assertTrue(
                requirement
                        .evaluate(context)
                        .isSatisfied()
        );
    }

    @Test
    public void requirementCanAcceptMultipleItemVariants()
    {
        RequirementContext context =
                createContext();

        LiveItemRequirement requirement =
                new LiveItemRequirement(
                        "Any test-ring variant",
                        new LinkedHashSet<>(
                                Arrays.asList(
                                        4001,
                                        4002,
                                        4003
                                )
                        ),
                        1,
                        LiveItemLocation
                                .INVENTORY_OR_EQUIPMENT
                );

        assertTrue(
                requirement
                        .evaluate(context)
                        .isSatisfied()
        );
    }

    @Test
    public void missingItemIsBlocked()
    {
        RequirementContext context =
                createContext();

        LiveItemRequirement requirement =
                new LiveItemRequirement(
                        "Missing item",
                        9999,
                        1,
                        LiveItemLocation.INVENTORY
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
    }

    @Test
    public void loggedOutStateRequiresData()
    {
        RequirementContext context =
                new RequirementContext(
                        AccountState.loggedOut(),
                        null,
                        TravelCapabilitySnapshot
                                .loggedOut()
                );

        LiveItemRequirement requirement =
                new LiveItemRequirement(
                        "Test item",
                        1001,
                        1,
                        LiveItemLocation.INVENTORY
                );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                requirement
                        .evaluate(context)
                        .getState()
        );
    }

    @Test
    public void snapshotReportsLocationsSeparately()
    {
        TravelCapabilitySnapshot snapshot =
                createSnapshot();

        assertEquals(
                3,
                snapshot.getInventoryQuantity(
                        1001
                )
        );

        assertEquals(
                1,
                snapshot.getEquipmentQuantity(
                        2001
                )
        );

        assertEquals(
                3,
                snapshot.getCarriedQuantity(
                        3001
                )
        );

        assertTrue(
                snapshot.hasInInventory(
                        4002
                )
        );

        assertTrue(
                snapshot.isEquipped(
                        2001
                )
        );

        assertFalse(
                snapshot.hasCarriedItem(
                        9999
                )
        );
    }

    private RequirementContext createContext()
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

        return new RequirementContext(
                accountState,
                null,
                createSnapshot()
        );
    }

    private TravelCapabilitySnapshot createSnapshot()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                1001,
                3
        );

        inventory.put(
                3001,
                2
        );

        inventory.put(
                4002,
                1
        );

        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                2001,
                1
        );

        equipment.put(
                3001,
                1
        );

        Map<Integer, Integer> runePouch =
                new LinkedHashMap<>();

        runePouch.put(
                563,
                500
        );

        return new TravelCapabilitySnapshot(
                true,
                SpellbookType.STANDARD,
                null,
                inventory,
                equipment,
                runePouch
        );
    }
}