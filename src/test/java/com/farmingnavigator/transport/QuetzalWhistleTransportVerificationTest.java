package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QuetzalWhistleTransportVerificationTest
{
    @Test
    public void cataloguePromotesOnlyQuetzalWhistleRoute()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption whistle =
                findOption(
                        service,
                        "Quetzal whistle"
                );

        assertNotNull(
                whistle
        );

        assertEquals(
                TransportVerificationKind.QUETZAL_WHISTLE,
                whistle.getVerificationKind()
        );

        assertEquals(
                TransportCategory.ITEM,
                whistle.getCategory()
        );

        assertEquals(
                1,
                whistle.getBaselineRank()
        );

        TransportOption hunterCape =
                findOption(
                        service,
                        "Hunter cape"
                );

        assertNotNull(
                hunterCape
        );

        assertEquals(
                TransportVerificationKind.NOT_IMPLEMENTED,
                hunterCape.getVerificationKind()
        );

        TransportOption ajp =
                findOption(
                        service,
                        "Fairy ring AJP"
                );

        assertNotNull(
                ajp
        );

        assertEquals(
                TransportVerificationKind.NOT_IMPLEMENTED,
                ajp.getVerificationKind()
        );
    }

    @Test
    public void carriedUnlimitedWhistleIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.HG_QUETZALWHISTLE_PERFECTED_INFINITE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
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
                                "unlimited charges"
                        )
        );
    }

    @Test
    public void bankedUnlimitedWhistleIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.HG_QUETZALWHISTLE_PERFECTED_INFINITE,
                1L
        );

        VerifiedTransportResult result =
                evaluate(
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
                                "Perfected quetzal whistle(i)"
                        )
        );
    }

    @Test
    public void carriedFiniteWhistleRequiresChargeData()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.HG_QUETZALWHISTLE_ENHANCED,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        inventory,
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.DATA_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "finite-charge"
                        )
        );
    }

    @Test
    public void bankedFiniteWhistleRequiresChargeData()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.HG_QUETZALWHISTLE_PERFECTED,
                1L
        );

        VerifiedTransportResult result =
                evaluate(
                        Collections.emptyMap(),
                        bank
                );

        assertEquals(
                VerifiedTransportState.DATA_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "verified bank"
                        )
        );
    }

    @Test
    public void missingWhistleIsMissing()
    {
        VerifiedTransportResult result =
                evaluate(
                        Collections.emptyMap(),
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
                                "MISSING"
                        )
        );
    }

    @Test
    public void bankedUnlimitedWhistleWinsOverCarriedFiniteWhistle()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.HG_QUETZALWHISTLE_BASIC,
                1
        );

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.HG_QUETZALWHISTLE_PERFECTED_INFINITE,
                1L
        );

        VerifiedTransportResult result =
                evaluate(
                        inventory,
                        bank
                );

        assertEquals(
                VerifiedTransportState.AVAILABLE_FROM_BANK,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );
    }

    private TransportOption findOption(
            HerbPatchTransportService service,
            String displayName
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                HerbPatchDefinition.ORTUS_FARM
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

    private VerifiedTransportResult evaluate(
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
        HerbPatchTransportService transportService =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        transportService,
                        "Quetzal whistle"
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
                        SpellbookType.STANDARD,
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
                HerbPatchDefinition.ORTUS_FARM,
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
                                travelSnapshot,
                                storageSnapshot,
                                options
                        );

        return results
                .get(
                        HerbPatchDefinition.ORTUS_FARM
                )
                .get(
                        0
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