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

public class RemotePohPortalTransportVerificationTest
{
    @Test
    public void catalogueCarriesTypedTrollStrongholdPortalDestination()
    {
        assertCataloguePortal(
                HerbPatchDefinition.TROLL_STRONGHOLD,
                "POH portal to Troll Stronghold",
                PohPortalDestination.TROLL_STRONGHOLD
        );
    }

    @Test
    public void catalogueCarriesTypedWeissPortalDestination()
    {
        assertCataloguePortal(
                HerbPatchDefinition.WEISS,
                "POH portal to Weiss",
                PohPortalDestination.WEISS
        );
    }

    @Test
    public void catalogueCarriesTypedHarmonyIslandPortalDestination()
    {
        assertCataloguePortal(
                HerbPatchDefinition.HARMONY_ISLAND,
                "POH portal to Harmony Island",
                PohPortalDestination.HARMONY_ISLAND
        );
    }

    @Test
    public void installedTrollStrongholdPortalWithCarriedHouseTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                createCarriedHouseTablet();

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.TROLL_STRONGHOLD,
                        "POH portal to Troll Stronghold",
                        createConfig(
                                true,
                                PohPortalDestination.TROLL_STRONGHOLD
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
                                "Troll Stronghold"
                        )
        );
    }

    @Test
    public void installedWeissPortalWithCarriedHouseTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                createCarriedHouseTablet();

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.WEISS,
                        "POH portal to Weiss",
                        createConfig(
                                true,
                                PohPortalDestination.WEISS
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
                                "Weiss"
                        )
        );
    }

    @Test
    public void installedHarmonyPortalWithCarriedHouseTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                createCarriedHouseTablet();

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.HARMONY_ISLAND,
                        "POH portal to Harmony Island",
                        createConfig(
                                true,
                                PohPortalDestination.HARMONY_ISLAND
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
                                "Harmony Island"
                        )
        );
    }

    @Test
    public void bankedHouseTabletMakesHarmonyPortalAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                2L
        );

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.HARMONY_ISLAND,
                        "POH portal to Harmony Island",
                        createConfig(
                                true,
                                PohPortalDestination.HARMONY_ISLAND
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
    public void wrongInstalledPortalDoesNotSatisfyWeissRoute()
    {
        Map<Integer, Integer> inventory =
                createCarriedHouseTablet();

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.WEISS,
                        "POH portal to Weiss",
                        createConfig(
                                true,
                                PohPortalDestination.TROLL_STRONGHOLD
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
                                "Weiss is not configured"
                        )
        );
    }

    private void assertCataloguePortal(
            HerbPatchDefinition patch,
            String displayName,
            PohPortalDestination expectedDestination
    )
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        service,
                        patch,
                        displayName
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.POH_PORTAL,
                option.getVerificationKind()
        );

        assertEquals(
                expectedDestination,
                option.getPohPortalDestination()
        );

        assertEquals(
                TransportCategory.PLAYER_OWNED_HOUSE,
                option.getCategory()
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

        /*
         * Patch availability is evaluated separately by
         * HerbPatchService.
         *
         * These transport tests deliberately verify only:
         *
         * - POH entry
         * - exact configured portal destination
         */
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

    private Map<Integer, Integer>
    createCarriedHouseTablet()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

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