package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RuneKind;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DraynorManorTeleportTransportVerificationTest
{
    @Test
    public void catalogueAddsVerifiedDraynorManorTeleport()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        service
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.TELEPORT_SPELL,
                option.getVerificationKind()
        );

        assertEquals(
                TransportCategory.SPELL,
                option.getCategory()
        );

        assertEquals(
                TeleportSpellDefinition.DRAYNOR_MANOR_TELEPORT,
                option.getTeleportSpellDefinition()
        );

        assertEquals(
                8,
                option.getBaselineRank()
        );
    }

    @Test
    public void definitionUsesVerifiedSpellRequirements()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.DRAYNOR_MANOR_TELEPORT;

        assertEquals(
                "Draynor Manor Teleport",
                spell.getDisplayName()
        );

        assertEquals(
                17,
                spell.getRequiredMagicLevel()
        );

        assertEquals(
                SpellbookType.ARCEUUS,
                spell.getSpellbook()
        );

        assertEquals(
                Integer.valueOf(
                        1
                ),
                spell.getRuneCost()
                        .getQuantities()
                        .get(
                                RuneKind.EARTH
                        )
        );

        assertEquals(
                Integer.valueOf(
                        1
                ),
                spell.getRuneCost()
                        .getQuantities()
                        .get(
                                RuneKind.WATER
                        )
        );

        assertEquals(
                Integer.valueOf(
                        1
                ),
                spell.getRuneCost()
                        .getQuantities()
                        .get(
                                RuneKind.LAW
                        )
        );
    }

    @Test
    public void arceuusSpellbookWithCarriedRunesIsReadyNow()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.DRAYNOR_MANOR_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        17,
                        17,
                        SpellbookType.ARCEUUS,
                        createInventoryRunes(
                                spell
                        ),
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
                                "Draynor Manor Teleport"
                        )
        );
    }

    @Test
    public void bankedRunesAreAvailableFromBank()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.DRAYNOR_MANOR_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        17,
                        17,
                        SpellbookType.ARCEUUS,
                        Collections.emptyMap(),
                        createBankRunes(
                                spell
                        )
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
                                "AVAILABLE FROM BANK"
                        )
        );
    }

    @Test
    public void wrongSpellbookIsLocked()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.DRAYNOR_MANOR_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        99,
                        99,
                        SpellbookType.STANDARD,
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Arceuus spellbook"
                        )
        );
    }

    @Test
    public void insufficientMagicIsLocked()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.DRAYNOR_MANOR_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        16,
                        16,
                        SpellbookType.ARCEUUS,
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );
    }

    @Test
    public void missingRunesAreMissing()
    {
        VerifiedTransportResult result =
                evaluate(
                        99,
                        99,
                        SpellbookType.ARCEUUS,
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

    private TransportOption findOption(
            HerbPatchTransportService service
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                HerbPatchDefinition.FALADOR
        )
        )
        {
            if (
                    "Draynor Manor Teleport"
                            .equals(
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
            int realMagicLevel,
            int boostedMagicLevel,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
        HerbPatchTransportService transportService =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        transportService
                );

        assertNotNull(
                option
        );

        AccountState accountState =
                new AccountState(
                        true,
                        99,
                        99,
                        realMagicLevel,
                        boostedMagicLevel,
                        99,
                        99,
                        99,
                        99,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
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
                HerbPatchDefinition.FALADOR,
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
                        HerbPatchDefinition.FALADOR
                )
                .get(
                        0
                );
    }

    private Map<Integer, Integer>
    createInventoryRunes(
            TeleportSpellDefinition spell
    )
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        for (
                Map.Entry<RuneKind, Integer> entry
                : spell.getRuneCost()
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
    createBankRunes(
            TeleportSpellDefinition spell
    )
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        for (
                Map.Entry<RuneKind, Integer> entry
                : spell.getRuneCost()
                .getQuantities()
                .entrySet()
        )
        {
            bank.merge(
                    entry.getKey()
                            .getCanonicalItemId(),
                    entry.getValue()
                            .longValue(),
                    Long::sum
            );
        }

        return bank;
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