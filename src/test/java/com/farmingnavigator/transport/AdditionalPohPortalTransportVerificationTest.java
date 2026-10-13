package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.poh.PohPortalDestination;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AdditionalPohPortalTransportVerificationTest
{
    @Test
    public void catalogueCarriesTypedFaladorPortalDestination()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        service,
                        HerbPatchDefinition.FALADOR,
                        "POH portal to Falador"
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.POH_PORTAL,
                option.getVerificationKind()
        );

        assertEquals(
                PohPortalDestination.FALADOR,
                option.getPohPortalDestination()
        );

        assertEquals(
                TransportCategory.PLAYER_OWNED_HOUSE,
                option.getCategory()
        );
    }

    @Test
    public void catalogueCarriesTypedBattlefrontPortalDestination()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        service,
                        HerbPatchDefinition.FARMING_GUILD,
                        "POH portal to Battlefront"
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.POH_PORTAL,
                option.getVerificationKind()
        );

        assertEquals(
                PohPortalDestination.BATTLEFRONT,
                option.getPohPortalDestination()
        );

        assertEquals(
                TransportCategory.PLAYER_OWNED_HOUSE,
                option.getCategory()
        );
    }

    @Test
    public void installedFaladorPortalWithCarriedHouseTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.FALADOR,
                        "POH portal to Falador",
                        createConfig(
                                true,
                                PohPortalDestination.FALADOR
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Falador"
                        )
        );
    }

    @Test
    public void installedBattlefrontPortalWithCarriedHouseTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.FARMING_GUILD,
                        "POH portal to Battlefront",
                        createConfig(
                                true,
                                PohPortalDestination.BATTLEFRONT
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Battlefront"
                        )
        );
    }

    @Test
    public void bankedHouseTabletMakesBattlefrontPortalAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                2L
        );

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.FARMING_GUILD,
                        "POH portal to Battlefront",
                        createConfig(
                                true,
                                PohPortalDestination.BATTLEFRONT
                        ),
                        SpellbookType.ARCEUUS,
                        Collections.emptyMap(),
                        bank
                );

        assertEquals(
                VerifiedTransportState.AVAILABLE_FROM_BANK,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Teleport to house tablet"
                        )
        );
    }

    @Test
    public void battlefrontPortalDoesNotSatisfyFaladorPortalRoute()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.FALADOR,
                        "POH portal to Falador",
                        createConfig(
                                true,
                                PohPortalDestination.BATTLEFRONT
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.MISSING,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Falador is not configured"
                        )
        );
    }

    private TransportOption findOption(
            HerbPatchTransportService service,
            HerbPatchDefinition patch,
            String displayName
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                patch
        )
        )
        {
            if (
                    displayName.equals(
                            option.getDisplayName()
                    )
            )
            {
                return option;
            }
        }

        return null;
    }

    private VerifiedTransportResult evaluateCatalogueRoute(
            HerbPatchDefinition patch,
            String displayName,
            FarmingNavigatorConfig config,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
        HerbPatchTransportService transportService =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        transportService,
                        patch,
                        displayName
                );

        assertNotNull(
                option
        );

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

        Map<
                HerbPatchDefinition,
                List<TransportOption>
                > options =
                new EnumMap<>(
                        HerbPatchDefinition.class
                );

        options.put(
                patch,
                Collections.singletonList(
                        option
                )
        );

        Map<
                HerbPatchDefinition,
                List<VerifiedTransportResult>
                > results =
                new TransportVerificationService()
                        .evaluateAll(
                                accountState,
                                config,
                                travelSnapshot,
                                storageSnapshot,
                                options
                        );

        return results
                .get(
                        patch
                )
                .get(
                        0
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