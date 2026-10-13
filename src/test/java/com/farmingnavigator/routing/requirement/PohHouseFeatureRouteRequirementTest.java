package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
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

public class PohHouseFeatureRouteRequirementTest
{
    @Test
    public void mountedXericsWithCarriedHouseTabletIsReadyNow()
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
                                true
                        ),
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

        assertEquals(
                "POH route via Mounted Xeric's talisman",
                result.getRequirement()
        );
    }

    @Test
    public void mountedXericsWithHouseTeleportSpellIsReadyNow()
    {
        RequirementResult result =
                evaluate(
                        createConfig(
                                true,
                                true
                        ),
                        SpellbookType.STANDARD,
                        createHouseTeleportRunes(),
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
    }

    @Test
    public void mountedXericsWithBankedHouseTabletIsAvailableFromBank()
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
                                true
                        ),
                        SpellbookType.LUNAR,
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
    public void missingMountedXericsIsBlocked()
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
                                false
                        ),
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
                                "Mounted Xeric's talisman is not enabled"
                        )
        );
    }

    @Test
    public void configuredFeatureWithoutHouseIsBlocked()
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
                                true
                        ),
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

    @Test(expected = IllegalArgumentException.class)
    public void nullFeatureIsRejected()
    {
        new PohHouseFeatureRouteRequirement(
                null
        );
    }

    private RequirementResult evaluate(
            FarmingNavigatorConfig config,
            SpellbookType spellbook,
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

        return new PohHouseFeatureRouteRequirement(
                PohFeature.MOUNTED_XERICS_TALISMAN
        )
                .evaluate(
                        context
                );
    }

    private FarmingNavigatorConfig createConfig(
            boolean hasHouse,
            boolean hasMountedXericsTalisman
    )
    {
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
            public boolean
            pohMountedXericsTalisman()
            {
                return hasMountedXericsTalisman;
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