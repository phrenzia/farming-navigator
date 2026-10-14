package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.poh.PohHouseLocation;
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

public class PohHouseLocationRouteRequirementTest
{
    @Test
    public void hosidiusHouseWithCarriedTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        RequirementResult result =
                evaluate(
                        createConfig(
                                PohHouseLocation.HOSIDIUS
                        ),
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

        assertEquals(
                "POH route via Hosidius house location",
                result.getRequirement()
        );
    }

    @Test
    public void hosidiusHouseWithBankedTabletIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                2L
        );

        RequirementResult result =
                evaluate(
                        createConfig(
                                PohHouseLocation.HOSIDIUS
                        ),
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
                                "Teleport to house tablet"
                        )
        );
    }

    @Test
    public void wrongHouseLocationIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        RequirementResult result =
                evaluate(
                        createConfig(
                                PohHouseLocation.RIMMINGTON
                        ),
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
                                "Rimmington"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "requires Hosidius"
                        )
        );
    }

    @Test
    public void noConfiguredHouseIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        RequirementResult result =
                evaluate(
                        createConfig(
                                PohHouseLocation.NONE
                        ),
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
                                "no player-owned house is configured"
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

    @Test
    public void hosidiusHouseWithoutEntryMethodIsBlocked()
    {
        RequirementResult result =
                evaluate(
                        createConfig(
                                PohHouseLocation.HOSIDIUS
                        ),
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
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullLocationIsRejected()
    {
        new PohHouseLocationRouteRequirement(
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void noneLocationIsRejected()
    {
        new PohHouseLocationRouteRequirement(
                PohHouseLocation.NONE
        );
    }

    private RequirementResult evaluate(
            FarmingNavigatorConfig config,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
        AccountState accountState =
                new AccountState(
                        true,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.LUNAR,
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

        return new PohHouseLocationRouteRequirement(
                PohHouseLocation.HOSIDIUS
        )
                .evaluate(
                        context
                );
    }

    private FarmingNavigatorConfig createConfig(
            PohHouseLocation location
    )
    {
        return new FarmingNavigatorConfig()
        {
            @Override
            public PohHouseLocation
            pohHouseLocation()
            {
                return location;
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