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

public class UrbanPohPortalTransportVerificationTest
{
    @Test
    public void catalogueCarriesTypedEastArdougnePortalDestination()
    {
        assertCataloguePortal(
                HerbPatchDefinition.ARDOUGNE,
                "POH portal to East Ardougne",
                PohPortalDestination.EAST_ARDOUGNE
        );
    }

    @Test
    public void catalogueCarriesTypedCivitasPortalDestination()
    {
        assertCataloguePortal(
                HerbPatchDefinition.ORTUS_FARM,
                "POH portal to Civitas illa Fortis",
                PohPortalDestination.CIVITAS_ILLA_FORTIS
        );
    }

    @Test
    public void directArdougneTeleportUsesVerifiedSpell()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        service,
                        HerbPatchDefinition.ARDOUGNE,
                        "Ardougne Teleport"
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.TELEPORT_SPELL,
                option.getVerificationKind()
        );
    }

    @Test
    public void directCivitasSpellRemainsVerifiedAndAheadOfPohPortal()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption spell =
                findOption(
                        service,
                        HerbPatchDefinition.ORTUS_FARM,
                        "Civitas illa Fortis Teleport"
                );

        TransportOption portal =
                findOption(
                        service,
                        HerbPatchDefinition.ORTUS_FARM,
                        "POH portal to Civitas illa Fortis"
                );

        assertNotNull(
                spell
        );

        assertNotNull(
                portal
        );

        assertEquals(
                TransportVerificationKind.TELEPORT_SPELL,
                spell.getVerificationKind()
        );

        assertEquals(
                3,
                spell.getBaselineRank()
        );

        assertEquals(
                4,
                portal.getBaselineRank()
        );
    }

    @Test
    public void installedEastArdougnePortalWithCarriedHouseTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                createCarriedHouseTablet();

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.ARDOUGNE,
                        "POH portal to East Ardougne",
                        createConfig(
                                true,
                                PohPortalDestination.EAST_ARDOUGNE
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
                                "East Ardougne"
                        )
        );
    }

    @Test
    public void installedCivitasPortalWithCarriedHouseTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                createCarriedHouseTablet();

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.ORTUS_FARM,
                        "POH portal to Civitas illa Fortis",
                        createConfig(
                                true,
                                PohPortalDestination.CIVITAS_ILLA_FORTIS
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
                                "Civitas illa Fortis"
                        )
        );
    }

    @Test
    public void bankedHouseTabletMakesCivitasPortalAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                2L
        );

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.ORTUS_FARM,
                        "POH portal to Civitas illa Fortis",
                        createConfig(
                                true,
                                PohPortalDestination.CIVITAS_ILLA_FORTIS
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
    public void wrongInstalledPortalDoesNotSatisfyEastArdougneRoute()
    {
        Map<Integer, Integer> inventory =
                createCarriedHouseTablet();

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.ARDOUGNE,
                        "POH portal to East Ardougne",
                        createConfig(
                                true,
                                PohPortalDestination.CIVITAS_ILLA_FORTIS
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
                                "East Ardougne is not configured"
                        )
        );
    }

    @Test
    public void wrongInstalledPortalDoesNotSatisfyCivitasRoute()
    {
        Map<Integer, Integer> inventory =
                createCarriedHouseTablet();

        VerifiedTransportResult result =
                evaluateCatalogueRoute(
                        HerbPatchDefinition.ORTUS_FARM,
                        "POH portal to Civitas illa Fortis",
                        createConfig(
                                true,
                                PohPortalDestination.EAST_ARDOUGNE
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
                                "Civitas illa Fortis is not configured"
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
         * Installed POH portal destinations are explicit
         * configuration-backed capabilities.
         *
         * Original spell/build prerequisites are therefore
         * not re-applied here. Patch availability remains
         * independently evaluated by HerbPatchService.
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