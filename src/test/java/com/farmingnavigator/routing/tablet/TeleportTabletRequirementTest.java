package com.farmingnavigator.routing.tablet;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementReadiness;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RequirementState;
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

public class TeleportTabletRequirementTest
{
    @Test
    public void definitionUsesCurrentRuneLiteHouseTabletId()
    {
        assertEquals(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                TeleportTabletDefinition
                        .TELEPORT_TO_HOUSE
                        .getItemId()
        );
    }

    @Test
    public void carriedTabletIsReadyWithoutMagicOrStandardSpellbook()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        RequirementResult result =
                evaluate(
                        createHouseConfig(),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap()
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

        /*
         * AllOfRequirement intentionally collapses a fully
         * satisfied composite requirement to the composite
         * display name and generic success detail.
         *
         * Assert the stable requirement identity instead of
         * depending on the child item's incidental detail text.
         */
        assertEquals(
                "Teleport to house tablet",
                result.getRequirement()
        );
    }

    @Test
    public void bankedTabletIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                3L
        );

        RequirementResult result =
                evaluate(
                        createHouseConfig(),
                        SpellbookType.ARCEUUS,
                        Collections.emptyMap(),
                        bank
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
                                "AVAILABLE FROM BANK"
                        )
        );
    }

    @Test
    public void missingTabletReportsBlocked()
    {
        RequirementResult result =
                evaluate(
                        createHouseConfig(),
                        SpellbookType.STANDARD,
                        Collections.emptyMap(),
                        Collections.emptyMap()
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
    public void configuredHouseIsRequired()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        RequirementResult result =
                evaluate(
                        createNoHouseConfig(),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap()
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
                                "Player-owned house"
                        )
        );
    }

    @Test
    public void missingPohConfigurationRequiresConfiguration()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        RequirementResult result =
                evaluate(
                        null,
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap()
                );

        assertEquals(
                RequirementState.CONFIGURATION_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Configure your POH profile"
                        )
        );
    }

    private RequirementResult evaluate(
            FarmingNavigatorConfig config,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
        /*
         * This compatibility AccountState constructor leaves
         * Magic at 0.
         *
         * That is intentional: tablet usage must not depend
         * on the Teleport to House spell's 40 Magic
         * requirement.
         */
        AccountState accountState =
                new AccountState(
                        true,
                        1,
                        1,
                        EnumSet.noneOf(
                                Quest.class
                        ),
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        spellbook,
                        null,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );

        StorageSnapshot storageSnapshot =
                new StorageSnapshot(
                        convertToLongMap(
                                inventory
                        ),
                        Collections.emptyMap(),
                        bank,
                        true,
                        Instant.now()
                );

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelSnapshot,
                        storageSnapshot
                );

        return TeleportTabletDefinition
                .TELEPORT_TO_HOUSE
                .createRequirement()
                .evaluate(
                        context
                );
    }

    private FarmingNavigatorConfig
    createHouseConfig()
    {
        return new FarmingNavigatorConfig()
        {
            @Override
            public PohHouseLocation
            pohHouseLocation()
            {
                return PohHouseLocation.RIMMINGTON;
            }
        };
    }

    private FarmingNavigatorConfig
    createNoHouseConfig()
    {
        return new FarmingNavigatorConfig()
        {
            @Override
            public PohHouseLocation
            pohHouseLocation()
            {
                return PohHouseLocation.NONE;
            }
        };
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