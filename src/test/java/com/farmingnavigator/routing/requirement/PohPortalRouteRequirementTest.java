package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.poh.PohPortalDestination;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PohPortalRouteRequirementTest
{
    @Test
    public void installedCatherbyPortalWithCarriedHouseTabletIsReadyNow()
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
                                true,
                                PohPortalDestination.CATHERBY
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap(),
                        PohPortalDestination.CATHERBY
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
                "POH portal route to Catherby",
                result.getRequirement()
        );
    }

    @Test
    public void installedWeissPortalWithHouseTeleportSpellIsReadyNow()
    {
        RequirementResult result =
                evaluate(
                        createConfig(
                                true,
                                PohPortalDestination.WEISS
                        ),
                        SpellbookType.STANDARD,
                        createHouseTeleportRunes(),
                        Collections.emptyMap(),
                        PohPortalDestination.WEISS
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
    public void installedHarmonyPortalWithBankedHouseTabletIsAvailableFromBank()
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
                                true,
                                PohPortalDestination.HARMONY_ISLAND
                        ),
                        SpellbookType.ARCEUUS,
                        Collections.emptyMap(),
                        bank,
                        PohPortalDestination.HARMONY_ISLAND
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
    public void differentInstalledPortalDoesNotSatisfyRequestedDestination()
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
                                true,
                                PohPortalDestination.FALADOR
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap(),
                        PohPortalDestination.CATHERBY
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
                                "Catherby is not configured"
                        )
        );
    }

    @Test
    public void noInstalledPortalDestinationsIsBlocked()
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
                                true
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap(),
                        PohPortalDestination.TROLL_STRONGHOLD
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
                                "Troll Stronghold is not configured"
                        )
        );
    }

    @Test
    public void configuredPortalWithoutHouseIsBlocked()
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
                                false,
                                PohPortalDestination.CATHERBY
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap(),
                        PohPortalDestination.CATHERBY
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
                        Collections.emptyMap(),
                        PohPortalDestination.CATHERBY
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
    public void portalRequirementMatchesExactConfiguredDestination()
    {
        RequirementContext context =
                new RequirementContext(
                        null,
                        createConfig(
                                true,
                                PohPortalDestination.CATHERBY,
                                PohPortalDestination.WEISS
                        )
                );

        RequirementResult catherby =
                new PohPortalDestinationRequirement(
                        PohPortalDestination.CATHERBY
                )
                        .evaluate(
                                context
                        );

        RequirementResult harmony =
                new PohPortalDestinationRequirement(
                        PohPortalDestination.HARMONY_ISLAND
                )
                        .evaluate(
                                context
                        );

        assertTrue(
                catherby.isReadyNow()
        );

        assertEquals(
                RequirementState.BLOCKED,
                harmony.getState()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDestinationRequirementIsRejected()
    {
        new PohPortalDestinationRequirement(
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPortalRouteDestinationIsRejected()
    {
        new PohPortalRouteRequirement(
                null
        );
    }

    private RequirementResult evaluate(
            FarmingNavigatorConfig config,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank,
            PohPortalDestination destination
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

        return new PohPortalRouteRequirement(
                destination
        )
                .evaluate(
                        context
                );
    }

    private FarmingNavigatorConfig createConfig(
            boolean hasHouse,
            PohPortalDestination... destinations
    )
    {
        EnumSet<PohPortalDestination> installed =
                EnumSet.noneOf(
                        PohPortalDestination.class
                );

        if (destinations != null)
        {
            Collections.addAll(
                    installed,
                    destinations
            );
        }

        Set<PohPortalDestination> immutableInstalled =
                Collections.unmodifiableSet(
                        installed
                );

        return new FarmingNavigatorConfig()
        {
            @Override
            public PohHouseLocation
            pohHouseLocation()
            {
                return hasHouse
                        ? PohHouseLocation.RIMMINGTON
                        : PohHouseLocation.NONE;
            }

            @Override
            public Set<PohPortalDestination>
            pohPortalDestinations()
            {
                return immutableInstalled;
            }
        };
    }

    private Map<Integer, Integer>
    createHouseTeleportRunes()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        for (
                Map.Entry<RuneKind, Integer> entry
                : TeleportSpellDefinition
                .TELEPORT_TO_HOUSE
                .getRuneCost()
                .getQuantities()
                .entrySet()
        )
        {
            inventory.merge(
                    entry.getKey()
                            .getCanonicalItemId(),
                    entry.getValue(),
                    Integer::sum
            );
        }

        return inventory;
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